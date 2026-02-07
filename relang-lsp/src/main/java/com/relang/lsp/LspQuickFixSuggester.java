package com.relang.lsp;

import com.relang.diagnostics.DiagnosticFix;
import com.relang.diagnostics.DiagnosticTextEdit;
import com.relang.diagnostics.ReLangDiagnostic;
import com.relang.diagnostics.SourceRange;

import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Generates high-confidence quick fixes for selected diagnostics.
 */
final class LspQuickFixSuggester {

    private static final Pattern UNDEFINED_VARIABLE = Pattern.compile("^Undefined variable '([A-Za-z_][A-Za-z0-9_]*)'$");
    private static final Pattern MISSING_TYPE_ANNOTATION = Pattern.compile("^Parameter '([A-Za-z_][A-Za-z0-9_]*)' must have a type annotation$");
    private static final Pattern ARITY_REQUIRES_AT_LEAST = Pattern.compile("^Function '([A-Za-z_][A-Za-z0-9_]*)' requires at least (\\d+) arguments, got (\\d+)$");
    private static final Pattern ARITY_ACCEPTS_AT_MOST = Pattern.compile("^Function '([A-Za-z_][A-Za-z0-9_]*)' accepts at most (\\d+) arguments, got (\\d+)$");
    private static final Pattern UNKNOWN_FUNCTION = Pattern.compile("^Unknown function: ([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern FUNCTION_DECLARATION = Pattern.compile("\\bfn\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");
    private static final Pattern MISSING_FIELD = Pattern.compile("^Missing field '([A-Za-z_][A-Za-z0-9_]*)' in ([A-Za-z_][A-Za-z0-9_]*) construction$");
    private static final Pattern IMMUTABLE_PARAMETER = Pattern.compile("^Cannot reassign parameter '([A-Za-z_][A-Za-z0-9_]*)';.*$");

    private LspQuickFixSuggester() {
        // Utility class
    }

    static List<DiagnosticFix> suggest(String sourceText, ReLangDiagnostic diagnostic) {
        return switch (diagnostic.code().value()) {
            case "RL2002" -> suggestUndefinedVariable(diagnostic);
            case "RL2005" -> suggestMissingTypeAnnotation(sourceText, diagnostic);
            case "RL2003" -> suggestArity(sourceText, diagnostic);
            case "RL2004" -> suggestUnknownFunction(sourceText, diagnostic);
            case "RL2011" -> suggestMissingField(sourceText, diagnostic);
            case "RL2012" -> suggestImmutableParameter(sourceText, diagnostic);
            default -> List.of();
        };
    }

    private static List<DiagnosticFix> suggestUndefinedVariable(ReLangDiagnostic diagnostic) {
        var matcher = UNDEFINED_VARIABLE.matcher(diagnostic.message());
        if (!matcher.matches()) {
            return List.of();
        }

        var name = matcher.group(1);
        var line = diagnostic.primaryRange().startLine();
        var insertion = SourceRange.point(line, 0);
        var fix = new DiagnosticFix(
                "Create local variable '" + name + "'",
                List.of(new DiagnosticTextEdit(insertion, "let " + name + " = /* TODO */\n"))
        );
        return List.of(fix);
    }

    private static List<DiagnosticFix> suggestMissingTypeAnnotation(String sourceText, ReLangDiagnostic diagnostic) {
        var matcher = MISSING_TYPE_ANNOTATION.matcher(diagnostic.message());
        if (!matcher.matches()) {
            return List.of();
        }

        var name = matcher.group(1);
        var range = diagnostic.primaryRange();
        var insertionColumn = findIdentifierEnd(sourceText, range.startLine(), range.startColumn(), name);
        var insertion = SourceRange.point(range.startLine(), insertionColumn);
        var fix = new DiagnosticFix(
                "Add type annotation to '" + name + "'",
                List.of(new DiagnosticTextEdit(insertion, ": Int"))
        );
        return List.of(fix);
    }

    private static List<DiagnosticFix> suggestArity(String sourceText, ReLangDiagnostic diagnostic) {
        var requires = ARITY_REQUIRES_AT_LEAST.matcher(diagnostic.message());
        if (requires.matches()) {
            return suggestAddMissingArguments(sourceText, diagnostic, Integer.parseInt(requires.group(2)), Integer.parseInt(requires.group(3)));
        }

        var accepts = ARITY_ACCEPTS_AT_MOST.matcher(diagnostic.message());
        if (accepts.matches()) {
            return suggestTrimExtraArguments(sourceText, diagnostic, Integer.parseInt(accepts.group(2)));
        }
        return List.of();
    }

    private static List<DiagnosticFix> suggestUnknownFunction(String sourceText, ReLangDiagnostic diagnostic) {
        var matcher = UNKNOWN_FUNCTION.matcher(diagnostic.message());
        if (!matcher.matches()) {
            return List.of();
        }

        var unknownName = matcher.group(1);
        var replacement = bestFunctionCandidate(sourceText, unknownName);
        if (replacement == null || replacement.equals(unknownName)) {
            return List.of();
        }

        var lookup = SourceTextLookup.from(sourceText);
        var replaceRange = findCallIdentifierRange(lookup, diagnostic.primaryRange(), unknownName);
        if (replaceRange == null) {
            return List.of();
        }

        return List.of(new DiagnosticFix(
                "Rename function to '" + replacement + "'",
                List.of(new DiagnosticTextEdit(replaceRange, replacement))
        ));
    }

    private static List<DiagnosticFix> suggestAddMissingArguments(String sourceText, ReLangDiagnostic diagnostic, int expected, int actual) {
        var missing = expected - actual;
        if (missing <= 0) {
            return List.of();
        }

        var lookup = SourceTextLookup.from(sourceText);
        var call = locateCallParentheses(lookup, diagnostic.primaryRange());
        if (call == null) {
            return List.of();
        }

        var inner = lookup.text().substring(call.openOffset() + 1, call.closeOffset());
        var placeholders = new StringJoiner(", ");
        for (var i = 0; i < missing; i++) {
            placeholders.add("/* TODO */");
        }

        var insertionText = inner.trim().isEmpty()
                ? placeholders.toString()
                : ", " + placeholders;
        var insertion = lookup.rangeAt(call.closeOffset(), call.closeOffset());
        return List.of(new DiagnosticFix(
                "Add missing arguments",
                List.of(new DiagnosticTextEdit(insertion, insertionText))
        ));
    }

    private static List<DiagnosticFix> suggestTrimExtraArguments(String sourceText, ReLangDiagnostic diagnostic, int maxArgs) {
        if (maxArgs < 0) {
            return List.of();
        }

        var lookup = SourceTextLookup.from(sourceText);
        var call = locateCallParentheses(lookup, diagnostic.primaryRange());
        if (call == null) {
            return List.of();
        }

        var inner = lookup.text().substring(call.openOffset() + 1, call.closeOffset());
        var parts = splitTopLevelArgs(inner);
        if (parts.size() <= maxArgs) {
            return List.of();
        }

        var builder = new StringJoiner(", ");
        for (var i = 0; i < maxArgs; i++) {
            builder.add(parts.get(i).trim());
        }
        var replacement = builder.toString();
        var replaceRange = lookup.rangeAt(call.openOffset() + 1, call.closeOffset());
        return List.of(new DiagnosticFix(
                "Remove extra arguments",
                List.of(new DiagnosticTextEdit(replaceRange, replacement))
        ));
    }

    private static List<DiagnosticFix> suggestMissingField(String sourceText, ReLangDiagnostic diagnostic) {
        var matcher = MISSING_FIELD.matcher(diagnostic.message());
        if (!matcher.matches()) {
            return List.of();
        }

        var missingField = matcher.group(1);
        var lookup = SourceTextLookup.from(sourceText);
        var startOffset = lookup.offsetAt(diagnostic.primaryRange().startLine(), diagnostic.primaryRange().startColumn());
        if (startOffset < 0) {
            return List.of();
        }

        var closeBrace = findClosingBrace(lookup.text(), startOffset);
        if (closeBrace < 0) {
            return List.of();
        }

        var insertion = lookup.rangeAt(closeBrace, closeBrace);
        var prefix = needsLeadingComma(lookup.text(), startOffset, closeBrace) ? ", " : "";
        var text = prefix + missingField + ": /* TODO */";
        return List.of(new DiagnosticFix(
                "Add missing field '" + missingField + "'",
                List.of(new DiagnosticTextEdit(insertion, text))
        ));
    }

    private static List<DiagnosticFix> suggestImmutableParameter(String sourceText, ReLangDiagnostic diagnostic) {
        var matcher = IMMUTABLE_PARAMETER.matcher(diagnostic.message());
        if (!matcher.matches()) {
            return List.of();
        }
        var name = matcher.group(1);
        var lookup = SourceTextLookup.from(sourceText);
        var range = diagnostic.primaryRange();
        var line = lookup.line(range.startLine());
        if (line == null) {
            return List.of();
        }

        var start = Math.max(0, Math.min(range.startColumn(), line.length()));
        var assignmentStart = line.indexOf(name + " =", start);
        if (assignmentStart < 0) {
            assignmentStart = line.indexOf(name + "=", start);
        }
        if (assignmentStart < 0) {
            return List.of();
        }

        var replace = lookup.rangeAt(range.startLine(), assignmentStart, range.startLine(), assignmentStart + name.length());
        return List.of(new DiagnosticFix(
                "Shadow immutable parameter with local variable",
                List.of(new DiagnosticTextEdit(replace, "let " + name))
        ));
    }

    private static int findIdentifierEnd(String sourceText, int oneBasedLine, int startColumn, String identifier) {
        if (sourceText == null || sourceText.isBlank()) {
            return startColumn + identifier.length();
        }

        var lines = sourceText.split("\\R", -1);
        var lineIndex = oneBasedLine - 1;
        if (lineIndex < 0 || lineIndex >= lines.length) {
            return startColumn + identifier.length();
        }

        var line = lines[lineIndex];
        var safeStart = Math.max(0, Math.min(startColumn, line.length()));
        var exact = line.indexOf(identifier, safeStart);
        if (exact >= 0) {
            return exact + identifier.length();
        }

        var anywhere = line.indexOf(identifier);
        if (anywhere >= 0) {
            return anywhere + identifier.length();
        }
        return Math.min(line.length(), safeStart + identifier.length());
    }

    private static CallParentheses locateCallParentheses(SourceTextLookup lookup, SourceRange range) {
        var startOffset = lookup.offsetAt(range.startLine(), range.startColumn());
        if (startOffset < 0) {
            return null;
        }
        var text = lookup.text();
        var open = text.indexOf('(', startOffset);
        if (open < 0) {
            return null;
        }
        var close = findMatchingParen(text, open);
        if (close < 0) {
            return null;
        }
        return new CallParentheses(open, close);
    }

    private static SourceRange findCallIdentifierRange(SourceTextLookup lookup, SourceRange range, String expectedIdentifier) {
        var startOffset = lookup.offsetAt(range.startLine(), range.startColumn());
        if (startOffset < 0) {
            return null;
        }

        var call = locateCallParentheses(lookup, range);
        if (call == null) {
            return null;
        }

        var identifierEnd = call.openOffset();
        var identifierStart = identifierEnd;
        var text = lookup.text();
        while (identifierStart > 0 && Character.isJavaIdentifierPart(text.charAt(identifierStart - 1))) {
            identifierStart--;
        }
        if (identifierStart >= identifierEnd) {
            return null;
        }

        var found = text.substring(identifierStart, identifierEnd);
        if (!found.equals(expectedIdentifier)) {
            var search = text.indexOf(expectedIdentifier + "(", startOffset);
            if (search < 0) {
                return null;
            }
            identifierStart = search;
            identifierEnd = search + expectedIdentifier.length();
        }
        return lookup.rangeAt(identifierStart, identifierEnd);
    }

    private static String bestFunctionCandidate(String sourceText, String unknownName) {
        var candidates = new java.util.LinkedHashSet<String>();
        candidates.add("resolved");
        candidates.add("pending");
        candidates.add("now");
        candidates.add("Failure");
        candidates.add("json");

        if (sourceText != null && !sourceText.isBlank()) {
            var matcher = FUNCTION_DECLARATION.matcher(sourceText);
            while (matcher.find()) {
                candidates.add(matcher.group(1));
            }
        }

        var bestDistance = Integer.MAX_VALUE;
        String bestCandidate = null;
        var threshold = Math.max(1, unknownName.length() / 3);
        for (var candidate : candidates) {
            if (candidate.equals(unknownName)) {
                continue;
            }
            var distance = editDistance(unknownName, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestCandidate = candidate;
            }
        }
        return bestDistance <= threshold ? bestCandidate : null;
    }

    private static int editDistance(String left, String right) {
        var dp = new int[left.length() + 1][right.length() + 1];
        for (var i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (var j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }

        for (var i = 1; i <= left.length(); i++) {
            for (var j = 1; j <= right.length(); j++) {
                var substitutionCost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + substitutionCost
                );
            }
        }
        return dp[left.length()][right.length()];
    }

    private static int findMatchingParen(String text, int open) {
        var depth = 0;
        var inString = false;
        for (var i = open; i < text.length(); i++) {
            var c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '(') depth++;
            if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findClosingBrace(String text, int startOffset) {
        var depth = 0;
        var inString = false;
        for (var i = startOffset; i < text.length(); i++) {
            var c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{') depth++;
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean needsLeadingComma(String text, int startOffset, int closeOffset) {
        for (var i = closeOffset - 1; i >= startOffset; i--) {
            var c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            return c != '{' && c != ',';
        }
        return false;
    }

    private static List<String> splitTopLevelArgs(String inner) {
        var parts = new java.util.ArrayList<String>();
        var depthRound = 0;
        var depthBrace = 0;
        var depthSquare = 0;
        var inString = false;
        var start = 0;
        for (var i = 0; i < inner.length(); i++) {
            var c = inner.charAt(i);
            if (c == '"' && (i == 0 || inner.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (inString) continue;
            switch (c) {
                case '(' -> depthRound++;
                case ')' -> depthRound--;
                case '{' -> depthBrace++;
                case '}' -> depthBrace--;
                case '[' -> depthSquare++;
                case ']' -> depthSquare--;
                case ',' -> {
                    if (depthRound == 0 && depthBrace == 0 && depthSquare == 0) {
                        parts.add(inner.substring(start, i));
                        start = i + 1;
                    }
                }
                default -> {
                }
            }
        }
        if (start <= inner.length()) {
            parts.add(inner.substring(start));
        }
        return parts;
    }

    private record CallParentheses(int openOffset, int closeOffset) {}

    private record SourceTextLookup(String text, int[] lineStarts) {
        static SourceTextLookup from(String sourceText) {
            var safe = sourceText == null ? "" : sourceText;
            var starts = new java.util.ArrayList<Integer>();
            starts.add(0);
            for (var i = 0; i < safe.length(); i++) {
                if (safe.charAt(i) == '\n') {
                    starts.add(i + 1);
                }
            }
            var arr = new int[starts.size()];
            for (var i = 0; i < starts.size(); i++) {
                arr[i] = starts.get(i);
            }
            return new SourceTextLookup(safe, arr);
        }

        int offsetAt(int oneBasedLine, int zeroBasedColumn) {
            var lineIndex = oneBasedLine - 1;
            if (lineIndex < 0 || lineIndex >= lineStarts.length) {
                return -1;
            }
            var start = lineStarts[lineIndex];
            var end = (lineIndex + 1 < lineStarts.length) ? lineStarts[lineIndex + 1] - 1 : text.length();
            var column = Math.max(0, Math.min(zeroBasedColumn, Math.max(0, end - start)));
            return start + column;
        }

        SourceRange rangeAt(int startOffset, int endOffset) {
            var start = lineColAt(startOffset);
            var end = lineColAt(endOffset);
            return new SourceRange(start.line(), start.column(), end.line(), end.column());
        }

        SourceRange rangeAt(int startLine, int startColumn, int endLine, int endColumn) {
            return new SourceRange(startLine, startColumn, endLine, endColumn);
        }

        String line(int oneBasedLine) {
            var lineIndex = oneBasedLine - 1;
            if (lineIndex < 0 || lineIndex >= lineStarts.length) {
                return null;
            }
            var start = lineStarts[lineIndex];
            var end = (lineIndex + 1 < lineStarts.length) ? lineStarts[lineIndex + 1] - 1 : text.length();
            if (end < start) {
                return "";
            }
            return text.substring(start, end);
        }

        private LineCol lineColAt(int offset) {
            var safe = Math.max(0, Math.min(offset, text.length()));
            var line = 1;
            for (var i = 0; i < lineStarts.length; i++) {
                if (lineStarts[i] > safe) {
                    break;
                }
                line = i + 1;
            }
            var lineStart = lineStarts[line - 1];
            return new LineCol(line, safe - lineStart);
        }
    }

    private record LineCol(int line, int column) {}
}

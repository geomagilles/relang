package com.relang.parser;

import com.relang.diagnostics.DiagnosticCode;
import com.relang.diagnostics.DiagnosticCodes;

import java.util.regex.Pattern;

/**
 * Translates ANTLR parser messages into user-facing syntax diagnostics.
 */
final class SyntaxDiagnosticTranslator {

    private static final Pattern MISSING_PATTERN = Pattern.compile("^missing\\s+(.+?)\\s+at\\s+(.+)$");
    private static final Pattern MISMATCHED_PATTERN = Pattern.compile("^mismatched input\\s+(.+?)\\s+expecting\\s+(.+)$");
    private static final Pattern EXTRANEOUS_PATTERN = Pattern.compile("^extraneous input\\s+(.+?)\\s+expecting\\s+(.+)$");
    private static final Pattern NO_VIABLE_PATTERN = Pattern.compile("^no viable alternative at input\\s+(.+)$");
    private static final Pattern TOKEN_QUOTED = Pattern.compile("^'(.+)'$");

    private SyntaxDiagnosticTranslator() {
        // Utility class
    }

    static Result translate(String antlrMessage, String sourceSnippet) {
        if (antlrMessage == null || antlrMessage.isBlank()) {
            return new Result(DiagnosticCodes.SYNTAX_GENERIC, "Syntax error", null);
        }

        var message = antlrMessage.trim();
        var missing = MISSING_PATTERN.matcher(message);
        if (missing.matches()) {
            var expected = normalizeToken(missing.group(1));
            var actual = normalizeToken(missing.group(2));
            return new Result(
                    DiagnosticCodes.SYNTAX_MISSING_TOKEN,
                    "Expected " + expected + " before " + actual + ".",
                    "Insert " + expected + " at this location."
            );
        }

        var mismatched = MISMATCHED_PATTERN.matcher(message);
        if (mismatched.matches()) {
            var actual = normalizeToken(mismatched.group(1));
            var expected = normalizeExpectation(mismatched.group(2));
            return new Result(
                    DiagnosticCodes.SYNTAX_UNEXPECTED_TOKEN,
                    "Expected " + expected + ", found " + actual + ".",
                    "Check the previous token and delimiters near this position."
            );
        }

        var extraneous = EXTRANEOUS_PATTERN.matcher(message);
        if (extraneous.matches()) {
            var actual = normalizeToken(extraneous.group(1));
            var expected = normalizeExpectation(extraneous.group(2));
            return new Result(
                    DiagnosticCodes.SYNTAX_UNEXPECTED_TOKEN,
                    "Unexpected token " + actual + "; expected " + expected + ".",
                    "Remove " + actual + " or adjust surrounding syntax."
            );
        }

        var noViable = NO_VIABLE_PATTERN.matcher(message);
        if (noViable.matches()) {
            var input = normalizeToken(noViable.group(1));
            return new Result(
                    DiagnosticCodes.SYNTAX_UNEXPECTED_TOKEN,
                    "Cannot parse input near " + input + ".",
                    "Check for a missing operator, delimiter, or keyword before this point."
            );
        }

        var genericMessage = sourceSnippet == null || sourceSnippet.isBlank()
                ? "Syntax error."
                : "Syntax error near " + normalizeToken(sourceSnippet) + ".";
        var genericHelp = sourceSnippet == null || sourceSnippet.isBlank()
                ? "Check syntax near this position."
                : "Check syntax near " + normalizeToken(sourceSnippet) + ".";
        return new Result(DiagnosticCodes.SYNTAX_GENERIC, genericMessage, genericHelp);
    }

    private static String normalizeExpectation(String value) {
        if (value == null || value.isBlank()) {
            return "a valid token";
        }
        return value
                .replace("EOF", "end of file")
                .replace("ID", "identifier")
                .replace("INT", "integer")
                .replace("FLOAT", "float")
                .replace("STRING", "string literal");
    }

    private static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "input";
        }
        var text = value.trim();
        var quoted = TOKEN_QUOTED.matcher(text);
        if (quoted.matches()) {
            var inner = quoted.group(1);
            if ("<EOF>".equals(inner) || "EOF".equals(inner)) {
                return "end of file";
            }
            return "'" + inner + "'";
        }
        if ("<EOF>".equals(text) || "EOF".equals(text)) {
            return "end of file";
        }
        return text;
    }

    record Result(DiagnosticCode code, String message, String help) {}
}

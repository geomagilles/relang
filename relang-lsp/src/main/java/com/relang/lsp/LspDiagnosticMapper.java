package com.relang.lsp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.relang.diagnostics.DiagnosticFix;
import com.relang.diagnostics.DiagnosticSeverity;
import com.relang.diagnostics.DiagnosticTextEdit;
import com.relang.diagnostics.ReLangDiagnostic;
import com.relang.diagnostics.SourceRange;

import java.util.ArrayList;

/**
 * Maps ReLang diagnostics to LSP Diagnostic JSON payloads.
 */
final class LspDiagnosticMapper {

    private LspDiagnosticMapper() {
        // Utility class
    }

    static JsonObject toLspDiagnostic(String uri, String sourceText, ReLangDiagnostic diagnostic) {
        var lsp = new JsonObject();
        lsp.add("range", toRange(diagnostic.primaryRange()));
        lsp.addProperty("severity", toSeverity(diagnostic.severity()));
        lsp.addProperty("code", diagnostic.code().value());
        lsp.addProperty("source", "relang");
        lsp.addProperty("message", diagnostic.message());

        var data = new JsonObject();
        if (diagnostic.help() != null && !diagnostic.help().isBlank()) {
            data.addProperty("relangHelp", diagnostic.help());
        }

        var allFixes = new ArrayList<DiagnosticFix>();
        allFixes.addAll(diagnostic.fixes());
        allFixes.addAll(LspQuickFixSuggester.suggest(sourceText, diagnostic));
        if (!allFixes.isEmpty()) {
            data.add("relangQuickFixes", toQuickFixes(allFixes));
        }

        if (data.size() > 0) {
            lsp.add("data", data);
        }

        var related = new JsonArray();
        for (var note : diagnostic.notes()) {
            if (note.range() == null) {
                continue;
            }
            var info = new JsonObject();
            var location = new JsonObject();
            location.addProperty("uri", uri);
            location.add("range", toRange(note.range()));
            info.add("location", location);
            info.addProperty("message", note.message());
            related.add(info);
        }
        if (!related.isEmpty()) {
            lsp.add("relatedInformation", related);
        }

        return lsp;
    }

    private static JsonArray toQuickFixes(Iterable<DiagnosticFix> fixes) {
        var array = new JsonArray();
        for (var fix : fixes) {
            var jsonFix = new JsonObject();
            jsonFix.addProperty("title", fix.title());
            var edits = new JsonArray();
            for (var edit : fix.edits()) {
                edits.add(toQuickFixEdit(edit));
            }
            jsonFix.add("edits", edits);
            array.add(jsonFix);
        }
        return array;
    }

    private static JsonObject toQuickFixEdit(DiagnosticTextEdit edit) {
        var jsonEdit = new JsonObject();
        jsonEdit.add("range", toRange(edit.range()));
        jsonEdit.addProperty("newText", edit.newText());
        return jsonEdit;
    }

    private static JsonObject toRange(SourceRange range) {
        var lspRange = new JsonObject();
        lspRange.add("start", toPosition(range.startLine(), range.startColumn()));
        lspRange.add("end", toPosition(range.endLine(), range.endColumn()));
        return lspRange;
    }

    private static JsonObject toPosition(int oneBasedLine, int zeroBasedColumn) {
        var pos = new JsonObject();
        pos.addProperty("line", Math.max(0, oneBasedLine - 1));
        pos.addProperty("character", Math.max(0, zeroBasedColumn));
        return pos;
    }

    private static int toSeverity(DiagnosticSeverity severity) {
        return switch (severity) {
            case ERROR -> 1;   // LSP DiagnosticSeverity.Error
            case WARNING -> 2; // LSP DiagnosticSeverity.Warning
            case INFO -> 3;    // LSP DiagnosticSeverity.Information
        };
    }
}

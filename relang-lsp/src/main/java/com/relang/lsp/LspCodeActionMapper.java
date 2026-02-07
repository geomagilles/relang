package com.relang.lsp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Builds LSP code actions from diagnostic data payloads.
 */
final class LspCodeActionMapper {

    private LspCodeActionMapper() {
        // Utility class
    }

    static JsonArray toCodeActions(String uri, JsonArray diagnostics) {
        var actions = new JsonArray();
        if (uri == null || diagnostics == null) {
            return actions;
        }

        for (var diagnosticElement : diagnostics) {
            if (!diagnosticElement.isJsonObject()) {
                continue;
            }
            var diagnostic = diagnosticElement.getAsJsonObject();
            var quickFixes = readQuickFixes(diagnostic);
            for (var fix : quickFixes) {
                actions.add(toCodeAction(uri, diagnostic, fix));
            }
        }
        return actions;
    }

    private static JsonArray readQuickFixes(JsonObject diagnostic) {
        var data = diagnostic.get("data");
        if (data == null || !data.isJsonObject()) {
            return new JsonArray();
        }
        var quickFixes = data.getAsJsonObject().get("relangQuickFixes");
        if (quickFixes == null || !quickFixes.isJsonArray()) {
            return new JsonArray();
        }
        return quickFixes.getAsJsonArray();
    }

    private static JsonObject toCodeAction(String uri, JsonObject diagnostic, JsonElement fixElement) {
        var action = new JsonObject();
        action.addProperty("kind", "quickfix");

        if (fixElement.isJsonObject() && fixElement.getAsJsonObject().has("title")) {
            action.addProperty("title", fixElement.getAsJsonObject().get("title").getAsString());
        } else {
            action.addProperty("title", "Apply ReLang quick fix");
        }

        var diagnosticArray = new JsonArray();
        diagnosticArray.add(diagnostic);
        action.add("diagnostics", diagnosticArray);
        action.add("edit", toWorkspaceEdit(uri, fixElement));
        action.addProperty("isPreferred", true);
        return action;
    }

    private static JsonObject toWorkspaceEdit(String uri, JsonElement fixElement) {
        var workspaceEdit = new JsonObject();
        var changes = new JsonObject();
        var edits = new JsonArray();

        if (fixElement.isJsonObject()) {
            var fixObject = fixElement.getAsJsonObject();
            var fixEdits = fixObject.get("edits");
            if (fixEdits != null && fixEdits.isJsonArray()) {
                for (var edit : fixEdits.getAsJsonArray()) {
                    if (edit.isJsonObject()) {
                        edits.add(edit.getAsJsonObject());
                    }
                }
            }
        }

        changes.add(uri, edits);
        workspaceEdit.add("changes", changes);
        return workspaceEdit;
    }
}

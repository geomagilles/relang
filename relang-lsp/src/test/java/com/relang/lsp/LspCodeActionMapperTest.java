package com.relang.lsp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LSP Code Action Mapper")
class LspCodeActionMapperTest {

    @Test
    @DisplayName("converts diagnostic quick fixes into code actions")
    void mapsCodeActions() {
        var diagnostics = new JsonArray();
        var diagnostic = new JsonObject();
        diagnostic.addProperty("message", "Undefined variable 'x'");

        var data = new JsonObject();
        var fixes = new JsonArray();
        var fix = new JsonObject();
        fix.addProperty("title", "Create local variable 'x'");
        var edits = new JsonArray();
        var edit = new JsonObject();
        edit.add("range", range(0, 0, 0, 0));
        edit.addProperty("newText", "let x = /* TODO */\\n");
        edits.add(edit);
        fix.add("edits", edits);
        fixes.add(fix);
        data.add("relangQuickFixes", fixes);
        diagnostic.add("data", data);
        diagnostics.add(diagnostic);

        var actions = LspCodeActionMapper.toCodeActions("file:///tmp/test.re", diagnostics);
        assertEquals(1, actions.size());

        var action = actions.get(0).getAsJsonObject();
        assertEquals("quickfix", action.get("kind").getAsString());
        assertEquals("Create local variable 'x'", action.get("title").getAsString());
        assertTrue(action.has("edit"));
        var editChanges = action.getAsJsonObject("edit").getAsJsonObject("changes");
        assertTrue(editChanges.has("file:///tmp/test.re"));
    }

    private static JsonObject range(int sl, int sc, int el, int ec) {
        var range = new JsonObject();
        var start = new JsonObject();
        start.addProperty("line", sl);
        start.addProperty("character", sc);
        var end = new JsonObject();
        end.addProperty("line", el);
        end.addProperty("character", ec);
        range.add("start", start);
        range.add("end", end);
        return range;
    }
}

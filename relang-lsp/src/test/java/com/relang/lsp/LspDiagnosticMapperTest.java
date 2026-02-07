package com.relang.lsp;

import com.relang.diagnostics.DiagnosticFix;
import com.relang.diagnostics.DiagnosticTextEdit;
import com.relang.diagnostics.DiagnosticCodes;
import com.relang.diagnostics.DiagnosticNote;
import com.relang.diagnostics.ReLangDiagnostic;
import com.relang.diagnostics.SourceRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LSP Diagnostic Mapper")
class LspDiagnosticMapperTest {

    @Test
    @DisplayName("maps code, range, help, and related information")
    void mapsDiagnosticFields() {
        var diagnostic = ReLangDiagnostic.error(
                        DiagnosticCodes.TYPE_UNDEFINED_VARIABLE,
                        "Undefined variable 'x'",
                        new SourceRange(2, 4, 2, 5)
                )
                .withHelp("Declare it first with `let x = ...`.")
                .withFixes(List.of(new DiagnosticFix(
                        "Sample custom fix",
                        List.of(new DiagnosticTextEdit(SourceRange.point(2, 0), "// fix\n"))
                )))
                .withNotes(List.of(new DiagnosticNote("Declared elsewhere", new SourceRange(1, 0, 1, 3))));

        var source = """
                fn main(): Int {
                    x
                }
                """;
        var mapped = LspDiagnosticMapper.toLspDiagnostic("file:///tmp/test.re", source, diagnostic);
        assertEquals("RL2002", mapped.get("code").getAsString());
        assertEquals("relang", mapped.get("source").getAsString());
        assertEquals("Undefined variable 'x'", mapped.get("message").getAsString());
        assertEquals(1, mapped.get("severity").getAsInt());
        assertEquals(1, mapped.getAsJsonObject("range").getAsJsonObject("start").get("line").getAsInt());
        assertEquals(4, mapped.getAsJsonObject("range").getAsJsonObject("start").get("character").getAsInt());
        assertEquals(
                "Declare it first with `let x = ...`.",
                mapped.getAsJsonObject("data").get("relangHelp").getAsString()
        );
        assertTrue(mapped.has("relatedInformation"));
        assertEquals(1, mapped.getAsJsonArray("relatedInformation").size());
        assertTrue(mapped.getAsJsonObject("data").has("relangQuickFixes"));
        assertTrue(mapped.getAsJsonObject("data").getAsJsonArray("relangQuickFixes").size() >= 2);
    }

    @Test
    @DisplayName("adds suggested quick fix for missing type annotation")
    void addsMissingTypeAnnotationFix() {
        var diagnostic = ReLangDiagnostic.error(
                DiagnosticCodes.TYPE_MISSING_TYPE_ANNOTATION,
                "Parameter 'x' must have a type annotation",
                new SourceRange(1, 7, 1, 8)
        );
        var source = "fn test(x) = x;";

        var mapped = LspDiagnosticMapper.toLspDiagnostic("file:///tmp/test.re", source, diagnostic);
        var fixes = mapped.getAsJsonObject("data").getAsJsonArray("relangQuickFixes");
        assertTrue(fixes.size() >= 1);

        var firstFix = fixes.get(0).getAsJsonObject();
        assertTrue(firstFix.get("title").getAsString().contains("type annotation"));
        var edit = firstFix.getAsJsonArray("edits").get(0).getAsJsonObject();
        assertEquals(": Int", edit.get("newText").getAsString());
    }
}

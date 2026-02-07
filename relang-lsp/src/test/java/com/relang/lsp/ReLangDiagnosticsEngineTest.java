package com.relang.lsp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReLang Diagnostics Engine")
class ReLangDiagnosticsEngineTest {

    @Test
    @DisplayName("arity mismatch includes function declaration as related information")
    void arityMismatchIncludesDeclarationNote() {
        var source = """
                fn add(a: Int, b: Int): Int = a + b;
                add(1);
                """;

        var diagnostics = ReLangDiagnosticsEngine.analyze("file:///tmp/test.re", source);
        assertFalse(diagnostics.isEmpty(), "Expected at least one diagnostic");

        var first = diagnostics.getFirst();
        assertEquals("RL2003", first.code().value());
        assertFalse(first.notes().isEmpty(), "Expected declaration note");
        assertTrue(first.notes().getFirst().message().contains("declared"));

        var mapped = LspDiagnosticMapper.toLspDiagnostic("file:///tmp/test.re", source, first);
        assertTrue(mapped.has("relatedInformation"), "Expected LSP relatedInformation");
        var related = mapped.getAsJsonArray("relatedInformation");
        assertFalse(related.isEmpty());
    }
}

package com.relang.diagnostics;

import com.relang.parser.SyntaxError;
import com.relang.parser.TypeError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Diagnostic Codes")
class DiagnosticCodesTest {

    @Test
    @DisplayName("Catalog exposes mandatory fallback codes")
    void fallbackCodes() {
        assertEquals("RL1000", DiagnosticCodes.fallbackFor(DiagnosticCategory.SYNTAX).value());
        assertEquals("RL2000", DiagnosticCodes.fallbackFor(DiagnosticCategory.TYPE).value());
        assertEquals("RL3000", DiagnosticCodes.fallbackFor(DiagnosticCategory.RUNTIME).value());
        assertEquals("RL9000", DiagnosticCodes.fallbackFor(DiagnosticCategory.INTERNAL).value());
        assertTrue(DiagnosticCodes.find("RL3001").isPresent());
        assertTrue(DiagnosticCodes.find("RL3002").isPresent());
        assertTrue(DiagnosticCodes.find("RL3003").isPresent());
        assertTrue(DiagnosticCodes.find("RL3004").isPresent());
        assertTrue(DiagnosticCodes.find("RL3005").isPresent());
    }

    @Test
    @DisplayName("TypeError defaults to RL2000 and converts to structured diagnostic")
    void typeErrorDefaultCode() {
        var error = new TypeError(3, 12, "Type mismatch: expected Int, found String", "\"hello\"");

        assertEquals("RL2000", error.code());
        var diagnostic = error.toDiagnostic();
        assertEquals("RL2000", diagnostic.code().value());
        assertEquals(DiagnosticSeverity.ERROR, diagnostic.severity());
        assertEquals(3, diagnostic.primaryRange().startLine());
        assertEquals(12, diagnostic.primaryRange().startColumn());
    }

    @Test
    @DisplayName("TypeError preserves secondary notes in structured diagnostic")
    void typeErrorCarriesNotes() {
        var note = new DiagnosticNote("Variable declared here", SourceRange.point(1, 4));
        var error = new TypeError(
                DiagnosticCodes.TYPE_MISMATCH,
                3,
                12,
                "Cannot assign String to Int",
                "\"hello\"",
                "Use an Int value.",
                java.util.List.of(note)
        );

        var diagnostic = error.toDiagnostic();
        assertEquals(1, diagnostic.notes().size());
        assertEquals("Variable declared here", diagnostic.notes().getFirst().message());
    }

    @Test
    @DisplayName("SyntaxError defaults to RL1000 and keeps legacy formatting prefix")
    void syntaxErrorDefaultCode() {
        var error = new SyntaxError(1, 0, "Expected expression, found <EOF>", null);

        assertEquals("RL1000", error.code());
        assertTrue(error.format().startsWith("SyntaxError[RL1000]"));
        var diagnostic = error.toDiagnostic();
        assertEquals("RL1000", diagnostic.code().value());
    }
}

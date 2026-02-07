package com.relang.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Syntax Diagnostic Translator")
class SyntaxDiagnosticTranslatorTest {

    @Test
    @DisplayName("mismatched input is translated to RL1001")
    void mismatchedInput() {
        var result = SyntaxDiagnosticTranslator.translate(
                "mismatched input '<EOF>' expecting ID",
                null
        );

        assertEquals("RL1001", result.code().value());
        assertEquals("Expected identifier, found end of file.", result.message());
        assertNotNull(result.help());
    }

    @Test
    @DisplayName("missing token is translated to RL1002")
    void missingToken() {
        var result = SyntaxDiagnosticTranslator.translate(
                "missing ')' at 'return'",
                "return"
        );

        assertEquals("RL1002", result.code().value());
        assertEquals("Expected ')' before 'return'.", result.message());
        assertTrue(result.help().contains("Insert ')'"), "Expected insertion hint, got: " + result.help());
    }

    @Test
    @DisplayName("extraneous input is translated to RL1001")
    void extraneousInput() {
        var result = SyntaxDiagnosticTranslator.translate(
                "extraneous input '}' expecting ';'",
                "}"
        );

        assertEquals("RL1001", result.code().value());
        assertTrue(result.message().startsWith("Unexpected token"));
        assertTrue(result.help().contains("Remove"), "Expected removal hint, got: " + result.help());
    }

    @Test
    @DisplayName("unknown format falls back to RL1000")
    void fallback() {
        var result = SyntaxDiagnosticTranslator.translate(
                "totally custom parser failure",
                "oops"
        );

        assertEquals("RL1000", result.code().value());
        assertEquals("totally custom parser failure", result.message());
        assertTrue(result.help().contains("oops"));
    }
}

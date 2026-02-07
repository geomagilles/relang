package com.relang.diagnostics;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Diagnostic Rendering Integration")
class DiagnosticRenderingIntegrationTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("Undefined variable emits specific diagnostic code RL2002")
    void undefinedVariableCode() {
        var ex = expectFailure("x = 1;");
        assertTrue(ex.getMessage().contains("RL2002"), "Expected RL2002 in message, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("help:"), "Expected help hint in message, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Arity mismatch emits specific diagnostic code RL2003")
    void arityMismatchCode() {
        var ex = expectFailure("fn add(a: Int, b: Int): Int = a + b; add(1);");
        assertTrue(ex.getMessage().contains("RL2003"), "Expected RL2003 in message, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Syntax failure emits RL1xxx diagnostic code")
    void syntaxCode() {
        var ex = expectFailure("let broken =");
        assertTrue(ex.getMessage().contains("RL10"), "Expected RL1xxx code in message, got: " + ex.getMessage());
    }

    private PolyglotException expectFailure(String source) {
        return org.junit.jupiter.api.Assertions.assertThrows(
                PolyglotException.class,
                () -> context.eval("relang", source)
        );
    }
}

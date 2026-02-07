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

    @Test
    @DisplayName("Non-exhaustive match emits RL3003 runtime diagnostic code")
    void runtimeNonExhaustiveMatchCode() {
        var ex = expectFailure("match 1 { 2 -> 2 };");
        assertTrue(ex.getMessage().contains("RL3003"), "Expected RL3003 in message, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("help:"), "Expected help hint in message, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Unknown named argument emits RL3004 runtime diagnostic code")
    void runtimeInvalidNamedArgumentCode() {
        var ex = expectFailure("fn add(a: Int, b: Int): Int = a + b; add(a: 1, c: 2);");
        assertTrue(ex.getMessage().contains("RL3004"), "Expected RL3004 in message, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("help:"), "Expected help hint in message, got: " + ex.getMessage());
    }

    private PolyglotException expectFailure(String source) {
        return org.junit.jupiter.api.Assertions.assertThrows(
                PolyglotException.class,
                () -> context.eval("relang", source)
        );
    }
}

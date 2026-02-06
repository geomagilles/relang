package com.relang.variables;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for variable declarations (spec sections 2.1, 2.2).
 * Variables are declared with let, inferred or annotated types.
 */
@DisplayName("Variable declarations")
public class VariableDeclarationTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- 2.1 Simple declarations ---

    @Nested
    @DisplayName("Simple let declarations")
    class SimpleLet {

        @Test
        @DisplayName("let with Int value")
        void testLetInt() {
            assertEval("let x = 10; x;", 10);
        }

        @Test
        @DisplayName("let with Float value")
        void testLetFloat() {
            assertEval("let x = 3.14; x;", 3.14);
        }

        @Test
        @DisplayName("let with Bool value")
        void testLetBool() {
            assertEval("let x = true; x;", true);
        }

        @Test
        @DisplayName("let with String value")
        void testLetString() {
            assertEval("let x = \"hello\"; x;", "hello");
        }

        @Test
        @DisplayName("let with none value")
        void testLetNone() {
            Value result = context.eval("relang", "let x = none; x;");
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("let with unit value")
        void testLetUnit() {
            Value result = context.eval("relang", "let x = unit; x;");
            assertEquals("unit", result.toString());
        }

        @Test
        @DisplayName("let with expression")
        void testLetExpression() {
            assertEval("let x = 2 + 3; x;", 5);
        }

        @Test
        @DisplayName("let with complex expression")
        void testLetComplexExpression() {
            assertEval("let x = (2 + 3) * 4; x;", 20);
        }

        @Test
        @DisplayName("let with function call result")
        void testLetFunctionCall() {
            var src = """
                fn double(x: Int): Int = x * 2;
                let result = double(21);
                result;
            """;
            assertEval(src, 42);
        }

        @Test
        @DisplayName("let with comparison result")
        void testLetComparison() {
            assertEval("let bigger = 10 > 5; bigger;", true);
        }
    }

    // --- Multiple declarations ---

    @Nested
    @DisplayName("Multiple declarations")
    class MultipleDeclarations {

        @Test
        @DisplayName("multiple sequential lets")
        void testMultipleLets() {
            var src = """
                let a = 10;
                let b = 20;
                let c = 30;
                a + b + c;
            """;
            assertEval(src, 60);
        }

        @Test
        @DisplayName("let depending on previous let")
        void testDependentLets() {
            var src = """
                let a = 10;
                let b = a + 5;
                let c = a + b;
                c;
            """;
            assertEval(src, 25);
        }

        @Test
        @DisplayName("different types in sequence")
        void testMixedTypeLets() {
            var src = """
                let n = 42;
                let s = "answer";
                let b = true;
                s;
            """;
            assertEval(src, "answer");
        }
    }

    // --- Legacy assignment (without let) ---

    @Nested
    @DisplayName("Legacy assignment (without let)")
    class LegacyAssignment {

        @Test
        @DisplayName("assignment without let")
        void testAssignmentWithoutLet() {
            assertEval("x = 10; x;", 10);
        }

        @Test
        @DisplayName("multiple assignments without let")
        void testMultipleAssignments() {
            assertEval("x = 10; y = 20; x + y;", 30);
        }
    }

    // --- 2.2 Declaration with optional ---

    @Nested
    @DisplayName("Optional declarations")
    class OptionalDeclarations {

        @Test
        @DisplayName("variable initialized to none")
        void testOptionalNone() {
            Value result = context.eval("relang", "let email = none; email;");
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("optional variable later assigned a value")
        void testOptionalThenAssign() {
            assertEval("let email = none; email = \"alice@example.com\"; email;", "alice@example.com");
        }
    }

    // --- Helpers ---

    private void assertEval(String source, long expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asLong());
    }

    private void assertEval(String source, double expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asDouble(), 0.0001);
    }

    private void assertEval(String source, boolean expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asBoolean());
    }

    private void assertEval(String source, String expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asString());
    }
}

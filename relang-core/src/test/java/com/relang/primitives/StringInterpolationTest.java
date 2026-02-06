package com.relang.primitives;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for string interpolation (GitHub Issue #5).
 * Spec: relang-primitives-proposal.md section 3.2
 */
@DisplayName("String interpolation")
public class StringInterpolationTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Nested
    @DisplayName("Basic interpolation")
    class BasicInterpolation {

        @Test
        @DisplayName("interpolate a String variable")
        void testSimpleVariable() {
            var src = """
                let name = "Alice";
                "Hello ${name}";
            """;
            assertEval(src, "Hello Alice");
        }

        @Test
        @DisplayName("interpolate an Int variable")
        void testIntVariable() {
            var src = """
                let age = 30;
                "Age: ${age}";
            """;
            assertEval(src, "Age: 30");
        }

        @Test
        @DisplayName("interpolate multiple variables")
        void testMultipleVariables() {
            var src = """
                let name = "Alice";
                let age = 30;
                "${name} is ${age} years old";
            """;
            assertEval(src, "Alice is 30 years old");
        }

        @Test
        @DisplayName("interpolate expression")
        void testExpression() {
            var src = """
                let x = 10;
                "Result: ${x + 5}";
            """;
            assertEval(src, "Result: 15");
        }

        @Test
        @DisplayName("interpolate Bool value")
        void testBoolInterpolation() {
            var src = """
                let active = true;
                "Active: ${active}";
            """;
            assertEval(src, "Active: true");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("escaped dollar sign is not interpolated")
        void testEscapedDollar() {
            assertEval("\"Price: \\$100\";", "Price: $100");
        }

        @Test
        @DisplayName("string with no interpolation still works")
        void testNoInterpolation() {
            assertEval("\"Hello world\";", "Hello world");
        }

        @Test
        @DisplayName("empty string still works")
        void testEmptyString() {
            assertEval("\"\";", "");
        }

        @Test
        @DisplayName("interpolation at start of string")
        void testInterpolationAtStart() {
            var src = """
                let x = 42;
                "${x} is the answer";
            """;
            assertEval(src, "42 is the answer");
        }

        @Test
        @DisplayName("interpolation at end of string")
        void testInterpolationAtEnd() {
            var src = """
                let x = 42;
                "The answer is ${x}";
            """;
            assertEval(src, "The answer is 42");
        }

        @Test
        @DisplayName("adjacent interpolations")
        void testAdjacentInterpolations() {
            var src = """
                let a = "Hello";
                let b = "World";
                "${a}${b}";
            """;
            assertEval(src, "HelloWorld");
        }

        @Test
        @DisplayName("interpolation with function call")
        void testFunctionCallInterpolation() {
            var src = """
                fn double(x: Int): Int = x * 2;
                "Double of 5 is ${double(5)}";
            """;
            assertEval(src, "Double of 5 is 10");
        }
    }

    // --- Helpers ---

    private void assertEval(String source, String expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asString());
    }
}

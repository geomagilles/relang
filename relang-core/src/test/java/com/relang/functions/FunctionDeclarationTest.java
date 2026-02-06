package com.relang.functions;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests for function declaration forms (spec sections 2.1, 2.2).
 * Block body vs expression body, with and without type annotations.
 */
@DisplayName("Function declarations")
public class FunctionDeclarationTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- 2.1 Block body ---

    @Nested
    @DisplayName("Block body")
    class BlockBody {

        @Test
        @DisplayName("block body with explicit return type")
        void testBlockBodyExplicit() {
            var src = """
                fn double(x: Int): Int {
                    x * 2
                }
                double(5);
            """;
            assertEval(src, 10);
        }

        @Test
        @DisplayName("block body with inferred return type")
        void testBlockBodyInferred() {
            var src = """
                fn double(x: Int) {
                    x * 2
                }
                double(5);
            """;
            assertEval(src, 10);
        }

        @Test
        @DisplayName("block body with multiple statements")
        void testBlockBodyMultiStatements() {
            var src = """
                fn process(x: Int): Int {
                    let y = x * 2;
                    let z = y + 1;
                    z
                }
                process(10);
            """;
            assertEval(src, 21);
        }

        @Test
        @DisplayName("block body with String return")
        void testBlockBodyString() {
            var src = """
                fn greet(name: String): String {
                    "Hello " + name
                }
                greet("World");
            """;
            assertEval(src, "Hello World");
        }

        @Test
        @DisplayName("block body with Bool return")
        void testBlockBodyBool() {
            var src = """
                fn isPositive(n: Int): Bool {
                    n > 0
                }
                isPositive(5);
            """;
            assertEval(src, true);
        }

        @Test
        @DisplayName("block body returning Unit")
        void testBlockBodyUnit() {
            var src = """
                fn doNothing(): Unit {
                    unit
                }
                doNothing();
            """;
            Value result = context.eval("relang", src);
            assertFalse(result.isNull());
            assertEquals("unit", result.toString());
        }
    }

    // --- 2.1 Expression body ---

    @Nested
    @DisplayName("Expression body")
    class ExpressionBody {

        @Test
        @DisplayName("expression body with explicit return type")
        void testExprBodyExplicit() {
            assertEval("fn double(x: Int): Int = x * 2; double(5);", 10);
        }

        @Test
        @DisplayName("expression body with inferred return type")
        void testExprBodyInferred() {
            assertEval("fn double(x: Int) = x * 2; double(5);", 10);
        }

        @Test
        @DisplayName("expression body with param type, inferred return")
        void testExprBodyParamTyped() {
            assertEval("fn double(x: Int) = x * 2; double(5);", 10);
        }

        @Test
        @DisplayName("expression body with addition")
        void testExprBodyAdd() {
            assertEval("fn add(a: Int, b: Int): Int = a + b; add(3, 4);", 7);
        }

        @Test
        @DisplayName("expression body returning String")
        void testExprBodyString() {
            assertEval("fn hello(): String = \"hello\"; hello();", "hello");
        }

        @Test
        @DisplayName("expression body returning Bool")
        void testExprBodyBool() {
            assertEval("fn isZero(n: Int): Bool = n == 0; isZero(0);", true);
        }

        @Test
        @DisplayName("expression body with complex expression")
        void testExprBodyComplex() {
            assertEval("fn calc(a: Int, b: Int) = (a + b) * (a - b); calc(5, 3);", 16);
        }

        @Test
        @DisplayName("expression body with if-else")
        void testExprBodyIfElse() {
            var src = """
                fn max(a: Int, b: Int): Int = if a > b { a } else { b };
                max(10, 20);
            """;
            assertEval(src, 20);
        }
    }

    // --- No parameters ---

    @Nested
    @DisplayName("No parameters")
    class NoParameters {

        @Test
        @DisplayName("function with no parameters returning Int")
        void testNoParamsInt() {
            assertEval("fn answer(): Int { 42 } answer();", 42);
        }

        @Test
        @DisplayName("function with no parameters returning String")
        void testNoParamsString() {
            assertEval("fn version(): String = \"1.0.0\"; version();", "1.0.0");
        }

        @Test
        @DisplayName("function with no parameters and no type")
        void testNoParamsNoType() {
            assertEval("fn pi() = 3.14; pi();", 3.14);
        }
    }

    // --- Multiple functions ---

    @Nested
    @DisplayName("Multiple functions")
    class MultipleFunctions {

        @Test
        @DisplayName("calling one function from another")
        void testCallChain() {
            var src = """
                fn double(x: Int): Int = x * 2;
                fn quadruple(x: Int): Int = double(double(x));
                quadruple(3);
            """;
            assertEval(src, 12);
        }

        @Test
        @DisplayName("multiple independent functions")
        void testIndependentFunctions() {
            var src = """
                fn add(a: Int, b: Int): Int = a + b;
                fn mul(a: Int, b: Int): Int = a * b;
                add(3, 4) + mul(2, 5);
            """;
            assertEval(src, 17); // 7 + 10
        }

        @Test
        @DisplayName("function used in expression")
        void testFunctionInExpression() {
            var src = """
                fn square(x: Int): Int = x * x;
                let a = square(3);
                let b = square(4);
                a + b;
            """;
            assertEval(src, 25); // 9 + 16
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

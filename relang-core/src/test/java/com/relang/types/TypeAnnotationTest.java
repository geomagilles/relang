package com.relang.types;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for type annotations on function parameters and return types (spec section 2).
 * Type annotations are parsed but not enforced at runtime in v0.1.
 */
@DisplayName("Type annotations")
public class TypeAnnotationTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- Function parameter type annotations ---

    @Nested
    @DisplayName("Function parameter annotations")
    class FunctionParams {

        @Test
        @DisplayName("Int parameters")
        void testIntParams() {
            assertEval("fn add(a: Int, b: Int): Int { return a + b; } add(3, 4);", 7);
        }

        @Test
        @DisplayName("String parameters")
        void testStringParams() {
            var src = """
                fn greet(name: String): String {
                    return "Hello " + name;
                }
                greet("World");
            """;
            assertEval(src, "Hello World");
        }

        @Test
        @DisplayName("Bool parameters")
        void testBoolParams() {
            var src = """
                fn toggle(b: Bool): Bool { not b }
                toggle(true);
            """;
            assertEval(src, false);
        }

        @Test
        @DisplayName("Float parameters")
        void testFloatParams() {
            assertEval("fn half(x: Float): Float = x / 2.0; half(10.0);", 5.0);
        }

        @Test
        @DisplayName("mixed parameter types")
        void testMixedParams() {
            var src = """
                fn describe(name: String, age: Int): String {
                    name
                }
                describe("Alice", 30);
            """;
            assertEval(src, "Alice");
        }
    }

    // --- Return type annotations ---

    @Nested
    @DisplayName("Return type annotations")
    class ReturnTypes {

        @Test
        @DisplayName("Int return type")
        void testIntReturn() {
            assertEval("fn square(x: Int): Int { x * x } square(5);", 25);
        }

        @Test
        @DisplayName("String return type")
        void testStringReturn() {
            assertEval("fn hello(): String { \"hello\" } hello();", "hello");
        }

        @Test
        @DisplayName("Bool return type")
        void testBoolReturn() {
            assertEval("fn isZero(n: Int): Bool { n == 0 } isZero(0);", true);
        }

        @Test
        @DisplayName("Unit return type annotation")
        void testUnitReturn() {
            var src = """
                fn doWork(): Unit {
                    let x = 42;
                    unit
                }
                doWork();
            """;
            Value result = context.eval("relang", src);
            assertEquals("unit", result.toString());
        }
    }

    // --- Optional type annotations ---

    @Nested
    @DisplayName("Optional type annotations (T?)")
    class OptionalAnnotations {

        @Test
        @DisplayName("optional return type annotation parsed")
        void testOptionalReturnType() {
            var src = """
                fn findOrNone(x: Int): Int? {
                    if x > 0 { x } else { none }
                }
                findOrNone(5);
            """;
            assertEval(src, 5);
        }

        @Test
        @DisplayName("optional parameter type annotation parsed")
        void testOptionalParamType() {
            var src = """
                fn describe(x: Int?): String {
                    match x {
                        none -> "absent",
                        _ -> "present"
                    }
                }
                describe(none);
            """;
            assertEval(src, "absent");
        }
    }

    // --- Inferred return types ---

    @Nested
    @DisplayName("Inferred return types")
    class InferredReturnTypes {

        @Test
        @DisplayName("expression body with inferred return type")
        void testExprBodyInferred() {
            assertEval("fn square(x: Int) = x * x; square(6);", 36);
        }

        @Test
        @DisplayName("block body with inferred return type")
        void testBlockBodyInferred() {
            assertEval("fn add(a: Int, b: Int) { a + b } add(3, 4);", 7);
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

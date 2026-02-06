package com.relang.functions;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for function parameters (spec section 2.3).
 * Typed parameters, default values, named arguments.
 */
@DisplayName("Function parameters")
public class FunctionParameterTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- Typed parameters ---

    @Nested
    @DisplayName("Typed parameters")
    class TypedParams {

        @Test
        @DisplayName("single Int parameter")
        void testSingleIntParam() {
            assertEval("fn double(x: Int): Int { return x * 2; } double(5);", 10);
        }

        @Test
        @DisplayName("two Int parameters")
        void testTwoIntParams() {
            assertEval("fn add(a: Int, b: Int): Int { return a + b; } add(3, 4);", 7);
        }

        @Test
        @DisplayName("three parameters")
        void testThreeParams() {
            var src = """
                fn sum3(a: Int, b: Int, c: Int): Int {
                    a + b + c
                }
                sum3(1, 2, 3);
            """;
            assertEval(src, 6);
        }

        @Test
        @DisplayName("String parameter")
        void testStringParam() {
            var src = """
                fn greet(name: String): String {
                    return "Hello " + name;
                }
                greet("World");
            """;
            assertEval(src, "Hello World");
        }

        @Test
        @DisplayName("Bool parameter")
        void testBoolParam() {
            var src = """
                fn toggle(b: Bool): Bool { not b }
                toggle(true);
            """;
            assertEval(src, false);
        }

        @Test
        @DisplayName("Float parameter")
        void testFloatParam() {
            assertEval("fn half(x: Float): Float = x / 2.0; half(10.0);", 5.0);
        }

        @Test
        @DisplayName("mixed type parameters")
        void testMixedTypes() {
            var src = """
                fn repeat(s: String, n: Int): String {
                    let result = "";
                    for i in 0..n {
                        result = result + s;
                    }
                    result
                }
                repeat("ab", 3);
            """;
            assertEval(src, "ababab");
        }

        @Test
        @DisplayName("untyped parameters are rejected")
        void testUntypedParams() {
            var ex = org.junit.jupiter.api.Assertions.assertThrows(
                    org.graalvm.polyglot.PolyglotException.class,
                    () -> context.eval("relang", "fn add(a, b) { return a + b; } add(3, 4);"));
            org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("must have a type annotation"));
        }

        @Test
        @DisplayName("partially typed parameters are rejected")
        void testPartiallyTyped() {
            var ex = org.junit.jupiter.api.Assertions.assertThrows(
                    org.graalvm.polyglot.PolyglotException.class,
                    () -> context.eval("relang", "fn add(a: Int, b) { return a + b; } add(3, 4);"));
            org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("must have a type annotation"));
        }
    }

    // --- Default values ---

    @Nested
    @DisplayName("Default values")
    class DefaultValues {

        @Test
        @DisplayName("single default parameter, not provided")
        void testSingleDefaultNotProvided() {
            var src = """
                fn greet(name: String, greeting: String = "Hello"): String {
                    return greeting + " " + name;
                }
                greet("Alice");
            """;
            assertEval(src, "Hello Alice");
        }

        @Test
        @DisplayName("single default parameter, overridden")
        void testSingleDefaultOverridden() {
            var src = """
                fn greet(name: String, greeting: String = "Hello"): String {
                    return greeting + " " + name;
                }
                greet("Alice", "Hi");
            """;
            assertEval(src, "Hi Alice");
        }

        @Test
        @DisplayName("multiple default parameters, none provided")
        void testMultipleDefaultsNone() {
            var src = """
                fn make(a: Int, b: Int = 10, c: Int = 100): Int {
                    return a + b + c;
                }
                make(1);
            """;
            assertEval(src, 111);
        }

        @Test
        @DisplayName("multiple default parameters, partial override")
        void testMultipleDefaultsPartial() {
            var src = """
                fn make(a: Int, b: Int = 10, c: Int = 100): Int {
                    return a + b + c;
                }
                make(1, 20);
            """;
            assertEval(src, 121);
        }

        @Test
        @DisplayName("multiple default parameters, all overridden")
        void testMultipleDefaultsAllOverridden() {
            var src = """
                fn make(a: Int, b: Int = 10, c: Int = 100): Int {
                    return a + b + c;
                }
                make(1, 2, 3);
            """;
            assertEval(src, 6);
        }

        @Test
        @DisplayName("default value is an expression")
        void testDefaultExpression() {
            var src = """
                fn make(a: Int, b: Int = 2 + 3): Int {
                    return a + b;
                }
                make(10);
            """;
            assertEval(src, 15);
        }

        @Test
        @DisplayName("default String value")
        void testDefaultString() {
            var src = """
                fn tag(value: String, prefix: String = "["): String {
                    return prefix + value + "]";
                }
                tag("ok");
            """;
            assertEval(src, "[ok]");
        }

        @Test
        @DisplayName("default Bool value")
        void testDefaultBool() {
            var src = """
                fn check(value: Int, strict: Bool = true): Bool {
                    if strict { value > 0 } else { value >= 0 }
                }
                check(0);
            """;
            assertEval(src, false);
        }

        @Test
        @DisplayName("default Bool value overridden")
        void testDefaultBoolOverridden() {
            var src = """
                fn check(value: Int, strict: Bool = true): Bool {
                    if strict { value > 0 } else { value >= 0 }
                }
                check(0, false);
            """;
            assertEval(src, true);
        }
    }

    // --- Named arguments ---

    @Nested
    @DisplayName("Named arguments")
    class NamedArguments {

        @Test
        @DisplayName("all named, same order")
        void testAllNamedSameOrder() {
            var src = """
                fn add(a: Int, b: Int): Int { return a + b; }
                add(a: 10, b: 20);
            """;
            assertEval(src, 30);
        }

        @Test
        @DisplayName("all named, reversed order")
        void testAllNamedReversed() {
            var src = """
                fn add(a: Int, b: Int): Int { return a + b; }
                add(b: 20, a: 10);
            """;
            assertEval(src, 30);
        }

        @Test
        @DisplayName("named arguments with three parameters")
        void testThreeNamedArgs() {
            var src = """
                fn compute(a: Int, b: Int, c: Int): Int {
                    return a * 100 + b * 10 + c;
                }
                compute(c: 3, a: 1, b: 2);
            """;
            assertEval(src, 123);
        }

        @Test
        @DisplayName("named arguments verify correct assignment")
        void testNamedCorrectAssignment() {
            var src = """
                fn sub(a: Int, b: Int): Int { return a - b; }
                sub(b: 3, a: 10);
            """;
            assertEval(src, 7); // 10 - 3, not 3 - 10
        }
    }

    // --- Mixing positional and named ---

    @Nested
    @DisplayName("Mixed positional and named")
    class MixedArgs {

        @Test
        @DisplayName("first positional, rest named")
        void testFirstPositionalRestNamed() {
            var src = """
                fn compute(a: Int, b: Int, c: Int): Int {
                    return a + b + c;
                }
                compute(1, c: 3, b: 2);
            """;
            assertEval(src, 6);
        }

        @Test
        @DisplayName("two positional, one named")
        void testTwoPositionalOneNamed() {
            var src = """
                fn compute(a: Int, b: Int, c: Int): Int {
                    return a * 100 + b * 10 + c;
                }
                compute(1, 2, c: 3);
            """;
            assertEval(src, 123);
        }

        @Test
        @DisplayName("positional with named default override")
        void testPositionalWithNamedDefault() {
            var src = """
                fn greet(name: String, greeting: String = "Hello"): String {
                    return greeting + " " + name;
                }
                greet("Alice", greeting: "Hey");
            """;
            assertEval(src, "Hey Alice");
        }

        @Test
        @DisplayName("named argument to skip default")
        void testNamedSkipDefault() {
            var src = """
                fn make(a: Int, b: Int = 10, c: Int = 100): Int {
                    return a + b + c;
                }
                make(1, c: 50);
            """;
            assertEval(src, 61); // 1 + 10 + 50
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

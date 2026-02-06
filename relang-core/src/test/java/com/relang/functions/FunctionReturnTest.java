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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for function return semantics (spec sections 2.4, 3.1, 3.2).
 * Implicit return, early return, return type annotations.
 */
@DisplayName("Function return semantics")
public class FunctionReturnTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- 3.1 Implicit return ---

    @Nested
    @DisplayName("Implicit return")
    class ImplicitReturn {

        @Test
        @DisplayName("last expression is return value (Int)")
        void testLastExpressionInt() {
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
        @DisplayName("last expression is return value (String)")
        void testLastExpressionString() {
            var src = """
                fn greet(name: String): String {
                    let msg = "Hello " + name;
                    msg
                }
                greet("World");
            """;
            assertEval(src, "Hello World");
        }

        @Test
        @DisplayName("if-expression as implicit return")
        void testIfExpressionReturn() {
            var src = """
                fn max(a: Int, b: Int): Int {
                    if a > b { a } else { b }
                }
                max(10, 20);
            """;
            assertEval(src, 20);
        }

        @Test
        @DisplayName("match-expression as implicit return")
        void testMatchExpressionReturn() {
            var src = """
                fn describe(n: Int): String {
                    match n {
                        0 -> "zero",
                        1 -> "one",
                        _ -> "many"
                    }
                }
                describe(1);
            """;
            assertEval(src, "one");
        }

        @Test
        @DisplayName("match-expression implicit return: zero")
        void testMatchReturnZero() {
            var src = """
                fn describe(n: Int): String {
                    match n {
                        0 -> "zero",
                        1 -> "one",
                        _ -> "many"
                    }
                }
                describe(0);
            """;
            assertEval(src, "zero");
        }

        @Test
        @DisplayName("match-expression implicit return: wildcard")
        void testMatchReturnWildcard() {
            var src = """
                fn describe(n: Int): String {
                    match n {
                        0 -> "zero",
                        1 -> "one",
                        _ -> "many"
                    }
                }
                describe(99);
            """;
            assertEval(src, "many");
        }

        @Test
        @DisplayName("subjectless match as implicit return")
        void testSubjectlessMatchReturn() {
            var src = """
                fn grade(score: Int): String {
                    match {
                        score >= 90 -> "A",
                        score >= 80 -> "B",
                        score >= 70 -> "C",
                        _ -> "F"
                    }
                }
                grade(85);
            """;
            assertEval(src, "B");
        }

        @Test
        @DisplayName("arithmetic expression as implicit return")
        void testArithmeticReturn() {
            var src = """
                fn sumOfSquares(a: Int, b: Int): Int {
                    let sa = a * a;
                    let sb = b * b;
                    sa + sb
                }
                sumOfSquares(3, 4);
            """;
            assertEval(src, 25);
        }

        @Test
        @DisplayName("single expression body is implicit return")
        void testSingleExprBody() {
            var src = """
                fn identity(x: Int): Int {
                    x
                }
                identity(42);
            """;
            assertEval(src, 42);
        }
    }

    // --- 3.2 Early return ---

    @Nested
    @DisplayName("Early return")
    class EarlyReturn {

        @Test
        @DisplayName("early return in if block")
        void testEarlyReturnIf() {
            var src = """
                fn abs(x: Int): Int {
                    if x < 0 { return -(x); }
                    x
                }
                abs(-(5));
            """;
            assertEval(src, 5);
        }

        @Test
        @DisplayName("early return not taken")
        void testEarlyReturnNotTaken() {
            var src = """
                fn abs(x: Int): Int {
                    if x < 0 { return -(x); }
                    x
                }
                abs(5);
            """;
            assertEval(src, 5);
        }

        @Test
        @DisplayName("early return with value")
        void testEarlyReturnValue() {
            var src = """
                fn safeDivide(a: Int, b: Int): Int {
                    if b == 0 { return 0; }
                    a / b
                }
                safeDivide(10, 0);
            """;
            assertEval(src, 0);
        }

        @Test
        @DisplayName("normal return when guard passes")
        void testNormalReturnAfterGuard() {
            var src = """
                fn safeDivide(a: Int, b: Int): Int {
                    if b == 0 { return 0; }
                    a / b
                }
                safeDivide(10, 2);
            """;
            assertEval(src, 5);
        }

        @Test
        @DisplayName("explicit return in simple function")
        void testExplicitReturn() {
            var src = """
                fn add(a: Int, b: Int): Int {
                    return a + b;
                }
                add(3, 4);
            """;
            assertEval(src, 7);
        }

        @Test
        @DisplayName("early return in loop")
        void testEarlyReturnInLoop() {
            var src = """
                fn findFirst(target: Int): Int {
                    for i in 0..100 {
                        if i == target { return i; }
                    }
                    -(1)
                }
                findFirst(42);
            """;
            assertEval(src, 42);
        }

        @Test
        @DisplayName("early return not triggered in loop")
        void testEarlyReturnNotTriggeredLoop() {
            var src = """
                fn findFirst(target: Int): Int {
                    for i in 0..10 {
                        if i == target { return i; }
                    }
                    -(1)
                }
                findFirst(99);
            """;
            assertEval(src, -1);
        }

        @Test
        @DisplayName("multiple early return points")
        void testMultipleEarlyReturns() {
            var src = """
                fn classify(n: Int): String {
                    if n < 0 { return "negative"; }
                    if n == 0 { return "zero"; }
                    "positive"
                }
                classify(-(1));
            """;
            assertEval(src, "negative");
        }

        @Test
        @DisplayName("multiple early returns: second path")
        void testMultipleEarlyReturnsSecond() {
            var src = """
                fn classify(n: Int): String {
                    if n < 0 { return "negative"; }
                    if n == 0 { return "zero"; }
                    "positive"
                }
                classify(0);
            """;
            assertEval(src, "zero");
        }

        @Test
        @DisplayName("multiple early returns: fall through")
        void testMultipleEarlyReturnsFallThrough() {
            var src = """
                fn classify(n: Int): String {
                    if n < 0 { return "negative"; }
                    if n == 0 { return "zero"; }
                    "positive"
                }
                classify(5);
            """;
            assertEval(src, "positive");
        }
    }

    // --- 2.4 Return types ---

    @Nested
    @DisplayName("Return types")
    class ReturnTypes {

        @Test
        @DisplayName("function returning none (optional)")
        void testReturnNone() {
            var src = """
                fn maybe(x: Int): Int? {
                    if x > 0 { x } else { none }
                }
                maybe(-(1));
            """;
            Value result = context.eval("relang", src);
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("function returning value (optional)")
        void testReturnValue() {
            var src = """
                fn maybe(x: Int): Int? {
                    if x > 0 { x } else { none }
                }
                maybe(5);
            """;
            assertEval(src, 5);
        }

        @Test
        @DisplayName("Unit return type")
        void testUnitReturnType() {
            var src = """
                fn doWork(): Unit {
                    let x = 42;
                    unit
                }
                doWork();
            """;
            Value result = context.eval("relang", src);
            assertFalse(result.isNull());
        }

        @Test
        @DisplayName("function returns different types per branch")
        void testConditionalReturnTypes() {
            var src = """
                fn describe(n: Int) {
                    if n > 0 { "positive" } else { "non-positive" }
                }
                describe(5);
            """;
            assertEval(src, "positive");
        }
    }

    // --- Helpers ---

    private void assertEval(String source, long expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asLong());
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

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
 * Tests for recursion and hoisting (spec sections 3.3, 3.4).
 * Direct recursion, mutual recursion, function hoisting.
 */
@DisplayName("Recursion and hoisting")
public class FunctionRecursionTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- 3.3 Direct recursion ---

    @Nested
    @DisplayName("Direct recursion")
    class DirectRecursion {

        @Test
        @DisplayName("factorial")
        void testFactorial() {
            var src = """
                fn factorial(n: Int): Int {
                    if n <= 1 { 1 } else { n * factorial(n - 1) }
                }
                factorial(5);
            """;
            assertEval(src, 120);
        }

        @Test
        @DisplayName("factorial base case")
        void testFactorialBase() {
            var src = """
                fn factorial(n: Int): Int {
                    if n <= 1 { 1 } else { n * factorial(n - 1) }
                }
                factorial(0);
            """;
            assertEval(src, 1);
        }

        @Test
        @DisplayName("factorial of 1")
        void testFactorialOne() {
            var src = """
                fn factorial(n: Int): Int {
                    if n <= 1 { 1 } else { n * factorial(n - 1) }
                }
                factorial(1);
            """;
            assertEval(src, 1);
        }

        @Test
        @DisplayName("fibonacci")
        void testFibonacci() {
            var src = """
                fn fib(n: Int): Int {
                    if n < 2 { n } else { fib(n - 1) + fib(n - 2) }
                }
                fib(10);
            """;
            assertEval(src, 55);
        }

        @Test
        @DisplayName("fibonacci base cases")
        void testFibonacciBase() {
            var src = """
                fn fib(n: Int): Int {
                    if n < 2 { n } else { fib(n - 1) + fib(n - 2) }
                }
                fib(0);
            """;
            assertEval(src, 0);
        }

        @Test
        @DisplayName("fibonacci of 1")
        void testFibonacciOne() {
            var src = """
                fn fib(n: Int): Int {
                    if n < 2 { n } else { fib(n - 1) + fib(n - 2) }
                }
                fib(1);
            """;
            assertEval(src, 1);
        }

        @Test
        @DisplayName("sum via recursion")
        void testSumRecursion() {
            var src = """
                fn sumTo(n: Int): Int {
                    if n <= 0 { 0 } else { n + sumTo(n - 1) }
                }
                sumTo(10);
            """;
            assertEval(src, 55);
        }

        @Test
        @DisplayName("power function recursive")
        void testPowerRecursive() {
            var src = """
                fn power(base: Int, exp: Int): Int {
                    if exp == 0 { 1 } else { base * power(base, exp - 1) }
                }
                power(2, 10);
            """;
            assertEval(src, 1024);
        }

        @Test
        @DisplayName("recursive with early return")
        void testRecursiveEarlyReturn() {
            var src = """
                fn gcd(a: Int, b: Int): Int {
                    if b == 0 { return a; }
                    gcd(b, a % b)
                }
                gcd(48, 18);
            """;
            assertEval(src, 6);
        }
    }

    // --- 3.4 Hoisting ---

    @Nested
    @DisplayName("Hoisting")
    class Hoisting {

        @Test
        @DisplayName("function calls another defined later")
        void testCallDefinedLater() {
            var src = """
                fn double(x: Int): Int = x * 2;
                fn quadruple(x: Int): Int = double(double(x));
                quadruple(3);
            """;
            assertEval(src, 12);
        }

        @Test
        @DisplayName("function order doesn't matter for calls")
        void testOrderDoesntMatter() {
            var src = """
                fn caller(): Int {
                    helper(5)
                }
                fn helper(x: Int): Int {
                    x * 10
                }
                caller();
            """;
            assertEval(src, 50);
        }

        @Test
        @DisplayName("mutual recursion: isEven/isOdd")
        void testMutualRecursion() {
            var src = """
                fn isEven(n: Int): Bool {
                    if n == 0 { true } else { isOdd(n - 1) }
                }
                fn isOdd(n: Int): Bool {
                    if n == 0 { false } else { isEven(n - 1) }
                }
                isEven(10);
            """;
            assertEval(src, true);
        }

        @Test
        @DisplayName("mutual recursion: isOdd")
        void testMutualRecursionOdd() {
            var src = """
                fn isEven(n: Int): Bool {
                    if n == 0 { true } else { isOdd(n - 1) }
                }
                fn isOdd(n: Int): Bool {
                    if n == 0 { false } else { isEven(n - 1) }
                }
                isOdd(7);
            """;
            assertEval(src, true);
        }

        @Test
        @DisplayName("mutual recursion: isEven false")
        void testMutualRecursionEvenFalse() {
            var src = """
                fn isEven(n: Int): Bool {
                    if n == 0 { true } else { isOdd(n - 1) }
                }
                fn isOdd(n: Int): Bool {
                    if n == 0 { false } else { isEven(n - 1) }
                }
                isEven(7);
            """;
            assertEval(src, false);
        }

        @Test
        @DisplayName("chain of three mutually dependent functions")
        void testThreeFunctionChain() {
            var src = """
                fn a(n: Int): Int {
                    if n <= 0 { 0 } else { b(n - 1) + 1 }
                }
                fn b(n: Int): Int {
                    if n <= 0 { 0 } else { c(n - 1) + 1 }
                }
                fn c(n: Int): Int {
                    if n <= 0 { 0 } else { a(n - 1) + 1 }
                }
                a(6);
            """;
            assertEval(src, 6);
        }
    }

    // --- Deep recursion / stress ---

    @Nested
    @DisplayName("Deep recursion")
    class DeepRecursion {

        @Test
        @DisplayName("moderately deep recursion: factorial(12)")
        void testDeepFactorial() {
            var src = """
                fn factorial(n: Int): Int {
                    if n <= 1 { 1 } else { n * factorial(n - 1) }
                }
                factorial(12);
            """;
            assertEval(src, 479001600L);
        }

        @Test
        @DisplayName("recursive countdown")
        void testCountdown() {
            var src = """
                fn countdown(n: Int): Int {
                    if n <= 0 { 0 } else { countdown(n - 1) }
                }
                countdown(500);
            """;
            assertEval(src, 0);
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

package com.relang.primitives;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for Int primitive type (spec section 2.1).
 * Int is a signed 64-bit integer, immutable and serializable.
 */
@DisplayName("Int primitive")
public class IntPrimitiveTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- 2.1 Literals ---

    @Nested
    @DisplayName("Literals")
    class Literals {

        @Test
        @DisplayName("basic integer literal")
        void testBasicLiteral() {
            assertEval("42;", 42);
        }

        @Test
        @DisplayName("zero literal")
        void testZero() {
            assertEval("0;", 0);
        }

        @Test
        @DisplayName("single digit")
        void testSingleDigit() {
            assertEval("7;", 7);
        }

        @Test
        @DisplayName("large integer")
        void testLargeInt() {
            assertEval("9999999999;", 9999999999L);
        }

        @Test
        @DisplayName("underscore separator: 1_000_000")
        void testUnderscoreSeparator() {
            assertEval("1_000_000;", 1_000_000);
        }

        @Test
        @DisplayName("underscore in various positions: 1_0_0")
        void testUnderscoreVariousPositions() {
            assertEval("1_0_0;", 100);
        }

        @Test
        @DisplayName("negation of literal: -(17)")
        void testNegation() {
            assertEval("-(17);", -17);
        }

        @Test
        @DisplayName("negation of zero")
        void testNegationZero() {
            assertEval("-(0);", 0);
        }

        @Test
        @DisplayName("double negation")
        void testDoubleNegation() {
            assertEval("-(-(42));", 42);
        }

        @Test
        @DisplayName("int in let binding")
        void testLetBinding() {
            assertEval("let a = 42; a;", 42);
        }

        @Test
        @DisplayName("negative via let")
        void testLetNegative() {
            assertEval("let b = -(17); b;", -17);
        }

        @Test
        @DisplayName("underscore separator in let")
        void testLetUnderscore() {
            assertEval("let c = 1_000_000; c;", 1_000_000);
        }
    }

    // --- Arithmetic operations ---

    @Nested
    @DisplayName("Arithmetic")
    class Arithmetic {

        @Test
        @DisplayName("addition: 1 + 2 = 3")
        void testAddition() {
            assertEval("1 + 2;", 3);
        }

        @Test
        @DisplayName("subtraction: 10 - 3 = 7")
        void testSubtraction() {
            assertEval("10 - 3;", 7);
        }

        @Test
        @DisplayName("multiplication: 3 * 4 = 12")
        void testMultiplication() {
            assertEval("3 * 4;", 12);
        }

        @Test
        @DisplayName("division: 10 / 2 = 5")
        void testDivision() {
            assertEval("10 / 2;", 5);
        }

        @Test
        @DisplayName("integer division truncates: 7 / 2 = 3")
        void testDivisionTruncates() {
            assertEval("7 / 2;", 3);
        }

        @Test
        @DisplayName("modulo: 7 % 3 = 1")
        void testModulo() {
            assertEval("7 % 3;", 1);
        }

        @Test
        @DisplayName("modulo with no remainder: 10 % 5 = 0")
        void testModuloNoRemainder() {
            assertEval("10 % 5;", 0);
        }

        @Test
        @DisplayName("addition with zero: x + 0 = x")
        void testAddZero() {
            assertEval("42 + 0;", 42);
        }

        @Test
        @DisplayName("multiplication by zero: x * 0 = 0")
        void testMulZero() {
            assertEval("42 * 0;", 0);
        }

        @Test
        @DisplayName("multiplication by one: x * 1 = x")
        void testMulOne() {
            assertEval("42 * 1;", 42);
        }

        @Test
        @DisplayName("subtraction yielding negative: 3 - 10 = -7")
        void testSubNegativeResult() {
            assertEval("3 - 10;", -7);
        }

        @Test
        @DisplayName("chained addition: 1 + 2 + 3 = 6")
        void testChainedAddition() {
            assertEval("1 + 2 + 3;", 6);
        }

        @Test
        @DisplayName("chained operations: 10 - 3 + 2 = 9")
        void testChainedMixed() {
            assertEval("10 - 3 + 2;", 9);
        }
    }

    // --- Operator precedence ---

    @Nested
    @DisplayName("Precedence")
    class Precedence {

        @Test
        @DisplayName("mul before add: 2 + 3 * 4 = 14")
        void testMulBeforeAdd() {
            assertEval("2 + 3 * 4;", 14);
        }

        @Test
        @DisplayName("div before sub: 10 - 6 / 2 = 7")
        void testDivBeforeSub() {
            assertEval("10 - 6 / 2;", 7);
        }

        @Test
        @DisplayName("parentheses override: (2 + 3) * 4 = 20")
        void testParenthesesOverride() {
            assertEval("(2 + 3) * 4;", 20);
        }

        @Test
        @DisplayName("nested parentheses: ((1 + 2) * (3 + 4)) = 21")
        void testNestedParentheses() {
            assertEval("((1 + 2) * (3 + 4));", 21);
        }

        @Test
        @DisplayName("mod same precedence as mul: 10 % 3 * 2")
        void testModPrecedence() {
            // % and * are same precedence, left-to-right
            assertEval("10 % 3 * 2;", 2); // (10 % 3) * 2 = 1 * 2
        }
    }

    // --- Comparisons ---

    @Nested
    @DisplayName("Comparisons")
    class Comparisons {

        @Test
        @DisplayName("less than: 1 < 2 is true")
        void testLessThanTrue() {
            assertEval("1 < 2;", true);
        }

        @Test
        @DisplayName("less than: 2 < 1 is false")
        void testLessThanFalse() {
            assertEval("2 < 1;", false);
        }

        @Test
        @DisplayName("less than: equal values is false")
        void testLessThanEqual() {
            assertEval("5 < 5;", false);
        }

        @Test
        @DisplayName("less or equal: 2 <= 2 is true")
        void testLessOrEqualTrue() {
            assertEval("2 <= 2;", true);
        }

        @Test
        @DisplayName("less or equal: 1 <= 2 is true")
        void testLessOrEqualLess() {
            assertEval("1 <= 2;", true);
        }

        @Test
        @DisplayName("less or equal: 3 <= 2 is false")
        void testLessOrEqualFalse() {
            assertEval("3 <= 2;", false);
        }

        @Test
        @DisplayName("greater than: 3 > 1 is true")
        void testGreaterThanTrue() {
            assertEval("3 > 1;", true);
        }

        @Test
        @DisplayName("greater than: 1 > 3 is false")
        void testGreaterThanFalse() {
            assertEval("1 > 3;", false);
        }

        @Test
        @DisplayName("greater or equal: 2 >= 2 is true")
        void testGreaterOrEqualTrue() {
            assertEval("2 >= 2;", true);
        }

        @Test
        @DisplayName("greater or equal: 2 >= 3 is false")
        void testGreaterOrEqualFalse() {
            assertEval("2 >= 3;", false);
        }

        @Test
        @DisplayName("equals: 5 == 5 is true")
        void testEqualsTrue() {
            assertEval("5 == 5;", true);
        }

        @Test
        @DisplayName("equals: 5 == 3 is false")
        void testEqualsFalse() {
            assertEval("5 == 3;", false);
        }

        @Test
        @DisplayName("not equals: 5 != 3 is true")
        void testNotEqualsTrue() {
            assertEval("5 != 3;", true);
        }

        @Test
        @DisplayName("not equals: 5 != 5 is false")
        void testNotEqualsFalse() {
            assertEval("5 != 5;", false);
        }

        @Test
        @DisplayName("comparison with negative: -1 < 0")
        void testCompareNegative() {
            assertEval("-(1) < 0;", true);
        }

        @Test
        @DisplayName("comparison with zero: 0 == 0")
        void testCompareZeros() {
            assertEval("0 == 0;", true);
        }
    }

    // --- Int in functions ---

    @Nested
    @DisplayName("Functions")
    class Functions {

        @Test
        @DisplayName("function with Int params and return")
        void testIntFunction() {
            assertEval("fn add(a: Int, b: Int): Int { return a + b; } add(10, 20);", 30);
        }

        @Test
        @DisplayName("function with Int arithmetic")
        void testIntArithmeticFunction() {
            var src = """
                fn calc(a: Int, b: Int): Int {
                    let sum = a + b;
                    let product = a * b;
                    sum + product
                }
                calc(3, 4);
            """;
            assertEval(src, 19); // (3+4) + (3*4) = 7 + 12
        }

        @Test
        @DisplayName("recursive function with Int")
        void testRecursiveInt() {
            var src = """
                fn factorial(n: Int): Int {
                    if n < 2 { 1 } else { n * factorial(n - 1) }
                }
                factorial(6);
            """;
            assertEval(src, 720);
        }

        @Test
        @DisplayName("Int as expression body function")
        void testExprBodyFunction() {
            assertEval("fn double(x: Int): Int = x * 2; double(21);", 42);
        }
    }

    // --- Int in control flow ---

    @Nested
    @DisplayName("Control flow")
    class ControlFlow {

        @Test
        @DisplayName("Int in if condition comparison")
        void testIfCondition() {
            assertEval("let x = 10; if x > 5 { 1 } else { 0 };", 1);
        }

        @Test
        @DisplayName("Int in while loop")
        void testWhileLoop() {
            var src = """
                let i = 0;
                let sum = 0;
                while i < 5 {
                    sum = sum + i;
                    i = i + 1;
                }
                sum;
            """;
            assertEval(src, 10); // 0+1+2+3+4
        }

        @Test
        @DisplayName("Int in for range")
        void testForRange() {
            var src = """
                let sum = 0;
                for i in 1..=10 {
                    sum = sum + i;
                }
                sum;
            """;
            assertEval(src, 55);
        }

        @Test
        @DisplayName("Int in match expression")
        void testMatchExpr() {
            var src = """
                let x = 42;
                let r = match x {
                    0 -> "zero",
                    42 -> "answer",
                    _ -> "other"
                };
                r;
            """;
            assertEval(src, "answer");
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

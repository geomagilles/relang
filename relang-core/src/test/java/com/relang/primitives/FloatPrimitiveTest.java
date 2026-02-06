package com.relang.primitives;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Float primitive type (spec section 2.2).
 * Float is an IEEE 754 64-bit double. No implicit coercion Int <-> Float.
 */
@DisplayName("Float primitive")
public class FloatPrimitiveTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- 2.2 Literals ---

    @Nested
    @DisplayName("Literals")
    class Literals {

        @Test
        @DisplayName("basic float literal: 3.14")
        void testBasicLiteral() {
            assertEval("3.14;", 3.14);
        }

        @Test
        @DisplayName("zero float: 0.0")
        void testZero() {
            assertEval("0.0;", 0.0);
        }

        @Test
        @DisplayName("one point zero: 1.0")
        void testOne() {
            assertEval("1.0;", 1.0);
        }

        @Test
        @DisplayName("negation: -(0.5)")
        void testNegation() {
            assertEval("-(0.5);", -0.5);
        }

        @Test
        @DisplayName("scientific notation: 1.0e6")
        void testScientificNotation() {
            assertEval("1.0e6;", 1_000_000.0);
        }

        @Test
        @DisplayName("scientific notation with plus: 1.5e+3")
        void testScientificNotationPlus() {
            assertEval("1.5e+3;", 1500.0);
        }

        @Test
        @DisplayName("scientific notation with minus: 1.0e-2")
        void testScientificNotationMinus() {
            assertEval("1.0e-2;", 0.01);
        }

        @Test
        @DisplayName("underscore separator: 1_000.5")
        void testUnderscoreSeparator() {
            assertEval("1_000.5;", 1000.5);
        }

        @Test
        @DisplayName("underscore in decimal part: 3.141_592")
        void testUnderscoreDecimalPart() {
            assertEval("3.141_592;", 3.141592);
        }

        @Test
        @DisplayName("float in let binding")
        void testLetBinding() {
            assertEval("let x = 3.14; x;", 3.14);
        }

        @Test
        @DisplayName("negative float in let binding")
        void testLetNegative() {
            assertEval("let y = -(0.5); y;", -0.5);
        }

        @Test
        @DisplayName("double negation")
        void testDoubleNegation() {
            assertEval("-(-(3.14));", 3.14);
        }
    }

    // --- Arithmetic ---

    @Nested
    @DisplayName("Arithmetic")
    class Arithmetic {

        @Test
        @DisplayName("addition: 1.0 + 2.5 = 3.5")
        void testAddition() {
            assertEval("1.0 + 2.5;", 3.5);
        }

        @Test
        @DisplayName("subtraction: 10.0 - 3.5 = 6.5")
        void testSubtraction() {
            assertEval("10.0 - 3.5;", 6.5);
        }

        @Test
        @DisplayName("multiplication: 2.0 * 3.0 = 6.0")
        void testMultiplication() {
            assertEval("2.0 * 3.0;", 6.0);
        }

        @Test
        @DisplayName("division: 7.0 / 2.0 = 3.5")
        void testDivision() {
            assertEval("7.0 / 2.0;", 3.5);
        }

        @Test
        @DisplayName("addition with zero: 3.14 + 0.0")
        void testAddZero() {
            assertEval("3.14 + 0.0;", 3.14);
        }

        @Test
        @DisplayName("multiplication by zero: 3.14 * 0.0")
        void testMulZero() {
            assertEval("3.14 * 0.0;", 0.0);
        }

        @Test
        @DisplayName("multiplication by one: 3.14 * 1.0")
        void testMulOne() {
            assertEval("3.14 * 1.0;", 3.14);
        }

        @Test
        @DisplayName("chained float operations")
        void testChainedOperations() {
            assertEval("1.0 + 2.0 + 3.0;", 6.0);
        }

        @Test
        @DisplayName("mixed mul and add with precedence")
        void testPrecedence() {
            assertEval("1.0 + 2.0 * 3.0;", 7.0);
        }

        @Test
        @DisplayName("parentheses override precedence")
        void testParentheses() {
            assertEval("(1.0 + 2.0) * 3.0;", 9.0);
        }
    }

    // --- Comparisons ---

    @Nested
    @DisplayName("Comparisons")
    class Comparisons {

        @Test
        @DisplayName("less than: 1.0 < 2.0")
        void testLessThan() {
            assertEval("1.0 < 2.0;", true);
        }

        @Test
        @DisplayName("less than false: 2.0 < 1.0")
        void testLessThanFalse() {
            assertEval("2.0 < 1.0;", false);
        }

        @Test
        @DisplayName("less or equal: 1.0 <= 1.0")
        void testLessOrEqual() {
            assertEval("1.0 <= 1.0;", true);
        }

        @Test
        @DisplayName("greater than: 2.0 > 1.0")
        void testGreaterThan() {
            assertEval("2.0 > 1.0;", true);
        }

        @Test
        @DisplayName("greater or equal: 1.0 >= 1.0")
        void testGreaterOrEqual() {
            assertEval("1.0 >= 1.0;", true);
        }

        @Test
        @DisplayName("equals: 3.14 == 3.14")
        void testEquals() {
            assertEval("3.14 == 3.14;", true);
        }

        @Test
        @DisplayName("equals false: 3.14 == 2.0")
        void testEqualsFalse() {
            assertEval("3.14 == 2.0;", false);
        }

        @Test
        @DisplayName("not equals: 3.14 != 2.0")
        void testNotEquals() {
            assertEval("3.14 != 2.0;", true);
        }

        @Test
        @DisplayName("not equals false: 3.14 != 3.14")
        void testNotEqualsFalse() {
            assertEval("3.14 != 3.14;", false);
        }

        @Test
        @DisplayName("compare negative floats: -1.0 < 0.0")
        void testCompareNegative() {
            assertEval("-(1.0) < 0.0;", true);
        }
    }

    // --- No implicit coercion Int <-> Float ---

    @Nested
    @DisplayName("No implicit coercion")
    class NoCoercion {

        @Test
        @DisplayName("Int + Float should fail (no implicit coercion)")
        void testIntPlusFloat() {
            assertThrows(PolyglotException.class, () -> context.eval("relang", "1 + 2.0;"));
        }

        @Test
        @DisplayName("Float + Int should fail (no implicit coercion)")
        void testFloatPlusInt() {
            assertThrows(PolyglotException.class, () -> context.eval("relang", "1.0 + 2;"));
        }

        @Test
        @DisplayName("Int * Float should fail (no implicit coercion)")
        void testIntMulFloat() {
            assertThrows(PolyglotException.class, () -> context.eval("relang", "2 * 3.0;"));
        }

        @Test
        @DisplayName("Int < Float should fail (no implicit coercion)")
        void testIntLtFloat() {
            assertThrows(PolyglotException.class, () -> context.eval("relang", "1 < 2.0;"));
        }
    }

    // --- Float in functions ---

    @Nested
    @DisplayName("Functions")
    class Functions {

        @Test
        @DisplayName("function with Float params")
        void testFloatFunction() {
            assertEval("fn add(a: Float, b: Float): Float { return a + b; } add(1.5, 2.5);", 4.0);
        }

        @Test
        @DisplayName("expression body function with Float")
        void testExprBodyFunction() {
            assertEval("fn half(x: Float): Float = x / 2.0; half(10.0);", 5.0);
        }
    }

    // --- Helpers ---

    private void assertEval(String source, double expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asDouble(), 0.0001);
    }

    private void assertEval(String source, boolean expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asBoolean());
    }
}

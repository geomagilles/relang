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
 * Tests for Bool primitive type (spec section 3.1).
 * Bool has two values: true and false.
 */
@DisplayName("Bool primitive")
public class BoolPrimitiveTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- 3.1 Literals ---

    @Nested
    @DisplayName("Literals")
    class Literals {

        @Test
        @DisplayName("true literal")
        void testTrue() {
            assertEval("true;", true);
        }

        @Test
        @DisplayName("false literal")
        void testFalse() {
            assertEval("false;", false);
        }

        @Test
        @DisplayName("true in let binding")
        void testLetTrue() {
            assertEval("let ok = true; ok;", true);
        }

        @Test
        @DisplayName("false in let binding")
        void testLetFalse() {
            assertEval("let ko = false; ko;", false);
        }
    }

    // --- Logical operators ---

    @Nested
    @DisplayName("Logical AND")
    class LogicalAnd {

        @Test
        @DisplayName("true and true = true")
        void testTrueAndTrue() {
            assertEval("true and true;", true);
        }

        @Test
        @DisplayName("true and false = false")
        void testTrueAndFalse() {
            assertEval("true and false;", false);
        }

        @Test
        @DisplayName("false and true = false")
        void testFalseAndTrue() {
            assertEval("false and true;", false);
        }

        @Test
        @DisplayName("false and false = false")
        void testFalseAndFalse() {
            assertEval("false and false;", false);
        }
    }

    @Nested
    @DisplayName("Logical OR")
    class LogicalOr {

        @Test
        @DisplayName("true or true = true")
        void testTrueOrTrue() {
            assertEval("true or true;", true);
        }

        @Test
        @DisplayName("true or false = true")
        void testTrueOrFalse() {
            assertEval("true or false;", true);
        }

        @Test
        @DisplayName("false or true = true")
        void testFalseOrTrue() {
            assertEval("false or true;", true);
        }

        @Test
        @DisplayName("false or false = false")
        void testFalseOrFalse() {
            assertEval("false or false;", false);
        }
    }

    @Nested
    @DisplayName("Logical NOT")
    class LogicalNot {

        @Test
        @DisplayName("not true = false")
        void testNotTrue() {
            assertEval("not true;", false);
        }

        @Test
        @DisplayName("not false = true")
        void testNotFalse() {
            assertEval("not false;", true);
        }

        @Test
        @DisplayName("double negation: not not true = true")
        void testDoubleNot() {
            assertEval("not not true;", true);
        }

        @Test
        @DisplayName("triple negation: not not not true = false")
        void testTripleNot() {
            assertEval("not not not true;", false);
        }
    }

    // --- Complex logical expressions ---

    @Nested
    @DisplayName("Complex expressions")
    class ComplexExpressions {

        @Test
        @DisplayName("not (true and false) = true")
        void testNotAnd() {
            assertEval("not (true and false);", true);
        }

        @Test
        @DisplayName("not (false or false) = true")
        void testNotOr() {
            assertEval("not (false or false);", true);
        }

        @Test
        @DisplayName("(true or false) and (true or false) = true")
        void testOrAndOr() {
            assertEval("(true or false) and (true or false);", true);
        }

        @Test
        @DisplayName("true and true or false = true (and binds tighter)")
        void testPrecedenceAndOr() {
            // 'and' has higher precedence than 'or'
            assertEval("true and true or false;", true);
        }

        @Test
        @DisplayName("false or true and true = true (and binds tighter)")
        void testPrecedenceOrAnd() {
            assertEval("false or true and true;", true);
        }

        @Test
        @DisplayName("false or false and true = false")
        void testPrecedenceOrAndFalse() {
            // false or (false and true) = false or false = false
            assertEval("false or false and true;", false);
        }
    }

    // --- Bool equality ---

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("true == true")
        void testTrueEqualsTrue() {
            assertEval("true == true;", true);
        }

        @Test
        @DisplayName("false == false")
        void testFalseEqualsFalse() {
            assertEval("false == false;", true);
        }

        @Test
        @DisplayName("true == false is false")
        void testTrueEqualsFalse() {
            assertEval("true == false;", false);
        }

        @Test
        @DisplayName("true != false is true")
        void testTrueNotEqualsFalse() {
            assertEval("true != false;", true);
        }

        @Test
        @DisplayName("true != true is false")
        void testTrueNotEqualsTrue() {
            assertEval("true != true;", false);
        }
    }

    // --- Bool from comparisons ---

    @Nested
    @DisplayName("From comparisons")
    class FromComparisons {

        @Test
        @DisplayName("comparison result is bool: (1 < 2) and (3 > 1)")
        void testComparisonBoolAnd() {
            assertEval("(1 < 2) and (3 > 1);", true);
        }

        @Test
        @DisplayName("comparison result in let: let b = 1 < 2")
        void testComparisonInLet() {
            assertEval("let b = 1 < 2; b;", true);
        }

        @Test
        @DisplayName("comparison result in not: not (1 == 2)")
        void testNotComparison() {
            assertEval("not (1 == 2);", true);
        }
    }

    // --- Bool in control flow ---

    @Nested
    @DisplayName("Control flow")
    class ControlFlow {

        @Test
        @DisplayName("bool in if condition")
        void testIfCondition() {
            assertEval("let b = true; if b { 1 } else { 0 };", 1);
        }

        @Test
        @DisplayName("false in if condition")
        void testIfConditionFalse() {
            assertEval("let b = false; if b { 1 } else { 0 };", 0);
        }

        @Test
        @DisplayName("bool expression in while")
        void testWhileCondition() {
            var src = """
                let flag = true;
                let count = 0;
                while flag {
                    count = count + 1;
                    if count == 3 { flag = false; }
                }
                count;
            """;
            assertEval(src, 3);
        }

        @Test
        @DisplayName("bool in match")
        void testMatchBool() {
            var src = """
                let b = true;
                let r = match b {
                    true -> "yes",
                    false -> "no",
                    _ -> "unknown"
                };
                r;
            """;
            assertEval(src, "yes");
        }
    }

    // --- Bool in functions ---

    @Nested
    @DisplayName("Functions")
    class Functions {

        @Test
        @DisplayName("function returning Bool")
        void testBoolReturn() {
            assertEval("fn isPositive(n: Int): Bool { n > 0 } isPositive(5);", true);
        }

        @Test
        @DisplayName("function with Bool param")
        void testBoolParam() {
            var src = """
                fn toggle(b: Bool): Bool { not b }
                toggle(true);
            """;
            assertEval(src, false);
        }

        @Test
        @DisplayName("expression body returning Bool")
        void testExprBody() {
            assertEval("fn negate(b: Bool): Bool = not b; negate(false);", true);
        }
    }

    // --- Helpers ---

    private void assertEval(String source, boolean expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asBoolean());
    }

    private void assertEval(String source, long expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asLong());
    }

    private void assertEval(String source, String expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asString());
    }
}

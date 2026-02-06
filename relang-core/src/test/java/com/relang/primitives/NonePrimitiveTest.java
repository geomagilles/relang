package com.relang.primitives;

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
 * Tests for none and optionals (spec section 6.2).
 * none models absence of value. Optionals use T? + none.
 * Some/None are not exposed in surface language v0.1.
 */
@DisplayName("None and optionals")
public class NonePrimitiveTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- 6.2 none literal ---

    @Nested
    @DisplayName("Literals")
    class Literals {

        @Test
        @DisplayName("none literal is null")
        void testNoneLiteral() {
            Value result = context.eval("relang", "none;");
            assertTrue(result.isNull(), "none should be null");
        }

        @Test
        @DisplayName("none in let binding")
        void testNoneInLet() {
            Value result = context.eval("relang", "let x = none; x;");
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("none assigned to variable")
        void testNoneAssigned() {
            Value result = context.eval("relang", "x = none; x;");
            assertTrue(result.isNull());
        }
    }

    // --- none in match ---

    @Nested
    @DisplayName("Match on none")
    class MatchOnNone {

        @Test
        @DisplayName("match none pattern")
        void testMatchNonePattern() {
            var src = """
                let x = none;
                let r = match x {
                    none -> "absent",
                    _ -> "present"
                };
                r;
            """;
            assertEval(src, "absent");
        }

        @Test
        @DisplayName("match non-none value falls to wildcard")
        void testMatchNonNone() {
            var src = """
                let x = 42;
                let r = match x {
                    none -> "absent",
                    _ -> "present"
                };
                r;
            """;
            assertEval(src, "present");
        }

        @Test
        @DisplayName("match distinguishes none from values")
        void testMatchDistinguish() {
            var src = """
                fn describe(x) {
                    match x {
                        none -> "none",
                        0 -> "zero",
                        _ -> "something"
                    }
                }
                let a = describe(none);
                let b = describe(0);
                let c = describe(42);
                a + "," + b + "," + c;
            """;
            assertEval(src, "none,zero,something");
        }

        @Test
        @DisplayName("match on none with block body")
        void testMatchNoneBlockBody() {
            var src = """
                let x = none;
                let r = match x {
                    none -> { let msg = "was none"; msg },
                    _ -> "had value"
                };
                r;
            """;
            assertEval(src, "was none");
        }
    }

    // --- none in conditional logic ---

    @Nested
    @DisplayName("Conditional logic")
    class ConditionalLogic {

        @Test
        @DisplayName("function that may return none")
        void testFunctionReturnsNone() {
            var src = """
                fn findOrNone(x: Int) {
                    if x > 0 { x } else { none }
                }
                let r = findOrNone(-(1));
                match r {
                    none -> "not found",
                    _ -> "found"
                };
            """;
            assertEval(src, "not found");
        }

        @Test
        @DisplayName("function returns value when found")
        void testFunctionReturnsValue() {
            var src = """
                fn findOrNone(x: Int) {
                    if x > 0 { x } else { none }
                }
                findOrNone(5);
            """;
            Value result = context.eval("relang", src);
            assertEquals(5, result.asLong());
        }
    }

    // --- none propagation ---

    @Nested
    @DisplayName("Propagation")
    class Propagation {

        @Test
        @DisplayName("none passed through variable reassignment")
        void testNoneReassign() {
            var src = """
                let x = 42;
                x = none;
                x;
            """;
            Value result = context.eval("relang", src);
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("none passed as function argument")
        void testNoneAsArg() {
            var src = """
                fn isNone(x) {
                    match x {
                        none -> true,
                        _ -> false
                    }
                }
                isNone(none);
            """;
            assertEval(src, true);
        }

        @Test
        @DisplayName("non-none passed as function argument")
        void testNonNoneAsArg() {
            var src = """
                fn isNone(x) {
                    match x {
                        none -> true,
                        _ -> false
                    }
                }
                isNone(42);
            """;
            assertEval(src, false);
        }
    }

    // --- Helpers ---

    private void assertEval(String source, String expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asString());
    }

    private void assertEval(String source, boolean expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asBoolean());
    }
}

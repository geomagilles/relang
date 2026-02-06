package com.relang.functions;

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
 * Tests for immutable function parameters (GitHub Issue #2).
 * Spec: relang-functions.md section 2.3
 */
@DisplayName("Immutable function parameters")
public class ImmutableParameterTest {

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
    @DisplayName("Parameter reassignment is rejected")
    class ReassignmentRejected {

        @Test
        @DisplayName("direct reassignment of Int parameter")
        void testReassignIntParam() {
            var src = """
                fn process(count: Int): Int {
                    count = count + 1;
                    count * 2;
                }
                process(5);
            """;
            assertTypeError(src, "Cannot reassign parameter");
        }

        @Test
        @DisplayName("direct reassignment of String parameter")
        void testReassignStringParam() {
            var src = """
                fn greet(name: String): String {
                    name = "Mr. " + name;
                    return name;
                }
                greet("Alice");
            """;
            assertTypeError(src, "Cannot reassign parameter");
        }

        @Test
        @DisplayName("direct reassignment of Bool parameter")
        void testReassignBoolParam() {
            var src = """
                fn toggle(flag: Bool): Bool {
                    flag = not flag;
                    return flag;
                }
                toggle(true);
            """;
            assertTypeError(src, "Cannot reassign parameter");
        }

        @Test
        @DisplayName("reassignment of one of multiple parameters")
        void testReassignOneOfMultiple() {
            var src = """
                fn add(a: Int, b: Int): Int {
                    a = a + b;
                    return a;
                }
                add(1, 2);
            """;
            assertTypeError(src, "Cannot reassign parameter");
        }
    }

    @Nested
    @DisplayName("Shadowing parameters with let is allowed")
    class ShadowingAllowed {

        @Test
        @DisplayName("shadow parameter with let")
        void testShadowParameter() {
            var src = """
                fn process(count: Int): Int {
                    let count = count + 1;
                    count * 2;
                }
                process(5);
            """;
            assertEval(src, 12);
        }

        @Test
        @DisplayName("shadow String parameter with let")
        void testShadowStringParam() {
            var src = """
                fn greet(name: String): String {
                    let name = "Mr. " + name;
                    name;
                }
                greet("Alice");
            """;
            assertEval(src, "Mr. Alice");
        }

        @Test
        @DisplayName("shadow in inner scope")
        void testShadowInnerScope() {
            var src = """
                fn process(x: Int): Int {
                    if x > 0 {
                        let x = x * 2;
                        x;
                    } else {
                        x;
                    }
                }
                process(5);
            """;
            assertEval(src, 10);
        }
    }

    @Nested
    @DisplayName("Reading parameters is still allowed")
    class ReadingAllowed {

        @Test
        @DisplayName("read parameter in expression")
        void testReadParameter() {
            assertEval("fn double(x: Int): Int = x * 2; double(5);", 10);
        }

        @Test
        @DisplayName("read parameter in condition")
        void testReadParameterInCondition() {
            var src = """
                fn abs(x: Int): Int {
                    if x < 0 { -x } else { x }
                }
                abs(-5);
            """;
            assertEval(src, 5);
        }

        @Test
        @DisplayName("pass parameter to another function")
        void testPassParameter() {
            var src = """
                fn double(x: Int): Int = x * 2;
                fn quadruple(x: Int): Int = double(double(x));
                quadruple(3);
            """;
            assertEval(src, 12);
        }
    }

    @Nested
    @DisplayName("Local variables in functions are still reassignable")
    class LocalVarsReassignable {

        @Test
        @DisplayName("local variable can be reassigned")
        void testLocalVarReassign() {
            var src = """
                fn accumulate(n: Int): Int {
                    let sum = 0;
                    for i in 0..n {
                        sum = sum + i;
                    }
                    sum;
                }
                accumulate(5);
            """;
            assertEval(src, 10);
        }
    }

    // --- Helpers ---

    private void assertEval(String source, long expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asLong());
    }

    private void assertEval(String source, String expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asString());
    }

    private void assertTypeError(String source, String expectedSubstring) {
        var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", source));
        assertTrue(ex.getMessage().contains("TypeError"),
                "Expected TypeError in message but got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(expectedSubstring),
                "Expected '" + expectedSubstring + "' in message but got: " + ex.getMessage());
    }
}

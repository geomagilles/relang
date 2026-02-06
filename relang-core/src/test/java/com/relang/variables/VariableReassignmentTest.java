package com.relang.variables;

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
 * Tests for variable reassignment (spec section 3).
 * Reassignment is allowed; the type should remain stable.
 */
@DisplayName("Variable reassignment")
public class VariableReassignmentTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- 3. Reassignment ---

    @Nested
    @DisplayName("Basic reassignment")
    class BasicReassignment {

        @Test
        @DisplayName("reassign Int variable")
        void testReassignInt() {
            var src = """
                let count = 0;
                count = count + 1;
                count;
            """;
            assertEval(src, 1);
        }

        @Test
        @DisplayName("multiple reassignments")
        void testMultipleReassignments() {
            var src = """
                let x = 1;
                x = 2;
                x = 3;
                x;
            """;
            assertEval(src, 3);
        }

        @Test
        @DisplayName("reassign with expression")
        void testReassignWithExpression() {
            var src = """
                let x = 10;
                x = x * 2 + 5;
                x;
            """;
            assertEval(src, 25);
        }

        @Test
        @DisplayName("reassign String variable")
        void testReassignString() {
            var src = """
                let name = "Alice";
                name = "Bob";
                name;
            """;
            assertEval(src, "Bob");
        }

        @Test
        @DisplayName("reassign Bool variable")
        void testReassignBool() {
            var src = """
                let flag = true;
                flag = false;
                flag;
            """;
            assertEval(src, false);
        }

        @Test
        @DisplayName("reassign to none")
        void testReassignToNone() {
            var src = """
                let x = 42;
                x = none;
                x;
            """;
            Value result = context.eval("relang", src);
            assertTrue(result.isNull());
        }
    }

    // --- Reassignment in loops ---

    @Nested
    @DisplayName("In loops")
    class InLoops {

        @Test
        @DisplayName("counter in while loop")
        void testCounterWhile() {
            var src = """
                let i = 0;
                while i < 10 {
                    i = i + 1;
                }
                i;
            """;
            assertEval(src, 10);
        }

        @Test
        @DisplayName("accumulator in for loop")
        void testAccumulatorFor() {
            var src = """
                let sum = 0;
                for i in 1..=5 {
                    sum = sum + i;
                }
                sum;
            """;
            assertEval(src, 15);
        }

        @Test
        @DisplayName("swap values")
        void testSwapValues() {
            var src = """
                let a = 1;
                let b = 2;
                let tmp = a;
                a = b;
                b = tmp;
                a * 10 + b;
            """;
            assertEval(src, 21); // a=2, b=1 -> 2*10 + 1
        }
    }

    // --- Reassignment in functions ---

    @Nested
    @DisplayName("In functions")
    class InFunctions {

        @Test
        @DisplayName("local variable reassignment in function")
        void testLocalReassignment() {
            var src = """
                fn compute(n: Int): Int {
                    let result = 0;
                    result = n * n;
                    result = result + n;
                    result
                }
                compute(5);
            """;
            assertEval(src, 30); // 5*5 + 5
        }

        @Test
        @DisplayName("function parameter is separate from caller variable")
        void testParamSeparation() {
            var src = """
                fn inc(x: Int): Int {
                    return x + 1;
                }
                let x = 10;
                let y = inc(x);
                x;
            """;
            assertEval(src, 10); // x unchanged
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

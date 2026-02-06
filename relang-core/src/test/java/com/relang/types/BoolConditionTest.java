package com.relang.types;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that if/while conditions only accept Bool values (issue #3).
 * Integer truthiness (if (1), if (0)) is rejected.
 */
@DisplayName("Bool-only conditions")
public class BoolConditionTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- if rejects non-Bool ---

    private void assertEval(String source, long expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asLong());
    }

    // --- if accepts Bool ---

    @Nested
    @DisplayName("if rejects non-Bool conditions")
    class IfRejectsNonBool {

        @Test
        @DisplayName("if (1) should fail")
        void testIfIntegerOne() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "if (1) { 10; }"));
            assertTrue(ex.getMessage().contains("Condition must be Bool"));
        }

        @Test
        @DisplayName("if (0) should fail")
        void testIfIntegerZero() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "if (0) { 10; }"));
            assertTrue(ex.getMessage().contains("Condition must be Bool"));
        }

        @Test
        @DisplayName("if (42) should fail")
        void testIfIntegerArbitrary() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "if (42) { 10; }"));
            assertTrue(ex.getMessage().contains("Condition must be Bool"));
        }

        @Test
        @DisplayName("if with String should fail")
        void testIfString() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "if (\"hello\") { 10; }"));
            assertTrue(ex.getMessage().contains("Condition must be Bool"));
        }

        @Test
        @DisplayName("if (none) should fail")
        void testIfNone() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "if (none) { 10; }"));
            assertTrue(ex.getMessage().contains("Condition must be Bool"));
        }

        @Test
        @DisplayName("if with Float should fail")
        void testIfFloat() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "if (1.0) { 10; }"));
            assertTrue(ex.getMessage().contains("Condition must be Bool"));
        }
    }

    // --- while rejects non-Bool ---

    @Nested
    @DisplayName("if accepts Bool conditions")
    class IfAcceptsBool {

        @Test
        @DisplayName("if (true) works")
        void testIfTrue() {
            assertEval("if (true) { 10 } else { 20 };", 10);
        }

        @Test
        @DisplayName("if (false) works")
        void testIfFalse() {
            assertEval("if (false) { 10 } else { 20 };", 20);
        }

        @Test
        @DisplayName("if with comparison expression works")
        void testIfComparison() {
            assertEval("if (1 < 2) { 10 } else { 20 };", 10);
        }

        @Test
        @DisplayName("if with logical expression works")
        void testIfLogical() {
            assertEval("if (true and true) { 10 } else { 20 };", 10);
        }

        @Test
        @DisplayName("if with Bool variable works")
        void testIfBoolVar() {
            assertEval("let b = true; if b { 10 } else { 20 };", 10);
        }

        @Test
        @DisplayName("if with not works")
        void testIfNot() {
            assertEval("if not false { 10 } else { 20 };", 10);
        }
    }

    // --- while accepts Bool ---

    @Nested
    @DisplayName("while rejects non-Bool conditions")
    class WhileRejectsNonBool {

        @Test
        @DisplayName("while (1) should fail")
        void testWhileIntegerOne() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "while (1) { break; }"));
            assertTrue(ex.getMessage().contains("Condition must be Bool"));
        }

        @Test
        @DisplayName("while (0) should fail")
        void testWhileIntegerZero() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "while (0) { break; }"));
            assertTrue(ex.getMessage().contains("Condition must be Bool"));
        }

        @Test
        @DisplayName("while with String should fail")
        void testWhileString() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "while (\"yes\") { break; }"));
            assertTrue(ex.getMessage().contains("Condition must be Bool"));
        }
    }

    // --- if without parens rejects non-Bool ---

    @Nested
    @DisplayName("while accepts Bool conditions")
    class WhileAcceptsBool {

        @Test
        @DisplayName("while (true) with break works")
        void testWhileTrue() {
            assertEval("let i = 0; while true { i = i + 1; if i == 3 { break; } } i;", 3);
        }

        @Test
        @DisplayName("while (false) never executes")
        void testWhileFalse() {
            assertEval("let i = 0; while false { i = 99; } i;", 0);
        }

        @Test
        @DisplayName("while with comparison works")
        void testWhileComparison() {
            assertEval("let i = 0; while (i < 5) { i = i + 1; } i;", 5);
        }

        @Test
        @DisplayName("while with Bool variable works")
        void testWhileBoolVar() {
            var src = """
                        let flag = true
                        let count = 0
                        while flag {
                            count = count + 1
                            if count == 3 { flag = false }
                        }
                        count
                    """;
            assertEval(src, 3);
        }
    }

    // --- Helpers ---

    @Nested
    @DisplayName("if without parens rejects non-Bool")
    class IfNoParensRejectsNonBool {

        @Test
        @DisplayName("if 1 { } should fail")
        void testIfNoParensInt() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "if 1 { 10; }"));
            assertTrue(ex.getMessage().contains("Condition must be Bool"));
        }
    }
}

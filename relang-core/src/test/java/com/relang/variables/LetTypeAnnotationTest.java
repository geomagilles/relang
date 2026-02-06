package com.relang.variables;

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
 * Tests for type annotations on let declarations (GitHub Issue #1).
 * Spec: relang-variables-proposal.md section 2.1
 */
@DisplayName("Let type annotations")
public class LetTypeAnnotationTest {

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
    @DisplayName("Valid annotated let declarations")
    class ValidAnnotated {

        @Test
        @DisplayName("let with Int annotation")
        void testLetIntAnnotation() {
            assertEval("let x: Int = 42; x;", 42);
        }

        @Test
        @DisplayName("let with Float annotation")
        void testLetFloatAnnotation() {
            assertEval("let x: Float = 3.14; x;", 3.14);
        }

        @Test
        @DisplayName("let with Bool annotation")
        void testLetBoolAnnotation() {
            assertEval("let x: Bool = true; x;", true);
        }

        @Test
        @DisplayName("let with String annotation")
        void testLetStringAnnotation() {
            assertEval("let x: String = \"hello\"; x;", "hello");
        }

        @Test
        @DisplayName("let with optional type and none")
        void testLetOptionalNone() {
            Value result = context.eval("relang", "let email: String? = none; email;");
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("let with optional type and value")
        void testLetOptionalValue() {
            assertEval("let email: String? = \"alice@test.com\"; email;", "alice@test.com");
        }

        @Test
        @DisplayName("let with Int-to-Float widening")
        void testLetIntToFloatWidening() {
            assertEval("let x: Float = 42; x;", 42.0);
        }

        @Test
        @DisplayName("let without annotation still works")
        void testLetWithoutAnnotation() {
            assertEval("let x = 10; x;", 10);
        }
    }

    @Nested
    @DisplayName("Invalid annotated let declarations")
    class InvalidAnnotated {

        @Test
        @DisplayName("String value assigned to Int variable")
        void testStringToInt() {
            assertTypeError("let x: Int = \"hello\"; x;", "Cannot assign");
        }

        @Test
        @DisplayName("Int value assigned to Bool variable")
        void testIntToBool() {
            assertTypeError("let x: Bool = 42; x;", "Cannot assign");
        }

        @Test
        @DisplayName("Bool value assigned to String variable")
        void testBoolToString() {
            assertTypeError("let x: String = true; x;", "Cannot assign");
        }

        @Test
        @DisplayName("Float value assigned to Int variable (no implicit narrowing)")
        void testFloatToInt() {
            assertTypeError("let x: Int = 3.14; x;", "Cannot assign");
        }
    }

    @Nested
    @DisplayName("Annotated let with reassignment")
    class AnnotatedReassignment {

        @Test
        @DisplayName("optional type allows reassignment to none then back to value")
        void testOptionalReassignment() {
            var src = """
                let email: String? = "alice@test.com";
                email = none;
                email;
            """;
            Value result = context.eval("relang", src);
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("optional type allows reassignment from none to value")
        void testOptionalReassignFromNone() {
            var src = """
                let email: String? = none;
                email = "bob@test.com";
                email;
            """;
            assertEval(src, "bob@test.com");
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

    private void assertTypeError(String source, String expectedSubstring) {
        var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", source));
        assertTrue(ex.getMessage().contains("TypeError"),
                "Expected TypeError in message but got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(expectedSubstring),
                "Expected '" + expectedSubstring + "' in message but got: " + ex.getMessage());
    }
}

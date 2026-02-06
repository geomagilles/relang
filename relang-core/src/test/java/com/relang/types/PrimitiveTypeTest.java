package com.relang.types;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for primitive type system behavior (spec sections 1-3).
 * Verifies type distinctions, no implicit coercion, and type safety.
 */
@DisplayName("Primitive type system")
public class PrimitiveTypeTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- Type distinctions ---

    @Nested
    @DisplayName("Type distinctions")
    class TypeDistinctions {

        @Test
        @DisplayName("Int and Float are distinct types")
        void testIntFloatDistinct() {
            // 42 is Int (long), 42.0 is Float (double)
            Value intVal = context.eval("relang", "42;");
            Value floatVal = context.eval("relang", "42.0;");
            assertTrue(intVal.fitsInLong());
            assertTrue(floatVal.fitsInDouble());
        }

        @Test
        @DisplayName("Bool is distinct from Int")
        void testBoolDistinct() {
            Value boolVal = context.eval("relang", "true;");
            assertTrue(boolVal.isBoolean());
        }

        @Test
        @DisplayName("String is distinct from other types")
        void testStringDistinct() {
            Value strVal = context.eval("relang", "\"42\";");
            assertTrue(strVal.isString());
            assertEquals("42", strVal.asString());
        }

        @Test
        @DisplayName("unit is not null")
        void testUnitNotNull() {
            Value unitVal = context.eval("relang", "unit;");
            assertFalse(unitVal.isNull());
        }

        @Test
        @DisplayName("none is null")
        void testNoneIsNull() {
            Value noneVal = context.eval("relang", "none;");
            assertTrue(noneVal.isNull());
        }
    }

    // --- No implicit coercion ---

    @Nested
    @DisplayName("No implicit coercion")
    class NoImplicitCoercion {

        @Test
        @DisplayName("Int + Float fails (no implicit coercion)")
        void testIntPlusFloat() {
            assertThrows(PolyglotException.class, () -> context.eval("relang", "1 + 2.0;"));
        }

        @Test
        @DisplayName("Float - Int fails (no implicit coercion)")
        void testFloatMinusInt() {
            assertThrows(PolyglotException.class, () -> context.eval("relang", "1.0 - 2;"));
        }

        @Test
        @DisplayName("Int * Float fails (no implicit coercion)")
        void testIntTimesFloat() {
            assertThrows(PolyglotException.class, () -> context.eval("relang", "2 * 3.0;"));
        }

        @Test
        @DisplayName("Int / Float fails (no implicit coercion)")
        void testIntDivFloat() {
            assertThrows(PolyglotException.class, () -> context.eval("relang", "10 / 2.0;"));
        }

        @Test
        @DisplayName("Int < Float fails (no implicit coercion)")
        void testIntLtFloat() {
            assertThrows(PolyglotException.class, () -> context.eval("relang", "1 < 2.0;"));
        }

        @Test
        @DisplayName("Int == Float fails (no implicit coercion)")
        void testIntEqFloat() {
            assertThrows(PolyglotException.class, () -> context.eval("relang", "1 == 1.0;"));
        }
    }

    // --- Each primitive type operations ---

    @Nested
    @DisplayName("Type-specific operations")
    class TypeSpecificOps {

        @Test
        @DisplayName("Int supports modulo")
        void testIntModulo() {
            assertEval("10 % 3;", 1);
        }

        @Test
        @DisplayName("String supports concatenation with +")
        void testStringConcat() {
            assertEval("\"a\" + \"b\";", "a" + "b");
        }

        @Test
        @DisplayName("Bool supports logical operators")
        void testBoolLogical() {
            assertEval("true and false or true;", true);
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

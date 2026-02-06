package com.relang.primitives;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Unit primitive type (spec section 6.1).
 * Unit represents "no meaningful business value". It is NOT null/none.
 */
@DisplayName("Unit primitive")
public class UnitPrimitiveTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- 6.1 Unit literal ---

    @Nested
    @DisplayName("Literals")
    class Literals {

        @Test
        @DisplayName("unit literal evaluates to non-null")
        void testUnitLiteral() {
            Value result = context.eval("relang", "unit;");
            assertFalse(result.isNull(), "unit should not be null");
        }

        @Test
        @DisplayName("unit has display string 'unit'")
        void testUnitDisplayString() {
            Value result = context.eval("relang", "unit;");
            assertEquals("unit", result.toString());
        }

        @Test
        @DisplayName("unit in let binding")
        void testUnitInLet() {
            Value result = context.eval("relang", "let x = unit; x;");
            assertFalse(result.isNull());
        }
    }

    // --- Unit vs None ---

    @Nested
    @DisplayName("Unit vs None distinction")
    class UnitVsNone {

        @Test
        @DisplayName("unit is not null, none is null")
        void testUnitIsNotNull() {
            Value unitVal = context.eval("relang", "unit;");
            Value noneVal = context.eval("relang", "none;");
            assertFalse(unitVal.isNull(), "unit should not be null");
            assertTrue(noneVal.isNull(), "none should be null");
        }

        @Test
        @DisplayName("unit and none are different values")
        void testUnitNotNone() {
            Value unitVal = context.eval("relang", "unit;");
            Value noneVal = context.eval("relang", "none;");
            // They should be distinguishable via isNull
            assertFalse(unitVal.isNull());
            assertTrue(noneVal.isNull());
        }
    }

    // --- Unit in functions ---

    @Nested
    @DisplayName("Functions")
    class Functions {

        @Test
        @DisplayName("function explicitly returning unit")
        void testFunctionReturnsUnit() {
            var src = """
                fn doNothing(): Unit {
                    unit
                }
                doNothing();
            """;
            Value result = context.eval("relang", src);
            assertFalse(result.isNull());
        }

        @Test
        @DisplayName("unit as function argument")
        void testUnitAsArgument() {
            var src = """
                fn accept(x: Unit): Int {
                    42
                }
                accept(unit);
            """;
            Value result = context.eval("relang", src);
            assertEquals(42, result.asLong());
        }
    }
}

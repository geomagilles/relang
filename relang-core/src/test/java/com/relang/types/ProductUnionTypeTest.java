package com.relang.types;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product types (&) and union types (|) - Issue #7")
public class ProductUnionTypeTest {

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
    @DisplayName("Product types")
    class ProductTypes {
        @Test @DisplayName("simple product value")
        void testSimpleProduct() {
            Value result = context.eval("relang", "(42 & \"ok\");");
            assertNotNull(result);
            assertTrue(result.toString().contains("42"));
            assertTrue(result.toString().contains("ok"));
        }

        @Test @DisplayName("product with type annotation")
        void testProductAnnotation() {
            var src = "let p: Int & String = (42 & \"ok\"); p;";
            Value result = context.eval("relang", src);
            assertNotNull(result);
        }

        @Test @DisplayName("product with three components")
        void testTripleProduct() {
            Value result = context.eval("relang", "(1 & \"two\" & true);");
            assertNotNull(result);
        }

        @Test @DisplayName("product equality")
        void testProductEquality() {
            var src = "(42 & \"ok\") == (42 & \"ok\");";
            Value result = context.eval("relang", src);
            assertTrue(result.asBoolean());
        }

        @Test @DisplayName("product inequality")
        void testProductInequality() {
            var src = "(42 & \"ok\") == (42 & \"nope\");";
            Value result = context.eval("relang", src);
            assertFalse(result.asBoolean());
        }

        @Test @DisplayName("product in function parameter")
        void testProductParam() {
            var src = """
                fn getPair(p: Int & String): Int & String = p;
                getPair(1 & "hello");
            """;
            Value result = context.eval("relang", src);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Union types")
    class UnionTypes {
        @Test @DisplayName("Int assigned to Int | String")
        void testIntToUnion() {
            var src = "let v: Int | String = 42; v;";
            Value result = context.eval("relang", src);
            assertEquals(42, result.asLong());
        }

        @Test @DisplayName("String assigned to Int | String")
        void testStringToUnion() {
            var src = """
                let v: Int | String = "hello";
                v;
            """;
            Value result = context.eval("relang", src);
            assertEquals("hello", result.asString());
        }

        @Test @DisplayName("union as function return type")
        void testUnionReturn() {
            var src = """
                fn toStringOrInt(flag: Bool): Int | String {
                    if flag { 42 } else { "hello" }
                }
                toStringOrInt(true);
            """;
            Value result = context.eval("relang", src);
            assertEquals(42, result.asLong());
        }

        @Test @DisplayName("union as function parameter")
        void testUnionParam() {
            var src = """
                fn identity(v: Int | String): Int | String = v;
                identity(42);
            """;
            Value result = context.eval("relang", src);
            assertEquals(42, result.asLong());
        }
    }

    @Nested
    @DisplayName("Type errors")
    class TypeErrors {
        @Test @DisplayName("Bool assigned to Int | String is error")
        void testBoolToIntStringUnion() {
            var ex = assertThrows(PolyglotException.class,
                    () -> context.eval("relang", "let v: Int | String = true; v;"));
            assertTrue(ex.getMessage().contains("Cannot assign"));
        }

        @Test @DisplayName("wrong product type is error")
        void testWrongProductType() {
            var ex = assertThrows(PolyglotException.class,
                    () -> context.eval("relang", "let p: Int & String = (42 & 100); p;"));
            assertTrue(ex.getMessage().contains("Cannot assign"));
        }
    }
}

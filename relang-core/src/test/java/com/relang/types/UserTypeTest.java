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

/**
 * Tests for user type declarations (Issue #6).
 * Covers: type declarations, field access, construction, sealed hierarchies, structural equality,
 * and type error detection.
 */
@DisplayName("User type declarations (Issue #6)")
public class UserTypeTest {

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
    @DisplayName("Type declaration and construction")
    class Construction {

        @Test
        @DisplayName("simple record construction")
        void testSimpleConstruction() {
            var src = """
                type Point { x: Int, y: Int }
                let p = Point { x: 1, y: 2 };
                p.x;
            """;
            Value result = context.eval("relang", src);
            assertEquals(1, result.asLong());
        }

        @Test
        @DisplayName("record with String fields")
        void testStringFields() {
            var src = """
                type User { id: String, name: String }
                let u = User { id: "u1", name: "Alice" };
                u.name;
            """;
            Value result = context.eval("relang", src);
            assertEquals("Alice", result.asString());
        }

        @Test
        @DisplayName("record with optional field set to none")
        void testOptionalFieldNone() {
            var src = """
                type User { id: String, email: String? }
                let u = User { id: "u1", email: none };
                u.id;
            """;
            Value result = context.eval("relang", src);
            assertEquals("u1", result.asString());
        }

        @Test
        @DisplayName("record with optional field set to value")
        void testOptionalFieldPresent() {
            var src = """
                type User { id: String, email: String? }
                let u = User { id: "u1", email: "alice@example.com" };
                u.email;
            """;
            Value result = context.eval("relang", src);
            assertEquals("alice@example.com", result.asString());
        }
    }

    @Nested
    @DisplayName("Field access")
    class FieldAccess {

        @Test
        @DisplayName("access Int field")
        void testAccessIntField() {
            var src = """
                type Point { x: Int, y: Int }
                let p = Point { x: 10, y: 20 };
                p.x;
            """;
            Value result = context.eval("relang", src);
            assertEquals(10, result.asLong());
        }

        @Test
        @DisplayName("access second Int field")
        void testAccessSecondField() {
            var src = """
                type Point { x: Int, y: Int }
                let p = Point { x: 10, y: 20 };
                p.y;
            """;
            Value result = context.eval("relang", src);
            assertEquals(20, result.asLong());
        }

        @Test
        @DisplayName("access String field")
        void testAccessStringField() {
            var src = """
                type User { id: String, name: String }
                let u = User { id: "u1", name: "Alice" };
                u.name;
            """;
            Value result = context.eval("relang", src);
            assertEquals("Alice", result.asString());
        }

        @Test
        @DisplayName("pass record to function and access field")
        void testPassToFunction() {
            var src = """
                type Point { x: Int, y: Int }
                fn getX(p: Point): Int = p.x;
                getX(Point { x: 42, y: 0 });
            """;
            Value result = context.eval("relang", src);
            assertEquals(42, result.asLong());
        }

        @Test
        @DisplayName("chained field access on nested records")
        void testNestedFieldAccess() {
            var src = """
                type Inner { value: Int }
                type Outer { inner: Inner }
                let o = Outer { inner: Inner { value: 99 } };
                o.inner.value;
            """;
            Value result = context.eval("relang", src);
            assertEquals(99, result.asLong());
        }

        @Test
        @DisplayName("field access in arithmetic expression")
        void testFieldAccessInArithmetic() {
            var src = """
                type Point { x: Int, y: Int }
                let p = Point { x: 3, y: 4 };
                p.x + p.y;
            """;
            Value result = context.eval("relang", src);
            assertEquals(7, result.asLong());
        }
    }

    @Nested
    @DisplayName("Structural equality")
    class StructuralEquality {

        @Test
        @DisplayName("equal records are equal")
        void testEqualRecords() {
            var src = """
                type Point { x: Int, y: Int }
                Point { x: 1, y: 2 } == Point { x: 1, y: 2 };
            """;
            Value result = context.eval("relang", src);
            assertTrue(result.asBoolean());
        }

        @Test
        @DisplayName("different records are not equal")
        void testDifferentRecords() {
            var src = """
                type Point { x: Int, y: Int }
                Point { x: 1, y: 2 } == Point { x: 1, y: 3 };
            """;
            Value result = context.eval("relang", src);
            assertFalse(result.asBoolean());
        }

        @Test
        @DisplayName("not-equals operator on records")
        void testNotEqualsRecords() {
            var src = """
                type Point { x: Int, y: Int }
                Point { x: 1, y: 2 } != Point { x: 1, y: 3 };
            """;
            Value result = context.eval("relang", src);
            assertTrue(result.asBoolean());
        }

        @Test
        @DisplayName("equal records via variables")
        void testEqualRecordsViaVariables() {
            var src = """
                type Point { x: Int, y: Int }
                let a = Point { x: 5, y: 10 };
                let b = Point { x: 5, y: 10 };
                a == b;
            """;
            Value result = context.eval("relang", src);
            assertTrue(result.asBoolean());
        }
    }

    @Nested
    @DisplayName("Sealed types")
    class SealedTypes {

        @Test
        @DisplayName("sealed type with subtypes - construct and access")
        void testSealedWithSubtypes() {
            var src = """
                sealed PaymentStatus
                type Authorized : PaymentStatus { authId: String }
                type Declined : PaymentStatus { reason: String }
                let s = Authorized { authId: "auth-123" };
                s.authId;
            """;
            Value result = context.eval("relang", src);
            assertEquals("auth-123", result.asString());
        }

        @Test
        @DisplayName("sealed subtype - Declined variant")
        void testDeclinedVariant() {
            var src = """
                sealed PaymentStatus
                type Authorized : PaymentStatus { authId: String }
                type Declined : PaymentStatus { reason: String }
                let s = Declined { reason: "insufficient funds" };
                s.reason;
            """;
            Value result = context.eval("relang", src);
            assertEquals("insufficient funds", result.asString());
        }
    }

    @Nested
    @DisplayName("Type errors")
    class TypeErrors {

        @Test
        @DisplayName("wrong field type in construction")
        void testWrongFieldType() {
            var src = """
                type Point { x: Int, y: Int }
                let p = Point { x: "hello", y: 2 };
                p;
            """;
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", src));
            assertTrue(ex.getMessage().contains("TypeError"), "Expected TypeError, got: " + ex.getMessage());
        }

        @Test
        @DisplayName("missing field in construction")
        void testMissingField() {
            var src = """
                type Point { x: Int, y: Int }
                let p = Point { x: 1 };
                p;
            """;
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", src));
            assertTrue(ex.getMessage().contains("TypeError"), "Expected TypeError, got: " + ex.getMessage());
        }

        @Test
        @DisplayName("unknown field in construction")
        void testUnknownField() {
            var src = """
                type Point { x: Int, y: Int }
                let p = Point { x: 1, y: 2, z: 3 };
                p;
            """;
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", src));
            assertTrue(ex.getMessage().contains("TypeError"), "Expected TypeError, got: " + ex.getMessage());
        }

        @Test
        @DisplayName("unknown type name in construction")
        void testUnknownTypeName() {
            var src = """
                let p = Nonexistent { x: 1 };
                p;
            """;
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", src));
            assertTrue(ex.getMessage().contains("TypeError"), "Expected TypeError, got: " + ex.getMessage());
        }

        @Test
        @DisplayName("field access on non-record type")
        void testFieldAccessOnNonRecord() {
            var src = """
                let x = 42;
                x.foo;
            """;
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", src));
            assertTrue(ex.getMessage().contains("TypeError"), "Expected TypeError, got: " + ex.getMessage());
        }

        @Test
        @DisplayName("access nonexistent field")
        void testAccessNonexistentField() {
            var src = """
                type Point { x: Int, y: Int }
                let p = Point { x: 1, y: 2 };
                p.z;
            """;
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", src));
            assertTrue(ex.getMessage().contains("TypeError"), "Expected TypeError, got: " + ex.getMessage());
        }
    }
}

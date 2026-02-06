package com.relang.primitives;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("New primitive types (Issue #4)")
public class NewPrimitiveTypesTest {

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
    @DisplayName("Duration literals")
    class DurationLiterals {
        @Test @DisplayName("seconds literal")
        void testSeconds() {
            Value result = context.eval("relang", "5s;");
            assertNotNull(result);
            assertEquals("5s", result.toString());
        }

        @Test @DisplayName("milliseconds literal")
        void testMilliseconds() {
            Value result = context.eval("relang", "200ms;");
            assertEquals("200ms", result.toString());
        }

        @Test @DisplayName("hours literal")
        void testHours() {
            Value result = context.eval("relang", "2h;");
            assertEquals("2h", result.toString());
        }

        @Test @DisplayName("minutes literal")
        void testMinutes() {
            Value result = context.eval("relang", "30min;");
            assertEquals("30min", result.toString());
        }

        @Test @DisplayName("duration in let with type annotation")
        void testLetDuration() {
            Value result = context.eval("relang", "let d: Duration = 5s; d;");
            assertEquals("5s", result.toString());
        }

        @Test @DisplayName("duration passed to function")
        void testDurationParam() {
            var src = """
                fn getTimeout(d: Duration): Duration = d;
                getTimeout(10s);
            """;
            Value result = context.eval("relang", src);
            assertEquals("10s", result.toString());
        }
    }

    @Nested
    @DisplayName("Bytes literals")
    class BytesLiterals {
        @Test @DisplayName("simple bytes literal")
        void testSimpleBytes() {
            Value result = context.eval("relang", "b\"\\x01\\x02\\x03\";");
            assertNotNull(result);
        }

        @Test @DisplayName("bytes in let with type annotation")
        void testLetBytes() {
            Value result = context.eval("relang", "let payload: Bytes = b\"\\x0a\\x0b\"; payload;");
            assertNotNull(result);
        }

        @Test @DisplayName("empty bytes")
        void testEmptyBytes() {
            Value result = context.eval("relang", "b\"\";");
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Timestamp (now)")
    class TimestampTests {
        @Test @DisplayName("now() returns a Timestamp")
        void testNow() {
            Value result = context.eval("relang", "now();");
            assertNotNull(result);
        }

        @Test @DisplayName("now() in let with type annotation")
        void testLetTimestamp() {
            Value result = context.eval("relang", "let t: Timestamp = now(); t;");
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Failure type")
    class FailureTests {
        @Test @DisplayName("Failure constructor")
        void testFailure() {
            Value result = context.eval("relang", "Failure(\"something went wrong\");");
            assertNotNull(result);
            assertTrue(result.toString().contains("something went wrong"));
        }

        @Test @DisplayName("Failure in let with type annotation")
        void testLetFailure() {
            Value result = context.eval("relang", "let err: Failure = Failure(\"oops\"); err;");
            assertTrue(result.toString().contains("oops"));
        }

        @Test @DisplayName("Failure passed to function")
        void testFailureParam() {
            var src = """
                fn handle(err: Failure): Failure = err;
                handle(Failure("test error"));
            """;
            Value result = context.eval("relang", src);
            assertTrue(result.toString().contains("test error"));
        }
    }

    @Nested
    @DisplayName("Json type")
    class JsonTests {
        @Test @DisplayName("json() constructor from string")
        void testJsonFromString() {
            var src = """
                let data: Json = json("{\\"id\\": \\"u-1\\"}");
                data;
            """;
            Value result = context.eval("relang", src);
            assertNotNull(result);
        }

        @Test @DisplayName("json() passed to function")
        void testJsonParam() {
            var src = """
                fn process(data: Json): Json = data;
                process(json("{\\"key\\": 42}"));
            """;
            Value result = context.eval("relang", src);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Type annotation errors")
    class TypeErrors {
        @Test @DisplayName("Int assigned to Duration is error")
        void testIntToDuration() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "let d: Duration = 42; d;"));
            assertTrue(ex.getMessage().contains("Cannot assign"));
        }

        @Test @DisplayName("String assigned to Bytes is error")
        void testStringToBytes() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "let b: Bytes = \"hello\"; b;"));
            assertTrue(ex.getMessage().contains("Cannot assign"));
        }

        @Test @DisplayName("Int assigned to Failure is error")
        void testIntToFailure() {
            var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", "let f: Failure = 42; f;"));
            assertTrue(ex.getMessage().contains("Cannot assign"));
        }
    }
}

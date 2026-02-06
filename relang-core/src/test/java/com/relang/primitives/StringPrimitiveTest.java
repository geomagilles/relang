package com.relang.primitives;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for String primitive type (spec section 3.2).
 * Strings are immutable sequences of characters.
 */
@DisplayName("String primitive")
public class StringPrimitiveTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- 3.2 Literals ---

    @Nested
    @DisplayName("Literals")
    class Literals {

        @Test
        @DisplayName("basic string literal")
        void testBasicLiteral() {
            assertEval("\"hello\";", "hello");
        }

        @Test
        @DisplayName("empty string")
        void testEmptyString() {
            assertEval("\"\";", "");
        }

        @Test
        @DisplayName("single character string")
        void testSingleChar() {
            assertEval("\"a\";", "a");
        }

        @Test
        @DisplayName("string with spaces")
        void testWithSpaces() {
            assertEval("\"hello world\";", "hello world");
        }

        @Test
        @DisplayName("string with digits")
        void testWithDigits() {
            assertEval("\"abc123\";", "abc123");
        }

        @Test
        @DisplayName("string in let binding")
        void testLetBinding() {
            assertEval("let s = \"hello\"; s;", "hello");
        }
    }

    // --- Escape sequences ---

    @Nested
    @DisplayName("Escape sequences")
    class EscapeSequences {

        @Test
        @DisplayName("newline escape: \\n")
        void testNewline() {
            assertEval("\"hello\\nworld\";", "hello\nworld");
        }

        @Test
        @DisplayName("tab escape: \\t")
        void testTab() {
            assertEval("\"hello\\tworld\";", "hello\tworld");
        }

        @Test
        @DisplayName("carriage return escape: \\r")
        void testCarriageReturn() {
            assertEval("\"hello\\rworld\";", "hello\rworld");
        }

        @Test
        @DisplayName("escaped quote: \\\"")
        void testEscapedQuote() {
            assertEval("\"say \\\"hi\\\"\";", "say \"hi\"");
        }

        @Test
        @DisplayName("escaped backslash: \\\\")
        void testEscapedBackslash() {
            assertEval("\"path\\\\to\\\\file\";", "path\\to\\file");
        }
    }

    // --- Concatenation ---

    @Nested
    @DisplayName("Concatenation")
    class Concatenation {

        @Test
        @DisplayName("two strings: \"hello\" + \" world\"")
        void testTwoStrings() {
            assertEval("\"hello\" + \" world\";", "hello world");
        }

        @Test
        @DisplayName("three strings: \"a\" + \"b\" + \"c\"")
        void testThreeStrings() {
            assertEval("\"a\" + \"b\" + \"c\";", "abc");
        }

        @Test
        @DisplayName("concatenation with empty string")
        void testConcatEmpty() {
            assertEval("\"hello\" + \"\";", "hello");
        }

        @Test
        @DisplayName("empty + empty = empty")
        void testEmptyPlusEmpty() {
            assertEval("\"\" + \"\";", "");
        }

        @Test
        @DisplayName("concatenation via variables")
        void testConcatViaVars() {
            var src = """
                let a = "hello";
                let b = " ";
                let c = "world";
                a + b + c;
            """;
            assertEval(src, "hello world");
        }

        @Test
        @DisplayName("concatenation with space separator")
        void testConcatWithSeparator() {
            assertEval("\"hello\" + \" \" + \"world\";", "hello world");
        }
    }

    // --- Equality ---

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("equal strings: \"abc\" == \"abc\"")
        void testEqualStrings() {
            assertEval("\"abc\" == \"abc\";", true);
        }

        @Test
        @DisplayName("different strings: \"abc\" == \"def\"")
        void testDifferentStrings() {
            assertEval("\"abc\" == \"def\";", false);
        }

        @Test
        @DisplayName("not equals true: \"abc\" != \"def\"")
        void testNotEqualsTrue() {
            assertEval("\"abc\" != \"def\";", true);
        }

        @Test
        @DisplayName("not equals false: \"abc\" != \"abc\"")
        void testNotEqualsFalse() {
            assertEval("\"abc\" != \"abc\";", false);
        }

        @Test
        @DisplayName("empty strings are equal")
        void testEmptyEqual() {
            assertEval("\"\" == \"\";", true);
        }

        @Test
        @DisplayName("case sensitive: \"Hello\" != \"hello\"")
        void testCaseSensitive() {
            assertEval("\"Hello\" == \"hello\";", false);
        }

        @Test
        @DisplayName("equality of concatenated result")
        void testConcatEquality() {
            assertEval("(\"ab\" + \"c\") == \"abc\";", true);
        }
    }

    // --- String in functions ---

    @Nested
    @DisplayName("Functions")
    class Functions {

        @Test
        @DisplayName("function returning String")
        void testStringReturn() {
            var src = """
                fn greet(name: String): String {
                    return "Hello " + name;
                }
                greet("World");
            """;
            assertEval(src, "Hello World");
        }

        @Test
        @DisplayName("expression body returning String")
        void testExprBody() {
            assertEval("fn wrap(s: String): String = \"[\" + s + \"]\"; wrap(\"ok\");", "[ok]");
        }

        @Test
        @DisplayName("String as function argument and return")
        void testStringRoundTrip() {
            var src = """
                fn identity(s: String): String { s }
                identity("test");
            """;
            assertEval(src, "test");
        }
    }

    // --- String in control flow ---

    @Nested
    @DisplayName("Control flow")
    class ControlFlow {

        @Test
        @DisplayName("String in if/else")
        void testIfElse() {
            var src = """
                let x = 1;
                let r = if x == 1 { "one" } else { "other" };
                r;
            """;
            assertEval(src, "one");
        }

        @Test
        @DisplayName("String in match")
        void testMatch() {
            var src = """
                let lang = "relang";
                let r = match lang {
                    "relang" -> "correct",
                    _ -> "wrong"
                };
                r;
            """;
            assertEval(src, "correct");
        }

        @Test
        @DisplayName("String equality as condition")
        void testEqualityCondition() {
            var src = """
                let cmd = "stop";
                if cmd == "stop" { 1 } else { 0 };
            """;
            assertEval(src, 1);
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

    private void assertEval(String source, long expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asLong());
    }
}

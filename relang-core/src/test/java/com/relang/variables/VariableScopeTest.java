package com.relang.variables;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for variable scope and visibility (spec sections 4, 8).
 * Lexical scoping, shadowing, and variable visibility rules.
 */
@DisplayName("Variable scope")
public class VariableScopeTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // --- Scope basics ---

    @Nested
    @DisplayName("Scope basics")
    class ScopeBasics {

        @Test
        @DisplayName("variable visible after declaration")
        void testVisibleAfterDecl() {
            assertEval("let x = 42; x;", 42);
        }

        @Test
        @DisplayName("variable visible in subsequent statements")
        void testVisibleInSubsequent() {
            var src = """
                let x = 10;
                let y = x + 5;
                y;
            """;
            assertEval(src, 15);
        }

        @Test
        @DisplayName("function body has its own scope")
        void testFunctionScope() {
            var src = """
                fn getVal(): Int {
                    let x = 99;
                    x
                }
                let x = 1;
                let r = getVal();
                x + r;
            """;
            assertEval(src, 100); // x=1, r=99
        }
    }

    // --- Variables in blocks ---

    @Nested
    @DisplayName("Variables in blocks")
    class InBlocks {

        @Test
        @DisplayName("variable declared in if block modifies outer scope")
        void testIfBlockModifiesOuter() {
            var src = """
                let x = 0;
                if true {
                    x = 42;
                }
                x;
            """;
            assertEval(src, 42);
        }

        @Test
        @DisplayName("variable declared in while block modifies outer scope")
        void testWhileBlockModifiesOuter() {
            var src = """
                let sum = 0;
                let i = 0;
                while i < 3 {
                    sum = sum + i;
                    i = i + 1;
                }
                sum;
            """;
            assertEval(src, 3); // 0+1+2
        }

        @Test
        @DisplayName("variable declared in for block modifies outer scope")
        void testForBlockModifiesOuter() {
            var src = """
                let sum = 0;
                for i in 0..3 {
                    sum = sum + i;
                }
                sum;
            """;
            assertEval(src, 3); // 0+1+2
        }
    }

    // --- Variables in functions ---

    @Nested
    @DisplayName("Function scope")
    class FunctionScope {

        @Test
        @DisplayName("function parameters are local")
        void testParamsLocal() {
            var src = """
                fn add(a: Int, b: Int): Int { a + b }
                add(3, 4);
            """;
            assertEval(src, 7);
        }

        @Test
        @DisplayName("nested function calls have independent scopes")
        void testNestedCalls() {
            var src = """
                fn square(x: Int): Int { x * x }
                fn sumOfSquares(a: Int, b: Int): Int {
                    square(a) + square(b)
                }
                sumOfSquares(3, 4);
            """;
            assertEval(src, 25); // 9 + 16
        }

        @Test
        @DisplayName("recursive function variables are independent per frame")
        void testRecursiveScopes() {
            var src = """
                fn fib(n: Int): Int {
                    if n < 2 { n } else { fib(n - 1) + fib(n - 2) }
                }
                fib(10);
            """;
            assertEval(src, 55);
        }
    }

    // --- Variables with await ---

    @Nested
    @DisplayName("Variables with await")
    class WithAwait {

        @Test
        @DisplayName("await result bound to variable")
        void testAwaitLetBinding() {
            var src = """
                let task = resolved(42);
                let result = await task;
                result;
            """;
            assertEval(src, 42);
        }

        @Test
        @DisplayName("multiple await bindings")
        void testMultipleAwaitBindings() {
            var src = """
                let a = await resolved(10);
                let b = await resolved(20);
                a + b;
            """;
            assertEval(src, 30);
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
}

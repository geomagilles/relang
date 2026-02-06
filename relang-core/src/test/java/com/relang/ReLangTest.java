package com.relang;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReLangTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void testArithmetic() {
        assertEval("1 + 2;", 3);
        assertEval("10 * 20;", 200);
        assertEval("20 / 2;", 10);
        assertEval("10 - 2;", 8);
        assertEval("2 + 3 * 4;", 14); // 2 + 12
        assertEval("(2 + 3) * 4;", 20); // 5 * 4
    }

    @Test
    void testVariables() {
        assertEval("x = 10; x;", 10);
        assertEval("x = 10; y = 20; x + y;", 30);
        assertEval("x = 10; x = 20; x;", 20);
    }

    @Test
    void testComparison() {
        assertEval("1 < 2;", true);
        assertEval("2 < 1;", false);
        assertEval("1 == 1;", true);
        assertEval("1 == 2;", false);
    }

    @Test
    void testIf() {
        assertEval("if (true) { x=10; } else { x=20; } x;", 10);
        assertEval("if (false) { x=10; } else { x=20; } x;", 20);
        assertEval("x=0; if (1 < 2) { x=100; } x;", 100);
    }

    @Test
    void testWhile() {
        assertEval("i = 0; while (i < 5) { i = i + 1; } i;", 5);
        assertEval("i = 10; sum = 0; while (0 < i) { sum = sum + i; i = i - 1; } sum;", 55);
    }

    @Test
    void testFunctions() {
        String src = "fn add(a, b) { return a + b; } add(10, 20);";
        assertEval(src, 30);

        String src2 = "fn fac(n) { if (n < 2) { return 1; } else { return n * fac(n - 1); } } fac(5);";
        assertEval(src2, 120);
    }

    @Test
    void testIntegration() {
        String src = """
                    fn fib(n) {
                        if (n < 2) { return n; }
                        return fib(n - 1) + fib(n - 2);
                    }
                    fib(10);
                """;
        assertEval(src, 55);
    }

    @Test
    void testFloatArithmetic() {
        assertEval("3.14;", 3.14);
        assertEval("1.0 + 2.5;", 3.5);
        assertEval("10.0 - 3.5;", 6.5);
        assertEval("2.0 * 3.0;", 6.0);
        assertEval("7.0 / 2.0;", 3.5);
    }

    @Test
    void testFloatComparison() {
        assertEval("1.0 < 2.0;", true);
        assertEval("2.0 < 1.0;", false);
        assertEval("3.14 == 3.14;", true);
        assertEval("3.14 != 2.0;", true);
        assertEval("1.0 <= 1.0;", true);
        assertEval("2.0 > 1.0;", true);
        assertEval("1.0 >= 1.0;", true);
    }

    @Test
    void testStringLiterals() {
        assertEval("\"hello\";", "hello");
        assertEval("\"hello\" + \" \" + \"world\";", "hello world");
    }

    @Test
    void testStringComparison() {
        assertEval("\"abc\" == \"abc\";", true);
        assertEval("\"abc\" != \"def\";", true);
        assertEval("\"abc\" == \"def\";", false);
    }

    @Test
    void testBoolLiterals() {
        assertEval("true;", true);
        assertEval("false;", false);
    }

    @Test
    void testLogicalOperators() {
        assertEval("true and true;", true);
        assertEval("true and false;", false);
        assertEval("false and true;", false);
        assertEval("false or true;", true);
        assertEval("true or false;", true);
        assertEval("false or false;", false);
        assertEval("not true;", false);
        assertEval("not false;", true);
    }

    @Test
    void testModulo() {
        assertEval("7 % 3;", 1);
        assertEval("10 % 5;", 0);
    }

    @Test
    void testNegate() {
        assertEval("-(5);", -5);
        assertEval("-(3.14);", -3.14);
    }

    @Test
    void testAdditionalComparison() {
        assertEval("1 != 2;", true);
        assertEval("1 != 1;", false);
        assertEval("1 <= 2;", true);
        assertEval("2 <= 2;", true);
        assertEval("3 <= 2;", false);
        assertEval("2 > 1;", true);
        assertEval("1 > 2;", false);
        assertEval("2 >= 2;", true);
        assertEval("2 >= 3;", false);
    }

    @Test
    void testNoneLiteral() {
        assertEvalNull("none;");
    }

    @Test
    void testLetDeclaration() {
        assertEval("let x = 42; x;", 42);
        assertEval("let s = \"hello\"; s;", "hello");
        assertEval("let b = true; b;", true);
    }

    @Test
    void testUnderscoreInNumbers() {
        assertEval("1_000_000;", 1000000);
        assertEval("1_000.5;", 1000.5);
    }

    @Test
    void testIfWithoutParens() {
        assertEval("x = 10; if x > 5 { x = 1; } else { x = 2; } x;", 1);
        assertEval("x = 3; if x > 5 { x = 1; } else { x = 2; } x;", 2);
    }

    @Test
    void testIfAsExpression() {
        assertEval("let x = 10; let r = if x > 5 { 1 } else { 2 }; r;", 1);
        assertEval("let x = 3; let r = if x > 5 { 1 } else { 2 }; r;", 2);
    }

    @Test
    void testMatchSubjectLiterals() {
        var src = """
            let x = 2;
            let r = match x {
                1 -> "one",
                2 -> "two",
                3 -> "three",
                _ -> "other"
            };
            r;
        """;
        assertEval(src, "two");
    }

    @Test
    void testMatchSubjectWildcard() {
        var src = """
            let x = 99;
            let r = match x {
                1 -> "one",
                _ -> "other"
            };
            r;
        """;
        assertEval(src, "other");
    }

    @Test
    void testMatchSubjectNone() {
        var src = """
            let x = none;
            let r = match x {
                none -> "absent",
                _ -> "present"
            };
            r;
        """;
        assertEval(src, "absent");
    }

    @Test
    void testMatchSubjectless() {
        var src = """
            let score = 85;
            let grade = match {
                score >= 90 -> "A",
                score >= 80 -> "B",
                score >= 70 -> "C",
                _ -> "F"
            };
            grade;
        """;
        assertEval(src, "B");
    }

    @Test
    void testMatchSubjectlessAllCases() {
        var src = """
            let age = 15;
            let category = match {
                age < 13 -> "child",
                age < 20 -> "teenager",
                age < 65 -> "adult",
                _ -> "senior"
            };
            category;
        """;
        assertEval(src, "teenager");
    }

    @Test
    void testForRangeExclusive() {
        var src = """
            let sum = 0;
            for i in 0..5 {
                sum = sum + i;
            }
            sum;
        """;
        assertEval(src, 10); // 0+1+2+3+4
    }

    @Test
    void testForRangeInclusive() {
        var src = """
            let sum = 0;
            for i in 1..=5 {
                sum = sum + i;
            }
            sum;
        """;
        assertEval(src, 15); // 1+2+3+4+5
    }

    @Test
    void testBreakInWhile() {
        var src = """
            let i = 0;
            while true {
                if i == 5 { break; }
                i = i + 1;
            }
            i;
        """;
        assertEval(src, 5);
    }

    @Test
    void testBreakInFor() {
        var src = """
            let last = 0;
            for i in 0..100 {
                if i == 3 { break; }
                last = i;
            }
            last;
        """;
        assertEval(src, 2);
    }

    @Test
    void testContinueInFor() {
        var src = """
            let sum = 0;
            for i in 0..10 {
                if i % 2 == 0 { continue; }
                sum = sum + i;
            }
            sum;
        """;
        assertEval(src, 25); // 1+3+5+7+9
    }

    @Test
    void testNestedForLoops() {
        var src = """
            let sum = 0;
            for i in 0..3 {
                for j in 0..3 {
                    sum = sum + 1;
                }
            }
            sum;
        """;
        assertEval(src, 9);
    }

    @Test
    void testWhileWithoutParens() {
        var src = """
            let i = 0;
            while i < 5 {
                i = i + 1;
            }
            i;
        """;
        assertEval(src, 5);
    }

    @Test
    void testMatchWithBlock() {
        var src = """
            let x = 2;
            let r = match x {
                1 -> { let a = 10; a + 1 },
                2 -> { let a = 20; a + 2 },
                _ -> 0
            };
            r;
        """;
        assertEval(src, 22);
    }

    // === S4: Functions & Typed Parameters ===

    @Test
    void testTypedParameters() {
        // Typed params should work (types parsed but not enforced yet)
        assertEval("fn add(a: Int, b: Int): Int { return a + b; } add(3, 4);", 7);
        assertEval("fn greet(name: String): String { return \"Hello \" + name; } greet(\"World\");", "Hello World");
    }

    @Test
    void testExpressionBodyFunction() {
        assertEval("fn double(x: Int): Int = x * 2; double(5);", 10);
        assertEval("fn add(a: Int, b: Int) = a + b; add(3, 4);", 7);
    }

    @Test
    void testImplicitReturn() {
        // Last expression is the return value (no explicit return needed)
        var src = """
            fn max(a: Int, b: Int): Int {
                if a > b { a } else { b }
            }
            max(10, 20);
        """;
        assertEval(src, 20);
    }

    @Test
    void testDefaultParameters() {
        var src = """
            fn greet(name: String, greeting: String = "Hello"): String {
                return greeting + " " + name;
            }
            greet("Alice");
        """;
        assertEval(src, "Hello Alice");
    }

    @Test
    void testDefaultParametersOverride() {
        var src = """
            fn greet(name: String, greeting: String = "Hello"): String {
                return greeting + " " + name;
            }
            greet("Alice", "Hi");
        """;
        assertEval(src, "Hi Alice");
    }

    @Test
    void testNamedArguments() {
        var src = """
            fn add(a: Int, b: Int): Int {
                return a + b;
            }
            add(b: 20, a: 10);
        """;
        assertEval(src, 30);
    }

    @Test
    void testMixedPositionalAndNamed() {
        var src = """
            fn compute(a: Int, b: Int, c: Int): Int {
                return a + b + c;
            }
            compute(1, c: 3, b: 2);
        """;
        assertEval(src, 6);
    }

    @Test
    void testFunctionHoisting() {
        // Functions are hoisted: second function calls the first, definition order doesn't matter
        var src = """
            fn double(x: Int): Int = x * 2;
            fn quadruple(x: Int): Int = double(double(x));
            quadruple(3);
        """;
        assertEval(src, 12);
    }

    @Test
    void testMutualRecursion() {
        var src = """
            fn isEven(n: Int): Bool {
                if n == 0 { true } else { isOdd(n - 1) }
            }
            fn isOdd(n: Int): Bool {
                if n == 0 { false } else { isEven(n - 1) }
            }
            isEven(10);
        """;
        assertEval(src, true);
    }

    @Test
    void testImplicitReturnMatch() {
        var src = """
            fn describe(n: Int): String {
                match n {
                    0 -> "zero",
                    1 -> "one",
                    _ -> "many"
                }
            }
            describe(1);
        """;
        assertEval(src, "one");
    }

    @Test
    void testExprBodyNoTypeAnnotation() {
        // Expression body without type annotations
        assertEval("fn square(x) = x * x; square(6);", 36);
    }

    @Test
    void testMultipleDefaultParams() {
        var src = """
            fn make(a: Int, b: Int = 10, c: Int = 100): Int {
                return a + b + c;
            }
            make(1);
        """;
        assertEval(src, 111);
    }

    @Test
    void testMultipleDefaultParamsPartialOverride() {
        var src = """
            fn make(a: Int, b: Int = 10, c: Int = 100): Int {
                return a + b + c;
            }
            make(1, 20);
        """;
        assertEval(src, 121);
    }

    // === S5: Awaitables & Await ===

    @Test
    void testAwaitResolved() {
        // resolved(42) creates a *Int already resolved to 42
        assertEval("await resolved(42);", 42);
    }

    @Test
    void testAwaitResolvedString() {
        assertEval("await resolved(\"hello\");", "hello");
    }

    @Test
    void testAwaitResolvedBool() {
        assertEval("await resolved(true);", true);
    }

    @Test
    void testAwaitResolvedDouble() {
        assertEval("await resolved(3.14);", 3.14);
    }

    @Test
    void testAwaitInFunction() {
        var src = """
            fn work(): Int {
                let x = await resolved(10);
                let y = await resolved(20);
                x + y
            }
            work();
        """;
        assertEval(src, 30);
    }

    @Test
    void testAwaitInExpression() {
        assertEval("let x = await resolved(5); x * 2;", 10);
    }

    @Test
    void testAwaitChained() {
        var src = """
            let a = await resolved(10);
            let b = await resolved(20);
            let c = await resolved(30);
            a + b + c;
        """;
        assertEval(src, 60);
    }

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

    private void assertEval(String source, double expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asDouble(), 0.0001);
    }

    private void assertEvalNull(String source) {
        Value result = context.eval("relang", source);
        assertTrue(result.isNull(), "Expected null/none but got: " + result);
    }
}

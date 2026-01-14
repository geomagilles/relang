package com.relang;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEval("if (1) { x=10; } else { x=20; } x;", 10);
        assertEval("if (0) { x=10; } else { x=20; } x;", 20);
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

    private void assertEval(String source, long expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asLong());
    }

    private void assertEval(String source, boolean expected) {
        Value result = context.eval("relang", source);
        assertEquals(expected, result.asBoolean());
    }
}

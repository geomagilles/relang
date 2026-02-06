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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for the ReLang static type checker (Phase 1).
 */
@DisplayName("ReLang Type Checker")
public class ReLangTypeCheckerTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    // ================================================================
    // 1. Arithmetic type errors
    // ================================================================

    @Nested
    @DisplayName("Arithmetic type errors")
    class ArithmeticTypeErrors {

        @Test
        @DisplayName("Int + Float = Float (implicit widening)")
        void testIntPlusFloat() {
            var src = """
                fn add(a: Int, b: Float): Float {
                    return a + b;
                }
                add(1, 2.0);
            """;
            assertEval(src, 3.0);
        }

        @Test
        @DisplayName("String - String is a type error")
        void testStringMinusString() {
            var src = """
                fn sub(a: String, b: String): String {
                    return a - b;
                }
                sub("a", "b");
            """;
            assertTypeError(src, "Operator '-'");
        }

        @Test
        @DisplayName("Bool + Bool is a type error")
        void testBoolPlusBool() {
            var src = """
                fn add(a: Bool, b: Bool): Bool {
                    return a + b;
                }
                add(true, false);
            """;
            assertTypeError(src, "Operator '+'");
        }

        @Test
        @DisplayName("String * Int is a type error")
        void testStringTimesInt() {
            var src = """
                fn mul(a: String, b: Int): String {
                    return a * b;
                }
                mul("x", 3);
            """;
            assertTypeError(src, "Operator '*'");
        }

        @Test
        @DisplayName("Bool / Bool is a type error")
        void testBoolDivBool() {
            var src = """
                fn div(a: Bool, b: Bool): Bool {
                    return a / b;
                }
                div(true, false);
            """;
            assertTypeError(src, "Operator '/'");
        }
    }

    // ================================================================
    // 2. Modulo Int-only
    // ================================================================

    @Nested
    @DisplayName("Modulo type errors")
    class ModuloTypeErrors {

        @Test
        @DisplayName("Float % Float is a type error")
        void testFloatModFloat() {
            var src = """
                fn modf(a: Float, b: Float): Float {
                    return a % b;
                }
                modf(3.14, 2.0);
            """;
            assertTypeError(src, "Operator '%'");
        }

        @Test
        @DisplayName("String % Int is a type error")
        void testStringModInt() {
            var src = """
                fn modf(a: String, b: Int): Int {
                    return a % b;
                }
                modf("hello", 3);
            """;
            assertTypeError(src, "Operator '%'");
        }

        @Test
        @DisplayName("Int % Int is valid")
        void testIntModInt() {
            assertEval("fn modf(a: Int, b: Int): Int = a % b; modf(7, 3);", 1);
        }
    }

    // ================================================================
    // 3. Condition must be Bool
    // ================================================================

    @Nested
    @DisplayName("Condition must be Bool")
    class ConditionMustBeBool {

        @Test
        @DisplayName("if with Int literal condition")
        void testIfIntCondition() {
            assertTypeError("if (1) { 10; }", "Condition must be Bool");
        }

        @Test
        @DisplayName("if with String literal condition")
        void testIfStringCondition() {
            assertTypeError("if (\"hello\") { 10; }", "Condition must be Bool");
        }

        @Test
        @DisplayName("if with Float literal condition")
        void testIfFloatCondition() {
            assertTypeError("if (1.0) { 10; }", "Condition must be Bool");
        }

        @Test
        @DisplayName("if with none condition")
        void testIfNoneCondition() {
            assertTypeError("if (none) { 10; }", "Condition must be Bool");
        }

        @Test
        @DisplayName("while with Int condition")
        void testWhileIntCondition() {
            assertTypeError("while (1) { break; }", "Condition must be Bool");
        }

        @Test
        @DisplayName("while with String condition")
        void testWhileStringCondition() {
            assertTypeError("while (\"yes\") { break; }", "Condition must be Bool");
        }

        @Test
        @DisplayName("if-no-parens with Int condition")
        void testIfNoParensIntCondition() {
            assertTypeError("if 1 { 10; }", "Condition must be Bool");
        }

        @Test
        @DisplayName("while-no-parens with String condition")
        void testWhileNoParensStringCondition() {
            assertTypeError("while \"yes\" { break; }", "Condition must be Bool");
        }
    }

    // ================================================================
    // 4. Logical operators require Bool
    // ================================================================

    @Nested
    @DisplayName("Logical operators require Bool")
    class LogicalOperatorErrors {

        @Test
        @DisplayName("Int and Int is a type error")
        void testIntAndInt() {
            var src = """
                fn f(a: Int, b: Int): Bool {
                    return a and b;
                }
                f(1, 2);
            """;
            assertTypeError(src, "Operator 'and' requires Bool");
        }

        @Test
        @DisplayName("String or String is a type error")
        void testStringOrString() {
            var src = """
                fn f(a: String, b: String): Bool {
                    return a or b;
                }
                f("a", "b");
            """;
            assertTypeError(src, "Operator 'or' requires Bool");
        }

        @Test
        @DisplayName("not Int is a type error")
        void testNotInt() {
            var src = """
                fn f(a: Int): Bool {
                    return not a;
                }
                f(1);
            """;
            assertTypeError(src, "Operator 'not' requires Bool");
        }
    }

    // ================================================================
    // 5. Comparison type mismatch
    // ================================================================

    @Nested
    @DisplayName("Comparison type mismatches")
    class ComparisonTypeErrors {

        @Test
        @DisplayName("Int < String is a type error")
        void testIntLessThanString() {
            var src = """
                fn f(a: Int, b: String): Bool {
                    return a < b;
                }
                f(1, "x");
            """;
            assertTypeError(src, "Operator '<'");
        }

        @Test
        @DisplayName("Bool < Int is a type error")
        void testBoolLessThanInt() {
            var src = """
                fn f(a: Bool, b: Int): Bool {
                    return a < b;
                }
                f(true, 1);
            """;
            assertTypeError(src, "Operator '<'");
        }

        @Test
        @DisplayName("Int == String is a type error")
        void testIntEqualsString() {
            var src = """
                fn f(a: Int, b: String): Bool {
                    return a == b;
                }
                f(1, "x");
            """;
            assertTypeError(src, "Operator '=='");
        }

        @Test
        @DisplayName("Float != Bool is a type error")
        void testFloatNotEqualsBool() {
            var src = """
                fn f(a: Float, b: Bool): Bool {
                    return a != b;
                }
                f(1.0, true);
            """;
            assertTypeError(src, "Operator '!='");
        }
    }

    // ================================================================
    // 6. Negation type errors
    // ================================================================

    @Nested
    @DisplayName("Negation type errors")
    class NegationTypeErrors {

        @Test
        @DisplayName("-\"hello\" is a type error")
        void testNegateString() {
            var src = """
                fn f(s: String): String {
                    return -s;
                }
                f("hello");
            """;
            assertTypeError(src, "Cannot negate type String");
        }

        @Test
        @DisplayName("-true is a type error")
        void testNegateBool() {
            var src = """
                fn f(b: Bool): Bool {
                    return -b;
                }
                f(true);
            """;
            assertTypeError(src, "Cannot negate type Bool");
        }

        @Test
        @DisplayName("-42 is valid (Int)")
        void testNegateInt() {
            assertEval("fn f(x: Int): Int = -x; f(42);", -42);
        }

        @Test
        @DisplayName("-3.14 is valid (Float)")
        void testNegateFloat() {
            assertEval("fn f(x: Float): Float = -x; f(3.14);", -3.14);
        }
    }

    // ================================================================
    // 7. Function call checks
    // ================================================================

    @Nested
    @DisplayName("Function call checks")
    class FunctionCallErrors {

        @Test
        @DisplayName("too few arguments")
        void testTooFewArgs() {
            var src = """
                fn add(a: Int, b: Int): Int { return a + b; }
                add(1);
            """;
            assertTypeError(src, "requires at least 2 arguments");
        }

        @Test
        @DisplayName("too many arguments")
        void testTooManyArgs() {
            var src = """
                fn add(a: Int, b: Int): Int { return a + b; }
                add(1, 2, 3);
            """;
            assertTypeError(src, "accepts at most 2 arguments");
        }

        @Test
        @DisplayName("wrong argument type")
        void testWrongArgType() {
            var src = """
                fn double(x: Int): Int { return x * 2; }
                double("hello");
            """;
            assertTypeError(src, "expected Int but got String");
        }

        @Test
        @DisplayName("unknown function")
        void testUnknownFunction() {
            assertTypeError("nonexistent(42);", "Unknown function");
        }

        @Test
        @DisplayName("correct arity with defaults")
        void testCorrectArityWithDefaults() {
            var src = """
                fn greet(name: String, greeting: String = "Hello"): String {
                    return greeting + " " + name;
                }
                greet("Alice");
            """;
            assertEval(src, "Hello Alice");
        }
    }

    // ================================================================
    // 8. Return type mismatch
    // ================================================================

    @Nested
    @DisplayName("Return type mismatch")
    class ReturnTypeMismatch {

        @Test
        @DisplayName("function returns String instead of Int")
        void testReturnStringInsteadOfInt() {
            var src = """
                fn foo(): Int {
                    return "hello";
                }
                foo();
            """;
            assertTypeError(src, "Return type mismatch");
        }

        @Test
        @DisplayName("expression body returns wrong type")
        void testExprBodyWrongType() {
            var src = """
                fn foo(): Int = "hello";
                foo();
            """;
            assertTypeError(src, "Return type mismatch");
        }

        @Test
        @DisplayName("correct return type passes")
        void testCorrectReturnType() {
            assertEval("fn square(x: Int): Int = x * x; square(5);", 25);
        }
    }

    // ================================================================
    // 9. Variable type mismatch on reassignment
    // ================================================================

    @Nested
    @DisplayName("Variable type mismatch")
    class VariableTypeMismatch {

        @Test
        @DisplayName("reassign Int variable to String")
        void testReassignIntToString() {
            var src = """
                fn f(): Int {
                    let x = 1;
                    x = "hello";
                    return x;
                }
                f();
            """;
            assertTypeError(src, "Cannot assign String");
        }

        @Test
        @DisplayName("reassign to same type is ok")
        void testReassignSameType() {
            assertEval("let x = 1; x = 2; x;", 2);
        }

        @Test
        @DisplayName("reassign to none is ok")
        void testReassignToNone() {
            // none is assignable to any type
            var src = """
                let x = 42;
                x = none;
                x;
            """;
            Value result = context.eval("relang", src);
            assertTrue(result.isNull());
        }
    }

    // ================================================================
    // 10. For-range bounds must be Int
    // ================================================================

    @Nested
    @DisplayName("For-range bounds")
    class ForRangeBounds {

        @Test
        @DisplayName("Bool bounds in for-range is an error")
        void testBoolBounds() {
            var src = """
                fn f(a: Bool, b: Bool): Int {
                    let sum = 0;
                    for i in a..b { sum = sum + i; }
                    return sum;
                }
                f(true, false);
            """;
            assertTypeError(src, "must be Int");
        }

        @Test
        @DisplayName("String bounds in for-range is an error")
        void testStringBounds() {
            var src = """
                fn f(a: String, b: String): Int {
                    let sum = 0;
                    for i in a..b { sum = sum + i; }
                    return sum;
                }
                f("a", "z");
            """;
            assertTypeError(src, "must be Int");
        }

        @Test
        @DisplayName("Int bounds in for-range is valid")
        void testIntBounds() {
            var src = """
                let sum = 0;
                for i in 0..5 { sum = sum + i; }
                sum;
            """;
            assertEval(src, 10);
        }
    }

    // ================================================================
    // 11. Positive tests: all valid operations still work
    // ================================================================

    @Nested
    @DisplayName("Valid operations pass type checking")
    class ValidOperations {

        @Test
        @DisplayName("Int + Int")
        void testIntPlusInt() {
            assertEval("fn add(a: Int, b: Int): Int = a + b; add(3, 4);", 7);
        }

        @Test
        @DisplayName("Float + Float")
        void testFloatPlusFloat() {
            assertEval("fn add(a: Float, b: Float): Float = a + b; add(1.5, 2.5);", 4.0);
        }

        @Test
        @DisplayName("String + String")
        void testStringPlusString() {
            assertEval("fn cat(a: String, b: String): String = a + b; cat(\"hello \", \"world\");", "hello world");
        }

        @Test
        @DisplayName("Int * Int")
        void testIntTimesInt() {
            assertEval("fn mul(a: Int, b: Int): Int = a * b; mul(3, 4);", 12);
        }

        @Test
        @DisplayName("Float * Float")
        void testFloatTimesFloat() {
            assertEval("fn mul(a: Float, b: Float): Float = a * b; mul(2.0, 3.0);", 6.0);
        }

        @Test
        @DisplayName("Int / Int")
        void testIntDivInt() {
            assertEval("fn div(a: Int, b: Int): Int = a / b; div(10, 2);", 5);
        }

        @Test
        @DisplayName("Float / Float")
        void testFloatDivFloat() {
            assertEval("fn div(a: Float, b: Float): Float = a / b; div(7.0, 2.0);", 3.5);
        }

        @Test
        @DisplayName("Int - Int")
        void testIntMinusInt() {
            assertEval("fn sub(a: Int, b: Int): Int = a - b; sub(10, 3);", 7);
        }

        @Test
        @DisplayName("Float - Float")
        void testFloatMinusFloat() {
            assertEval("fn sub(a: Float, b: Float): Float = a - b; sub(10.0, 3.5);", 6.5);
        }

        @Test
        @DisplayName("Int comparisons return Bool")
        void testIntComparisons() {
            assertEval("fn lt(a: Int, b: Int): Bool = a < b; lt(1, 2);", true);
            assertEval("fn le(a: Int, b: Int): Bool = a <= b; le(1, 1);", true);
            assertEval("fn gt(a: Int, b: Int): Bool = a > b; gt(2, 1);", true);
            assertEval("fn ge(a: Int, b: Int): Bool = a >= b; ge(1, 1);", true);
        }

        @Test
        @DisplayName("Float comparisons return Bool")
        void testFloatComparisons() {
            assertEval("fn lt(a: Float, b: Float): Bool = a < b; lt(1.0, 2.0);", true);
        }

        @Test
        @DisplayName("Equality operators")
        void testEquality() {
            assertEval("fn eq(a: Int, b: Int): Bool = a == b; eq(1, 1);", true);
            assertEval("fn eq(a: String, b: String): Bool = a == b; eq(\"a\", \"a\");", true);
            assertEval("fn eq(a: Bool, b: Bool): Bool = a == b; eq(true, true);", true);
            assertEval("fn ne(a: Int, b: Int): Bool = a != b; ne(1, 2);", true);
        }

        @Test
        @DisplayName("Logical operators with Bool")
        void testLogicalOps() {
            assertEval("fn f(a: Bool, b: Bool): Bool = a and b; f(true, true);", true);
            assertEval("fn f(a: Bool, b: Bool): Bool = a or b; f(true, false);", true);
            assertEval("fn f(a: Bool): Bool = not a; f(false);", true);
        }

        @Test
        @DisplayName("Recursive function with explicit return type works")
        void testRecursiveWithReturnType() {
            assertEval("fn fac(n: Int): Int { if n < 2 { 1 } else { n * fac(n - 1) } } fac(5);", 120);
        }

        @Test
        @DisplayName("Return type inference works for non-recursive functions")
        void testReturnTypeInference() {
            assertEval("fn square(x: Int) = x * x; square(5);", 25);
        }

        @Test
        @DisplayName("Return type inference for block body")
        void testReturnTypeInferenceBlock() {
            assertEval("fn double(x: Int) { x * 2 } double(5);", 10);
        }

        @Test
        @DisplayName("if-else expression with matching branch types")
        void testIfElseExpr() {
            assertEval("let x = if true { 10 } else { 20 }; x;", 10);
        }

        @Test
        @DisplayName("match expression works")
        void testMatchExpr() {
            var src = """
                let x = 2;
                let r = match x {
                    1 -> "one",
                    2 -> "two",
                    _ -> "other"
                };
                r;
            """;
            assertEval(src, "two");
        }

        @Test
        @DisplayName("subjectless match works")
        void testSubjectlessMatch() {
            var src = """
                let score = 85;
                let grade = match {
                    score >= 90 -> "A",
                    score >= 80 -> "B",
                    _ -> "F"
                };
                grade;
            """;
            assertEval(src, "B");
        }

        @Test
        @DisplayName("await resolved works")
        void testAwaitResolved() {
            assertEval("await resolved(42);", 42);
        }

        @Test
        @DisplayName("for-range with Int bounds")
        void testForRange() {
            var src = """
                let sum = 0;
                for i in 1..=5 { sum = sum + i; }
                sum;
            """;
            assertEval(src, 15);
        }

        @Test
        @DisplayName("break and continue in loops")
        void testBreakContinue() {
            var src = """
                let sum = 0;
                for i in 0..10 {
                    if i == 5 { break; }
                    sum = sum + i;
                }
                sum;
            """;
            assertEval(src, 10);
        }

        @Test
        @DisplayName("optional type allows none")
        void testOptionalType() {
            var src = """
                fn findOrNone(x: Int): Int? {
                    if x > 0 { x } else { none }
                }
                findOrNone(5);
            """;
            assertEval(src, 5);
        }
    }

    // ================================================================
    // 12. Missing type annotations
    // ================================================================

    @Nested
    @DisplayName("Missing type annotations")
    class MissingTypeAnnotations {

        @Test
        @DisplayName("Untyped parameters are rejected")
        void testUntypedParams() {
            assertTypeError("fn add(a, b) { return a + b; } add(1, 2);",
                    "must have a type annotation");
        }

        @Test
        @DisplayName("Partially typed parameters are rejected")
        void testPartiallyTypedParams() {
            assertTypeError("fn add(a: Int, b) { return a + b; } add(1, 2);",
                    "must have a type annotation");
        }

        @Test
        @DisplayName("Untyped parameter with expression body is rejected")
        void testUntypedExprBody() {
            assertTypeError("fn f(x) = x; f(1);",
                    "must have a type annotation");
        }

        @Test
        @DisplayName("Multiple untyped parameters are rejected")
        void testMultipleUntypedParams() {
            assertTypeError("fn f(a, b, c) { return a; } f(1, 2, 3);",
                    "must have a type annotation");
        }
    }

    // ================================================================
    // 13. Undefined variables
    // ================================================================

    @Nested
    @DisplayName("Undefined variables")
    class UndefinedVariables {

        @Test
        @DisplayName("Bare assignment to undefined variable is rejected")
        void testBareAssignment() {
            assertTypeError("x = 10; x;", "Undefined variable");
        }

        @Test
        @DisplayName("Reading undefined variable is rejected")
        void testReadUndefined() {
            assertTypeError("let y = 1; x;", "Undefined variable");
        }

        @Test
        @DisplayName("Undefined variable inside function is rejected")
        void testUndefinedInFunction() {
            assertTypeError("fn f(): Int { return x; } f();", "Undefined variable");
        }

        @Test
        @DisplayName("Reference to undefined parameter is rejected")
        void testUndefinedParameter() {
            assertTypeError("fn f(a: Int): Int { return b; } f(1);", "Undefined variable");
        }
    }

    // ================================================================
    // 14. Return type inference errors
    // ================================================================

    @Nested
    @DisplayName("Return type inference errors")
    class ReturnTypeInferenceErrors {

        @Test
        @DisplayName("Self-recursive function without return type is rejected")
        void testSelfRecursiveNoReturnType() {
            assertTypeError(
                    "fn fac(n: Int) { if n < 2 { 1 } else { n * fac(n - 1) } } fac(5);",
                    "Cannot infer return type for recursive function");
        }

        @Test
        @DisplayName("Return type inferred correctly for expression body")
        void testInferredExprBody() {
            assertEval("fn double(x: Int) = x * 2; double(5);", 10);
        }

        @Test
        @DisplayName("Block body inferred correctly")
        void testInferredBlockBody() {
            assertEval("fn negate(b: Bool) { not b } negate(true);", false);
        }

        @Test
        @DisplayName("Return statement vs declared type mismatch")
        void testReturnStatementMismatch() {
            var src = """
                fn foo(): Int {
                    return "hello";
                }
                foo();
            """;
            assertTypeError(src, "Return type mismatch");
        }

        @Test
        @DisplayName("Expression body vs declared type mismatch")
        void testExprBodyMismatch() {
            assertTypeError("fn foo(): Bool = 42; foo();", "Return type mismatch");
        }

        @Test
        @DisplayName("Inferred return type used in caller")
        void testInferredTypeUsedByCaller() {
            var src = """
                fn getNum(x: Int) = x + 1;
                fn check(x: Int): Bool = getNum(x) > 5;
                check(10);
            """;
            assertEval(src, true);
        }

        @Test
        @DisplayName("Multiple return paths must have same type")
        void testMultipleReturnPathsMismatch() {
            var src = """
                fn f(x: Int): Int {
                    if x > 0 {
                        return "positive";
                    } else {
                        return -1;
                    }
                }
                f(5);
            """;
            assertTypeError(src, "Return type mismatch");
        }
    }

    // ================================================================
    // 15. Variable reassignment errors
    // ================================================================

    @Nested
    @DisplayName("Variable reassignment errors")
    class VariableReassignmentErrors {

        @Test
        @DisplayName("Cannot reassign Bool variable to Int")
        void testReassignBoolToInt() {
            var src = """
                fn f(): Int {
                    let x = true;
                    x = 42;
                    return x;
                }
                f();
            """;
            assertTypeError(src, "Cannot assign Int to variable of type Bool");
        }

        @Test
        @DisplayName("Cannot reassign none variable to Int")
        void testReassignNoneToInt() {
            assertTypeError("let x = none; x = 42; x;", "Cannot assign Int to variable of type None");
        }

        @Test
        @DisplayName("Cannot reassign String variable to Int")
        void testReassignStringToInt() {
            assertTypeError("let x = \"hello\"; x = 42; x;", "Cannot assign Int to variable of type String");
        }

        @Test
        @DisplayName("Cannot reassign Int variable to String in function context")
        void testReassignIntToStringInFunction() {
            var src = """
                fn f(): String {
                    let x = 1;
                    x = "hello";
                    return x;
                }
                f();
            """;
            assertTypeError(src, "Cannot assign String");
        }
    }

    // ================================================================
    // Helpers
    // ================================================================

    private void assertTypeError(String source, String expectedSubstring) {
        var ex = assertThrows(PolyglotException.class, () -> context.eval("relang", source));
        assertTrue(ex.getMessage().contains("TypeError"),
                "Expected TypeError in message but got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(expectedSubstring),
                "Expected '" + expectedSubstring + "' in message but got: " + ex.getMessage());
    }

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
}

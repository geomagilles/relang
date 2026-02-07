package com.relang.parser;

import com.relang.diagnostics.DiagnosticCode;
import com.relang.diagnostics.DiagnosticCodes;

/**
 * Centralized builders for type-checker diagnostics.
 */
final class TypeDiagnostics {

    private TypeDiagnostics() {
        // Utility class
    }

    static Spec missingTypeAnnotation(String parameterName) {
        return new Spec(
                DiagnosticCodes.TYPE_MISSING_TYPE_ANNOTATION,
                "Parameter '" + parameterName + "' must have a type annotation",
                "Add an explicit type, for example `" + parameterName + ": Int`."
        );
    }

    static Spec returnTypeMismatch(ReLangType expected, ReLangType actual) {
        return new Spec(
                DiagnosticCodes.TYPE_MISMATCH,
                "Return type mismatch: expected " + expected.displayName() + " but got " + actual.displayName(),
                "Update the return expression or change the function return type annotation."
        );
    }

    static Spec invalidNegationOperand(ReLangType operand) {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_OPERATOR,
                "Cannot negate type " + operand.displayName() + ", expected Int or Float",
                "Use `-` only on Int or Float expressions."
        );
    }

    static Spec invalidNotOperand(ReLangType operand) {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_OPERATOR,
                "Operator 'not' requires Bool, got " + operand.displayName(),
                "Convert the expression to Bool before using `not`."
        );
    }

    static Spec unknownOperator(String operator) {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_OPERATOR,
                "Unknown operator: " + operator,
                "Use a valid ReLang operator."
        );
    }

    static Spec invalidBinaryOperator(String operator, ReLangType left, ReLangType right, String help) {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_OPERATOR,
                "Operator '" + operator + "' cannot be applied to " + left.displayName() + " and " + right.displayName(),
                help
        );
    }

    static Spec invalidModuloOperands(ReLangType left, ReLangType right) {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_OPERATOR,
                "Operator '%' can only be applied to Int, got " + left.displayName() + " and " + right.displayName(),
                "Convert both operands to Int before using `%`."
        );
    }

    static Spec logicalOperandMustBeBool(String operator, ReLangType actual) {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_OPERATOR,
                "Operator '" + operator + "' requires Bool operands, got " + actual.displayName(),
                "Convert both operands to Bool before using `" + operator + "`."
        );
    }

    static Spec undefinedVariable(String name) {
        return new Spec(
                DiagnosticCodes.TYPE_UNDEFINED_VARIABLE,
                "Undefined variable '" + name + "'",
                "Declare it first with `let " + name + " = ...`."
        );
    }

    static Spec outOfScopeVariable(String name) {
        return new Spec(
                DiagnosticCodes.TYPE_OUT_OF_SCOPE_VARIABLE,
                "Variable '" + name + "' is out of scope",
                "Move the declaration to an outer scope or use the variable inside its declaring block."
        );
    }

    static Spec unknownFunction(String functionName) {
        return new Spec(
                DiagnosticCodes.TYPE_UNKNOWN_FUNCTION,
                "Unknown function: " + functionName,
                "Check the function name spelling or declare `" + functionName + "` before calling it."
        );
    }

    static Spec unknownNamedArgument(String functionName, String argumentName, String expectedSignature) {
        return new Spec(
                DiagnosticCodes.TYPE_UNKNOWN_NAMED_ARGUMENT,
                "Unknown named argument '" + argumentName + "' for function '" + functionName + "'",
                "Use one of the declared parameter names. Expected signature: " + expectedSignature
        );
    }

    static Spec duplicateNamedArgument(String functionName, String argumentName, String expectedSignature) {
        return new Spec(
                DiagnosticCodes.TYPE_UNKNOWN_NAMED_ARGUMENT,
                "Argument '" + argumentName + "' is provided more than once in call to '" + functionName + "'",
                "Pass each parameter only once. Expected signature: " + expectedSignature
        );
    }

    static Spec positionalAfterNamedArgument(String functionName, String expectedSignature) {
        return new Spec(
                DiagnosticCodes.TYPE_UNKNOWN_NAMED_ARGUMENT,
                "Positional argument cannot appear after named arguments in call to '" + functionName + "'",
                "Move positional arguments before named arguments. Expected signature: " + expectedSignature
        );
    }

    static Spec arityAtLeast(String functionName, int required, int actual, String expectedSignature) {
        return new Spec(
                DiagnosticCodes.TYPE_ARITY_MISMATCH,
                "Function '" + functionName + "' requires at least " + required + " arguments, got " + actual,
                "Add the missing arguments to the call. Expected signature: " + expectedSignature
        );
    }

    static Spec arityAtMost(String functionName, int maximum, int actual, String expectedSignature) {
        return new Spec(
                DiagnosticCodes.TYPE_ARITY_MISMATCH,
                "Function '" + functionName + "' accepts at most " + maximum + " arguments, got " + actual,
                "Remove extra arguments or update the function signature. Expected signature: " + expectedSignature
        );
    }

    static Spec argumentTypeMismatch(String functionName, int oneBasedIndex, ReLangType expected, ReLangType actual) {
        return new Spec(
                DiagnosticCodes.TYPE_MISMATCH,
                "Argument " + oneBasedIndex + " of '" + functionName + "': expected "
                        + expected.displayName() + " but got " + actual.displayName(),
                "Update argument " + oneBasedIndex + " to type " + expected.displayName() + "."
        );
    }

    static Spec recursiveReturnTypeInference(String functionName) {
        return new Spec(
                DiagnosticCodes.TYPE_RETURN_TYPE_INFERENCE,
                "Cannot infer return type for recursive function '" + functionName + "'; add explicit return type annotation",
                "Add an explicit return type to `fn " + functionName + "(...)`."
        );
    }

    static Spec invalidAwaitOperand(ReLangType actual) {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_AWAIT,
                "Cannot await non-awaitable type " + actual.displayName(),
                "Use `await` only on values of type Awaitable<T>."
        );
    }

    static Spec unknownFieldInType(String fieldName, String typeName) {
        return new Spec(
                DiagnosticCodes.TYPE_UNKNOWN_FIELD,
                "No field '" + fieldName + "' in type " + typeName,
                "Check field name spelling or update type `" + typeName + "`."
        );
    }

    static Spec unknownFieldOnType(String fieldName, ReLangType receiverType) {
        return new Spec(
                DiagnosticCodes.TYPE_UNKNOWN_FIELD,
                "Cannot access field '" + fieldName + "' on type " + receiverType.displayName(),
                "Use field access only on record values with the requested field."
        );
    }

    static Spec unknownType(String typeName) {
        return new Spec(
                DiagnosticCodes.TYPE_UNKNOWN_TYPE,
                "Unknown type: " + typeName,
                "Declare `type " + typeName + " { ... }` before using it."
        );
    }

    static Spec unknownFieldInConstruction(String fieldName, String typeName) {
        return new Spec(
                DiagnosticCodes.TYPE_UNKNOWN_FIELD,
                "Unknown field '" + fieldName + "' in type " + typeName,
                "Remove `" + fieldName + "` or add it to type `" + typeName + "`."
        );
    }

    static Spec fieldTypeMismatch(String fieldName, ReLangType expected, ReLangType actual) {
        return new Spec(
                DiagnosticCodes.TYPE_MISMATCH,
                "Field '" + fieldName + "': expected " + expected.displayName() + " but got " + actual.displayName(),
                "Update `" + fieldName + "` to type " + expected.displayName() + "."
        );
    }

    static Spec missingField(String fieldName, String typeName) {
        return new Spec(
                DiagnosticCodes.TYPE_MISSING_FIELD,
                "Missing field '" + fieldName + "' in " + typeName + " construction",
                "Provide `" + fieldName + ": ...` in the constructor."
        );
    }

    static Spec declaredAssignmentMismatch(ReLangType declared, ReLangType actual) {
        return new Spec(
                DiagnosticCodes.TYPE_MISMATCH,
                "Cannot assign " + actual.displayName() + " to variable of declared type " + declared.displayName(),
                "Use a value compatible with " + declared.displayName() + "."
        );
    }

    static Spec immutableParameterReassignment(String name) {
        return new Spec(
                DiagnosticCodes.TYPE_IMMUTABLE_PARAMETER,
                "Cannot reassign parameter '" + name + "'; parameters are immutable. Use 'let " + name + " = ...' to shadow instead",
                "Introduce a new local with `let " + name + " = ...`."
        );
    }

    static Spec assignmentMismatch(ReLangType expected, ReLangType actual) {
        return new Spec(
                DiagnosticCodes.TYPE_MISMATCH,
                "Cannot assign " + actual.displayName() + " to variable of type " + expected.displayName(),
                "Use a value compatible with " + expected.displayName() + "."
        );
    }

    static Spec invalidCondition(String keyword, ReLangType conditionType) {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_CONDITION,
                "Condition must be Bool, got " + conditionType.displayName(),
                "Change the `" + keyword + "` condition to a Bool expression."
        );
    }

    static Spec breakOutsideLoop() {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_CONTROL_FLOW,
                "`break` can only be used inside a loop",
                "Use `break` inside `while` or `for`, or remove it."
        );
    }

    static Spec continueOutsideLoop() {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_CONTROL_FLOW,
                "`continue` can only be used inside a loop",
                "Use `continue` inside `while` or `for`, or remove it."
        );
    }

    static Spec invalidRangeBound(String label, ReLangType boundType) {
        return new Spec(
                DiagnosticCodes.TYPE_MISMATCH,
                label + " must be Int, got " + boundType.displayName(),
                "Use Int values for range bounds."
        );
    }

    static Spec invalidSubjectlessMatchPattern(ReLangType patternType) {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_MATCH_PATTERN,
                "Subjectless match pattern must be Bool, got " + patternType.displayName(),
                "Use a Bool expression in each pattern arm."
        );
    }

    static Spec unclosedInterpolation() {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_INTERPOLATION,
                "Unclosed interpolation in string",
                "Close interpolation with `}`."
        );
    }

    static Spec invalidInterpolationExpression(String expressionText) {
        return new Spec(
                DiagnosticCodes.TYPE_INVALID_INTERPOLATION,
                "Invalid expression in string interpolation: " + expressionText,
                "Use a valid ReLang expression inside `${...}`."
        );
    }

    record Spec(DiagnosticCode code, String message, String help) {}
}

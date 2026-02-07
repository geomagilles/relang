package com.relang.nodes;

import com.relang.diagnostics.DiagnosticCode;
import com.relang.diagnostics.DiagnosticCodes;

/**
 * Centralized builders for runtime diagnostics.
 */
final class RuntimeDiagnostics {

    private RuntimeDiagnostics() {
        // Utility class
    }

    static Spec unknownFunction(String functionName) {
        return new Spec(
                DiagnosticCodes.RUNTIME_UNKNOWN_FUNCTION,
                "Function not found at runtime: '" + functionName + "'.",
                "Check the function name and ensure it is declared before use."
        );
    }

    static Spec unknownNamedArgument(String functionName, String argumentName) {
        return new Spec(
                DiagnosticCodes.RUNTIME_INVALID_ARGUMENT,
                "Unknown parameter name '" + argumentName + "' in function '" + functionName + "'.",
                "Use one of the function's declared parameter names."
        );
    }

    static Spec invalidAwaitOperand(Object value) {
        return new Spec(
                DiagnosticCodes.RUNTIME_INVALID_OPERATION,
                "await requires an Awaitable value, got " + typeName(value) + ".",
                "Wrap a value with `resolved(...)` or use an existing Awaitable."
        );
    }

    static Spec invalidFieldAccess(String fieldName, Object receiver) {
        return new Spec(
                DiagnosticCodes.RUNTIME_INVALID_OPERATION,
                "Cannot access field '" + fieldName + "' on value of type " + typeName(receiver) + ".",
                "Use field access only on records that define this field."
        );
    }

    static Spec missingRecordField(String fieldName, String typeName) {
        return new Spec(
                DiagnosticCodes.RUNTIME_INVALID_OPERATION,
                "No field '" + fieldName + "' in type '" + typeName + "'.",
                "Check field names against the record type declaration."
        );
    }

    static Spec rangeBoundMustBeInt(String boundName, Object value) {
        return new Spec(
                DiagnosticCodes.RUNTIME_INVALID_OPERATION,
                "Range " + boundName + " must be Int, got " + typeName(value) + ".",
                "Convert the bound to Int before iterating."
        );
    }

    static Spec logicalOperandMustBeBool(String operator, Object value) {
        return new Spec(
                DiagnosticCodes.RUNTIME_INVALID_OPERATION,
                "Operand of '" + operator + "' must be Bool, got " + typeName(value) + ".",
                "Use Bool expressions on both sides of `" + operator + "`."
        );
    }

    static Spec conditionMustBeBool(Object value) {
        return new Spec(
                DiagnosticCodes.RUNTIME_INVALID_CONDITION,
                "Condition must be Bool, got " + typeName(value) + ".",
                "Use a Bool expression in this condition."
        );
    }

    static Spec nonExhaustiveMatchOnSubject(Object subject) {
        return new Spec(
                DiagnosticCodes.RUNTIME_NON_EXHAUSTIVE_MATCH,
                "Non-exhaustive match: no arm matched value " + String.valueOf(subject) + ".",
                "Add a matching arm or a wildcard `_ -> ...` arm."
        );
    }

    static Spec nonExhaustiveSubjectlessMatch() {
        return new Spec(
                DiagnosticCodes.RUNTIME_NON_EXHAUSTIVE_MATCH,
                "Non-exhaustive match: no condition was true.",
                "Add a fallback arm `_ -> ...`."
        );
    }

    static Spec invalidSubjectlessMatchCondition(Object value) {
        return new Spec(
                DiagnosticCodes.RUNTIME_INVALID_CONDITION,
                "Subjectless match arm condition must be Bool, got " + typeName(value) + ".",
                "Return Bool from each subjectless match condition."
        );
    }

    static Spec resolvedArity(int actualArgumentCount) {
        return new Spec(
                DiagnosticCodes.RUNTIME_INVALID_ARGUMENT,
                "resolved() requires exactly 1 argument, got " + actualArgumentCount + ".",
                "Call `resolved(value)` with a single value."
        );
    }

    static Spec productIndexOutOfBounds(int index, int size) {
        return new Spec(
                DiagnosticCodes.RUNTIME_INVALID_OPERATION,
                "Product index " + index + " out of bounds (size: " + size + ").",
                "Use an index between 0 and " + Math.max(0, size - 1) + "."
        );
    }

    private static String typeName(Object value) {
        if (value == null || value instanceof ReLangNone) {
            return "None";
        }
        return switch (value) {
            case Long ignored -> "Int";
            case Double ignored -> "Float";
            case Boolean ignored -> "Bool";
            case String ignored -> "String";
            case ReLangRecord ignored -> "Record";
            case ReLangProduct ignored -> "Product";
            case AwaitableHandle ignored -> "Awaitable";
            default -> value.getClass().getSimpleName();
        };
    }

    record Spec(DiagnosticCode code, String message, String help) {}
}

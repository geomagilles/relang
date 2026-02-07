package com.relang.diagnostics;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central catalog of diagnostic codes.
 */
public final class DiagnosticCodes {

    // RL1xxx: syntax and parse diagnostics
    public static final DiagnosticCode SYNTAX_GENERIC = new DiagnosticCode("RL1000", DiagnosticCategory.SYNTAX, "Syntax error");
    public static final DiagnosticCode SYNTAX_UNEXPECTED_TOKEN = new DiagnosticCode("RL1001", DiagnosticCategory.SYNTAX, "Unexpected token");
    public static final DiagnosticCode SYNTAX_MISSING_TOKEN = new DiagnosticCode("RL1002", DiagnosticCategory.SYNTAX, "Missing token");

    // RL2xxx: static type diagnostics
    public static final DiagnosticCode TYPE_GENERIC = new DiagnosticCode("RL2000", DiagnosticCategory.TYPE, "Type error");
    public static final DiagnosticCode TYPE_MISMATCH = new DiagnosticCode("RL2001", DiagnosticCategory.TYPE, "Type mismatch");
    public static final DiagnosticCode TYPE_UNDEFINED_VARIABLE = new DiagnosticCode("RL2002", DiagnosticCategory.TYPE, "Undefined variable");
    public static final DiagnosticCode TYPE_ARITY_MISMATCH = new DiagnosticCode("RL2003", DiagnosticCategory.TYPE, "Function arity mismatch");
    public static final DiagnosticCode TYPE_UNKNOWN_FUNCTION = new DiagnosticCode("RL2004", DiagnosticCategory.TYPE, "Unknown function");
    public static final DiagnosticCode TYPE_MISSING_TYPE_ANNOTATION = new DiagnosticCode("RL2005", DiagnosticCategory.TYPE, "Missing type annotation");
    public static final DiagnosticCode TYPE_INVALID_OPERATOR = new DiagnosticCode("RL2006", DiagnosticCategory.TYPE, "Invalid operator usage");
    public static final DiagnosticCode TYPE_RETURN_TYPE_INFERENCE = new DiagnosticCode("RL2007", DiagnosticCategory.TYPE, "Return type inference failed");
    public static final DiagnosticCode TYPE_INVALID_AWAIT = new DiagnosticCode("RL2008", DiagnosticCategory.TYPE, "Invalid await operand");
    public static final DiagnosticCode TYPE_UNKNOWN_TYPE = new DiagnosticCode("RL2009", DiagnosticCategory.TYPE, "Unknown type");
    public static final DiagnosticCode TYPE_UNKNOWN_FIELD = new DiagnosticCode("RL2010", DiagnosticCategory.TYPE, "Unknown field");
    public static final DiagnosticCode TYPE_MISSING_FIELD = new DiagnosticCode("RL2011", DiagnosticCategory.TYPE, "Missing required field");
    public static final DiagnosticCode TYPE_IMMUTABLE_PARAMETER = new DiagnosticCode("RL2012", DiagnosticCategory.TYPE, "Immutable parameter reassignment");
    public static final DiagnosticCode TYPE_INVALID_CONDITION = new DiagnosticCode("RL2013", DiagnosticCategory.TYPE, "Invalid condition type");
    public static final DiagnosticCode TYPE_INVALID_INTERPOLATION = new DiagnosticCode("RL2014", DiagnosticCategory.TYPE, "Invalid string interpolation");
    public static final DiagnosticCode TYPE_INVALID_MATCH_PATTERN = new DiagnosticCode("RL2015", DiagnosticCategory.TYPE, "Invalid match pattern");

    // RL3xxx: runtime diagnostics
    public static final DiagnosticCode RUNTIME_GENERIC = new DiagnosticCode("RL3000", DiagnosticCategory.RUNTIME, "Runtime error");
    public static final DiagnosticCode RUNTIME_INVALID_OPERATION = new DiagnosticCode("RL3001", DiagnosticCategory.RUNTIME, "Invalid runtime operation");

    // RL9xxx: internal/tooling diagnostics
    public static final DiagnosticCode INTERNAL_GENERIC = new DiagnosticCode("RL9000", DiagnosticCategory.INTERNAL, "Internal tooling error");

    private static final Map<String, DiagnosticCode> CATALOG = catalog(
            SYNTAX_GENERIC,
            SYNTAX_UNEXPECTED_TOKEN,
            SYNTAX_MISSING_TOKEN,
            TYPE_GENERIC,
            TYPE_MISMATCH,
            TYPE_UNDEFINED_VARIABLE,
            TYPE_ARITY_MISMATCH,
            TYPE_UNKNOWN_FUNCTION,
            TYPE_MISSING_TYPE_ANNOTATION,
            TYPE_INVALID_OPERATOR,
            TYPE_RETURN_TYPE_INFERENCE,
            TYPE_INVALID_AWAIT,
            TYPE_UNKNOWN_TYPE,
            TYPE_UNKNOWN_FIELD,
            TYPE_MISSING_FIELD,
            TYPE_IMMUTABLE_PARAMETER,
            TYPE_INVALID_CONDITION,
            TYPE_INVALID_INTERPOLATION,
            TYPE_INVALID_MATCH_PATTERN,
            RUNTIME_GENERIC,
            RUNTIME_INVALID_OPERATION,
            INTERNAL_GENERIC
    );

    private DiagnosticCodes() {
        // Utility class
    }

    public static Collection<DiagnosticCode> all() {
        return CATALOG.values();
    }

    public static Optional<DiagnosticCode> find(String value) {
        return Optional.ofNullable(CATALOG.get(value));
    }

    public static DiagnosticCode require(String value) {
        var code = CATALOG.get(value);
        if (code == null) {
            throw new IllegalArgumentException("Unknown diagnostic code: " + value);
        }
        return code;
    }

    public static DiagnosticCode fallbackFor(DiagnosticCategory category) {
        return switch (category) {
            case SYNTAX -> SYNTAX_GENERIC;
            case TYPE -> TYPE_GENERIC;
            case RUNTIME -> RUNTIME_GENERIC;
            case INTERNAL -> INTERNAL_GENERIC;
        };
    }

    private static Map<String, DiagnosticCode> catalog(DiagnosticCode... codes) {
        var byCode = new LinkedHashMap<String, DiagnosticCode>();
        for (var code : codes) {
            var previous = byCode.putIfAbsent(code.value(), code);
            if (previous != null) {
                throw new IllegalStateException("Duplicate diagnostic code: " + code.value());
            }
        }
        return Map.copyOf(byCode);
    }
}

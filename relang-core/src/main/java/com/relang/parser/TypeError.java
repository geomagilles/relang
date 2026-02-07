package com.relang.parser;

import com.relang.diagnostics.DiagnosticCode;
import com.relang.diagnostics.DiagnosticCodes;
import com.relang.diagnostics.ReLangDiagnostic;
import com.relang.diagnostics.SourceRange;

import java.util.Objects;

/**
 * A single type error found during static analysis.
 */
public record TypeError(String code, int line, int column, String message, String sourceSnippet, String help) {

    public TypeError {
        code = Objects.requireNonNullElse(code, DiagnosticCodes.TYPE_GENERIC.value());
        message = Objects.requireNonNull(message, "message");
    }

    public TypeError(int line, int column, String message, String sourceSnippet) {
        this(DiagnosticCodes.TYPE_GENERIC.value(), line, column, message, sourceSnippet, null);
    }

    public TypeError(DiagnosticCode code, int line, int column, String message, String sourceSnippet) {
        this(code.value(), line, column, message, sourceSnippet, null);
    }

    public TypeError(DiagnosticCode code, int line, int column, String message, String sourceSnippet, String help) {
        this(code.value(), line, column, message, sourceSnippet, help);
    }

    public ReLangDiagnostic toDiagnostic() {
        var diagnosticCode = DiagnosticCodes.find(code).orElse(DiagnosticCodes.TYPE_GENERIC);
        var diagnostic = ReLangDiagnostic.error(diagnosticCode, message, SourceRange.fromSnippet(line, column, sourceSnippet));
        if (help != null && !help.isBlank()) {
            return diagnostic.withHelp(help);
        }
        return diagnostic;
    }

    public String format() {
        return "TypeError[" + code + "] at line " + line + ", column " + column + ": " + message;
    }
}

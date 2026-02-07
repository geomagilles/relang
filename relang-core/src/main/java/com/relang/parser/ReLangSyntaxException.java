package com.relang.parser;

import com.relang.diagnostics.ReLangDiagnostic;
import com.relang.diagnostics.CliDiagnosticRenderer;

import java.util.List;

/**
 * Thrown when parsing finds one or more syntax errors.
 */
public final class ReLangSyntaxException extends RuntimeException {

    private final List<SyntaxError> errors;

    public ReLangSyntaxException(List<SyntaxError> errors) {
        this(errors, null, null);
    }

    public ReLangSyntaxException(List<SyntaxError> errors, String sourceName, String sourceText) {
        super("SyntaxError\n" + CliDiagnosticRenderer.render(
                errors.stream().map(SyntaxError::toDiagnostic).toList(),
                sourceName,
                sourceText
        ));
        this.errors = List.copyOf(errors);
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    public List<ReLangDiagnostic> getDiagnostics() {
        return errors.stream().map(SyntaxError::toDiagnostic).toList();
    }
}

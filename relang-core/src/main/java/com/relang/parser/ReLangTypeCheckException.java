package com.relang.parser;

import com.relang.diagnostics.ReLangDiagnostic;
import com.relang.diagnostics.CliDiagnosticRenderer;

import java.util.List;

/**
 * Thrown when the type checker finds one or more errors.
 */
public class ReLangTypeCheckException extends RuntimeException {

    private final List<TypeError> errors;

    public ReLangTypeCheckException(List<TypeError> errors) {
        this(errors, null, null);
    }

    public ReLangTypeCheckException(List<TypeError> errors, String sourceName, String sourceText) {
        super("TypeError\n" + CliDiagnosticRenderer.render(
                errors.stream().map(TypeError::toDiagnostic).toList(),
                sourceName,
                sourceText
        ));
        this.errors = List.copyOf(errors);
    }

    public List<TypeError> getErrors() {
        return errors;
    }

    public List<ReLangDiagnostic> getDiagnostics() {
        return errors.stream().map(TypeError::toDiagnostic).toList();
    }
}

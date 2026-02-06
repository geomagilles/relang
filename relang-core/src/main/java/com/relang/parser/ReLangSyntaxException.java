package com.relang.parser;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when parsing finds one or more syntax errors.
 */
public final class ReLangSyntaxException extends RuntimeException {

    private final List<SyntaxError> errors;

    public ReLangSyntaxException(List<SyntaxError> errors) {
        super(errors.stream().map(SyntaxError::format).collect(Collectors.joining("\n")));
        this.errors = List.copyOf(errors);
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }
}

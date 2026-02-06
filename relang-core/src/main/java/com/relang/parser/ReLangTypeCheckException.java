package com.relang.parser;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when the type checker finds one or more errors.
 */
public class ReLangTypeCheckException extends RuntimeException {

    private final List<TypeError> errors;

    public ReLangTypeCheckException(List<TypeError> errors) {
        super(errors.stream().map(TypeError::format).collect(Collectors.joining("\n")));
        this.errors = List.copyOf(errors);
    }

    public List<TypeError> getErrors() {
        return errors;
    }
}

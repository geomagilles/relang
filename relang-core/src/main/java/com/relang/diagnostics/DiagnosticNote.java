package com.relang.diagnostics;

import java.util.Objects;

/**
 * Extra context attached to a diagnostic.
 */
public record DiagnosticNote(String message, SourceRange range) {

    public DiagnosticNote {
        message = Objects.requireNonNull(message, "message");
    }

    public DiagnosticNote(String message) {
        this(message, null);
    }
}

package com.relang.diagnostics;

import java.util.Objects;

/**
 * Replacement edit used by diagnostic fixes.
 */
public record DiagnosticTextEdit(SourceRange range, String newText) {

    public DiagnosticTextEdit {
        range = Objects.requireNonNull(range, "range");
        newText = Objects.requireNonNull(newText, "newText");
    }
}

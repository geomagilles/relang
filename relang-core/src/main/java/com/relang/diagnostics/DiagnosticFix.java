package com.relang.diagnostics;

import java.util.List;
import java.util.Objects;

/**
 * Candidate fix for a diagnostic.
 */
public record DiagnosticFix(String title, List<DiagnosticTextEdit> edits) {

    public DiagnosticFix {
        title = Objects.requireNonNull(title, "title");
        edits = List.copyOf(Objects.requireNonNull(edits, "edits"));
    }
}

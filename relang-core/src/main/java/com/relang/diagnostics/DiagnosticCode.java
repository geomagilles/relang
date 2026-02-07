package com.relang.diagnostics;

import java.util.Objects;

/**
 * Stable machine-readable diagnostic identifier.
 */
public record DiagnosticCode(String value, DiagnosticCategory category, String title) {

    public DiagnosticCode {
        value = Objects.requireNonNull(value, "value");
        category = Objects.requireNonNull(category, "category");
        title = Objects.requireNonNull(title, "title");
        if (!value.matches("RL\\d{4}")) {
            throw new IllegalArgumentException("Diagnostic code must match RLdddd: " + value);
        }
    }
}

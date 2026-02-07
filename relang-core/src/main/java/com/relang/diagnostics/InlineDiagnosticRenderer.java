package com.relang.diagnostics;

import java.util.Objects;

/**
 * Renders a compact text representation suitable for editor/transport messages.
 */
public final class InlineDiagnosticRenderer {

    private InlineDiagnosticRenderer() {
        // Utility class
    }

    public static String render(ReLangDiagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic");
        var base = "[" + diagnostic.code().value() + "] " + diagnostic.message();
        if (diagnostic.help() != null && !diagnostic.help().isBlank()) {
            return base + "\nhelp: " + diagnostic.help();
        }
        return base;
    }
}

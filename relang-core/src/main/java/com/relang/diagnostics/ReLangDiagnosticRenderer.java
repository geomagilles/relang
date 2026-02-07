package com.relang.diagnostics;

import java.util.List;

/**
 * @deprecated Use {@link CliDiagnosticRenderer} and {@link InlineDiagnosticRenderer}.
 */
@Deprecated(forRemoval = true)
public final class ReLangDiagnosticRenderer {

    private ReLangDiagnosticRenderer() {
        // Utility class
    }

    public static String renderForCli(List<ReLangDiagnostic> diagnostics, String sourceName, String sourceText) {
        return CliDiagnosticRenderer.render(diagnostics, sourceName, sourceText);
    }

    public static String renderForLsp(ReLangDiagnostic diagnostic) {
        return InlineDiagnosticRenderer.render(diagnostic);
    }

    public static String renderSingleForCli(ReLangDiagnostic diagnostic, String sourceName, String sourceText) {
        return CliDiagnosticRenderer.renderSingle(diagnostic, sourceName, sourceText);
    }
}

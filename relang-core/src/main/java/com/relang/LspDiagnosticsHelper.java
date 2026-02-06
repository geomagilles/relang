package com.relang;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Helper class for LSP diagnostics integration.
 * This class is only loaded when the LSP tool is available at runtime.
 * Separated from ReLang.java to avoid NoClassDefFoundError during class loading in CLI mode.
 */
final class LspDiagnosticsHelper {

    private LspDiagnosticsHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts type errors to an LSP DiagnosticsNotification for real-time editor feedback.
     * Uses the GraalVM LSP tool API (available at runtime when the LSP server is active).
     *
     * Note: This method uses Object parameter type for reflection compatibility.
     */
    @SuppressWarnings("unchecked")
    static org.graalvm.tools.lsp.exceptions.DiagnosticsNotification buildDiagnosticsNotification(
            Object sourceUri, Object typeErrors) {
        URI uri = (URI) sourceUri;
        List<com.relang.parser.TypeError> errors = (List<com.relang.parser.TypeError>) typeErrors;
        var diagnostics = errors.stream()
                .map(error -> org.graalvm.tools.lsp.server.types.Diagnostic.create(
                        org.graalvm.tools.lsp.server.types.Range.create(
                                error.line() - 1,    // LSP lines are 0-indexed
                                error.column(),       // columns already 0-indexed from ANTLR
                                error.line() - 1,
                                error.column() + (error.sourceSnippet() != null ? error.sourceSnippet().length() : 1)
                        ),
                        error.message(),
                        org.graalvm.tools.lsp.server.types.DiagnosticSeverity.Error,
                        null,            // code
                        "relang",        // source identifier shown in editor
                        null             // related information
                ))
                .toList();
        return new org.graalvm.tools.lsp.exceptions.DiagnosticsNotification(
                Map.of(uri, diagnostics));
    }
}

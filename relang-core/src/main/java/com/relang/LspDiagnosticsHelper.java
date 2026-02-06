package com.relang;

/**
 * Lightweight utility for detecting whether parsing runs inside Graal LSP.
 */
final class LspDiagnosticsHelper {

    private LspDiagnosticsHelper() {
        // Utility class
    }

    static boolean isLspRequest() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.anyMatch(
                        f -> f.getDeclaringClass().getPackageName().startsWith("org.graalvm.tools.lsp")
                ));
    }
}

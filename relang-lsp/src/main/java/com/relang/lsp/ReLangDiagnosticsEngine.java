package com.relang.lsp;

import com.oracle.truffle.api.source.Source;
import com.relang.diagnostics.DiagnosticCodes;
import com.relang.diagnostics.ReLangDiagnostic;
import com.relang.diagnostics.SourceRange;
import com.relang.parser.ReLangSyntaxException;
import com.relang.parser.ReLangTruffleParser;
import com.relang.parser.ReLangTypeChecker;
import com.relang.parser.TypeError;

import java.util.List;

/**
 * Computes static diagnostics (syntax + type) for a ReLang document.
 */
final class ReLangDiagnosticsEngine {

    private ReLangDiagnosticsEngine() {
        // Utility class
    }

    static List<ReLangDiagnostic> analyze(String uri, String sourceText) {
        if (sourceText == null) {
            return List.of();
        }

        try {
            var sourceName = uri == null || uri.isBlank() ? "<memory>" : uri;
            var source = Source.newBuilder("relang", sourceText, sourceName).build();

            var tree = ReLangTruffleParser.parseAntlr(source);
            var checker = new ReLangTypeChecker();
            var typeErrors = checker.check(tree);
            return typeErrors.stream().map(TypeError::toDiagnostic).toList();
        } catch (ReLangSyntaxException syntaxException) {
            return syntaxException.getDiagnostics();
        } catch (Exception e) {
            var message = "Internal diagnostic failure: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            var diagnostic = ReLangDiagnostic.error(
                    DiagnosticCodes.INTERNAL_GENERIC,
                    message,
                    SourceRange.point(1, 0)
            ).withHelp("Please report this issue with the failing source snippet.");
            return List.of(diagnostic);
        }
    }
}

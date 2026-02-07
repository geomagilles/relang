package com.relang.diagnostics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Diagnostic Renderers")
class ReLangDiagnosticRendererTest {

    @Test
    @DisplayName("CLI renderer prints code frame and help")
    void renderCli() {
        var diagnostic = ReLangDiagnostic.error(
                        DiagnosticCodes.TYPE_UNDEFINED_VARIABLE,
                        "Undefined variable 'x'",
                        new SourceRange(2, 4, 2, 5)
                )
                .withHelp("Declare it first with `let x = ...`.");

        var source = """
                fn main(): Int {
                    x
                }
                """;
        var rendered = CliDiagnosticRenderer.render(List.of(diagnostic), "sample.re", source);

        assertTrue(rendered.contains("error[RL2002]: Undefined variable 'x'"), rendered);
        assertTrue(rendered.contains(" --> sample.re:2:5"), rendered);
        assertTrue(rendered.contains("2 |     x"), rendered);
        assertTrue(rendered.contains("help: Declare it first with `let x = ...`."), rendered);
    }

    @Test
    @DisplayName("LSP renderer keeps one-line prefix plus help")
    void renderLsp() {
        var diagnostic = ReLangDiagnostic.error(
                        DiagnosticCodes.TYPE_ARITY_MISMATCH,
                        "Function 'add' requires at least 2 arguments, got 1",
                        SourceRange.point(1, 0)
                )
                .withHelp("Add the missing arguments to the call.");

        var rendered = InlineDiagnosticRenderer.render(diagnostic);

        assertEquals(
                """
                [RL2003] Function 'add' requires at least 2 arguments, got 1
                help: Add the missing arguments to the call.""",
                rendered
        );
    }
}

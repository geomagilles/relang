package com.relang.lsp;

import com.relang.diagnostics.DiagnosticCodes;
import com.relang.diagnostics.ReLangDiagnostic;
import com.relang.diagnostics.SourceRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LSP Quick Fix Suggester")
class LspQuickFixSuggesterTest {

    @Test
    @DisplayName("suggests placeholder arguments for too few arguments")
    void arityAddArguments() {
        var diagnostic = ReLangDiagnostic.error(
                DiagnosticCodes.TYPE_ARITY_MISMATCH,
                "Function 'add' requires at least 3 arguments, got 1",
                new SourceRange(1, 0, 1, 10)
        );
        var source = "add(1)";
        var fixes = LspQuickFixSuggester.suggest(source, diagnostic);

        assertFalse(fixes.isEmpty());
        var edit = fixes.getFirst().edits().getFirst();
        assertEquals(", /* TODO */, /* TODO */", edit.newText());
    }

    @Test
    @DisplayName("suggests trimming extra arguments")
    void arityRemoveArguments() {
        var diagnostic = ReLangDiagnostic.error(
                DiagnosticCodes.TYPE_ARITY_MISMATCH,
                "Function 'add' accepts at most 2 arguments, got 4",
                new SourceRange(1, 0, 1, 20)
        );
        var source = "add(1, 2, 3, 4)";
        var fixes = LspQuickFixSuggester.suggest(source, diagnostic);

        assertFalse(fixes.isEmpty());
        var edit = fixes.getFirst().edits().getFirst();
        assertEquals("1, 2", edit.newText());
    }

    @Test
    @DisplayName("suggests missing field insertion")
    void missingField() {
        var diagnostic = ReLangDiagnostic.error(
                DiagnosticCodes.TYPE_MISSING_FIELD,
                "Missing field 'age' in User construction",
                new SourceRange(1, 0, 1, 20)
        );
        var source = "User{name: \"bob\"}";
        var fixes = LspQuickFixSuggester.suggest(source, diagnostic);

        assertFalse(fixes.isEmpty());
        var edit = fixes.getFirst().edits().getFirst();
        assertTrue(edit.newText().contains("age: /* TODO */"));
    }

    @Test
    @DisplayName("suggests function rename for unknown function typos")
    void unknownFunctionRename() {
        var diagnostic = ReLangDiagnostic.error(
                DiagnosticCodes.TYPE_UNKNOWN_FUNCTION,
                "Unknown function: rezolved",
                new SourceRange(2, 0, 2, 11)
        );
        var source = """
                fn main(): Int {
                    rezolved(1);
                }
                """;

        var fixes = LspQuickFixSuggester.suggest(source, diagnostic);

        assertFalse(fixes.isEmpty());
        assertTrue(fixes.getFirst().title().contains("resolved"));
        var edit = fixes.getFirst().edits().getFirst();
        assertEquals("resolved", edit.newText());
    }
}

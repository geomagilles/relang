package com.relang.diagnostics;

import java.util.List;
import java.util.Objects;

/**
 * Structured diagnostic payload shared across parser, type checker, CLI, and LSP.
 */
public record ReLangDiagnostic(
        DiagnosticCode code,
        DiagnosticSeverity severity,
        String message,
        SourceRange primaryRange,
        List<DiagnosticNote> notes,
        String help,
        List<DiagnosticFix> fixes
) {

    public ReLangDiagnostic {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message");
        primaryRange = Objects.requireNonNull(primaryRange, "primaryRange");
        notes = List.copyOf(Objects.requireNonNull(notes, "notes"));
        fixes = List.copyOf(Objects.requireNonNull(fixes, "fixes"));
    }

    public static ReLangDiagnostic error(DiagnosticCode code, String message, SourceRange range) {
        return new ReLangDiagnostic(code, DiagnosticSeverity.ERROR, message, range, List.of(), null, List.of());
    }

    public ReLangDiagnostic withHelp(String newHelp) {
        return new ReLangDiagnostic(code, severity, message, primaryRange, notes, newHelp, fixes);
    }

    public ReLangDiagnostic withNotes(List<DiagnosticNote> newNotes) {
        return new ReLangDiagnostic(code, severity, message, primaryRange, newNotes, help, fixes);
    }

    public ReLangDiagnostic withFixes(List<DiagnosticFix> newFixes) {
        return new ReLangDiagnostic(code, severity, message, primaryRange, notes, help, newFixes);
    }
}

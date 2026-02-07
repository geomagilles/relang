package com.relang.diagnostics;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Renders diagnostics for CLI and REPL output.
 */
public final class CliDiagnosticRenderer {

    private CliDiagnosticRenderer() {
        // Utility class
    }

    public static String render(List<ReLangDiagnostic> diagnostics, String sourceName, String sourceText) {
        Objects.requireNonNull(diagnostics, "diagnostics");
        if (diagnostics.isEmpty()) {
            return "No diagnostics.";
        }

        var joiner = new StringJoiner("\n\n");
        for (var diagnostic : diagnostics) {
            joiner.add(renderSingle(diagnostic, sourceName, sourceText));
        }
        return joiner.toString();
    }

    public static String renderSingle(ReLangDiagnostic diagnostic, String sourceName, String sourceText) {
        Objects.requireNonNull(diagnostic, "diagnostic");

        var range = diagnostic.primaryRange();
        var location = (sourceName == null || sourceName.isBlank() ? "<source>" : sourceName)
                + ":" + range.startLine() + ":" + (range.startColumn() + 1);
        var title = switch (diagnostic.severity()) {
            case ERROR -> "error";
            case WARNING -> "warning";
            case INFO -> "info";
        };

        var builder = new StringBuilder();
        builder.append(title)
                .append("[")
                .append(diagnostic.code().value())
                .append("]")
                .append(": ")
                .append(diagnostic.message())
                .append('\n');
        builder.append(" --> ").append(location);

        var frame = renderCodeFrame(range, sourceText);
        if (!frame.isEmpty()) {
            builder.append('\n').append(frame);
        }

        for (var note : diagnostic.notes()) {
            builder.append('\n').append("note: ").append(note.message());
        }

        if (diagnostic.help() != null && !diagnostic.help().isBlank()) {
            builder.append('\n').append("help: ").append(diagnostic.help());
        }

        return builder.toString();
    }

    private static String renderCodeFrame(SourceRange range, String sourceText) {
        if (sourceText == null || sourceText.isBlank()) {
            return "";
        }

        var lines = sourceText.split("\\R", -1);
        var lineIndex = range.startLine() - 1;
        if (lineIndex < 0 || lineIndex >= lines.length) {
            return "";
        }

        var lineText = lines[lineIndex];
        var startColumn = Math.min(Math.max(0, range.startColumn()), lineText.length());
        var endColumn = Math.min(Math.max(startColumn + 1, range.endColumn()), lineText.length());
        var pointerLength = Math.max(1, endColumn - startColumn);

        var builder = new StringBuilder();
        builder.append("  |\n");
        builder.append(range.startLine()).append(" | ").append(lineText).append('\n');
        builder.append("  | ");
        builder.append(" ".repeat(startColumn));
        builder.append("^".repeat(pointerLength));
        return builder.toString();
    }
}

package com.relang.diagnostics;

/**
 * Source location range for diagnostics.
 * Line numbers are 1-based. Columns are 0-based (ANTLR/LSP compatible).
 */
public record SourceRange(
        int startLine,
        int startColumn,
        int endLine,
        int endColumn
) {

    public SourceRange {
        if (startLine < 1 || endLine < 1) {
            throw new IllegalArgumentException("Line numbers must be >= 1");
        }
        if (startColumn < 0 || endColumn < 0) {
            throw new IllegalArgumentException("Columns must be >= 0");
        }
        if (endLine < startLine) {
            throw new IllegalArgumentException("endLine must be >= startLine");
        }
        if (endLine == startLine && endColumn < startColumn) {
            throw new IllegalArgumentException("endColumn must be >= startColumn on same line");
        }
    }

    public static SourceRange point(int line, int column) {
        return new SourceRange(line, column, line, column + 1);
    }

    public static SourceRange fromSnippet(int line, int column, String snippet) {
        var length = (snippet == null || snippet.isEmpty()) ? 1 : snippet.length();
        return new SourceRange(line, column, line, column + length);
    }
}

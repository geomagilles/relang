package com.relang.parser;

/**
 * A single syntax error found during parsing.
 */
public record SyntaxError(int line, int column, String message, String sourceSnippet) {

    public String format() {
        return "SyntaxError at line " + line + ", column " + column + ": " + message;
    }
}

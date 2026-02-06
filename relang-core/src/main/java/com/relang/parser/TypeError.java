package com.relang.parser;

/**
 * A single type error found during static analysis.
 */
public record TypeError(int line, int column, String message, String sourceSnippet) {

    public String format() {
        return "TypeError at line " + line + ", column " + column + ": " + message;
    }
}

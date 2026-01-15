package com.relang.nodes;

/**
 * Thrown when attempting to resume execution with a state that was captured
 * from different source code than what is currently being executed.
 * <p>
 * This prevents subtle bugs from mismatched execution paths, variable names,
 * or statement indices that would occur if code changed between suspend and resume.
 */
public class StateCodeMismatchException extends RuntimeException {

    public StateCodeMismatchException(String message) {
        super(message);
    }

    public StateCodeMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}

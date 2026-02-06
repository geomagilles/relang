package com.relang.parser;

import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.Node;

/**
 * Truffle exception used in LSP mode so Graal can surface diagnostics from parse/type failures.
 */
public final class ReLangDiagnosticTruffleException extends AbstractTruffleException {

    public ReLangDiagnosticTruffleException(String message, Node location) {
        super(message, location);
    }
}

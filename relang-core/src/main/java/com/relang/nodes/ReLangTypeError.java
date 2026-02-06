package com.relang.nodes;

import com.oracle.truffle.api.nodes.Node;

public class ReLangTypeError extends RuntimeException {
    public ReLangTypeError(Node node, String message) {
        super(message);
    }
}

package com.relang.nodes;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class ReLangReturnException extends ControlFlowException {

    private final Object result;

    public ReLangReturnException(Object result) {
        this.result = result;
    }

    public Object getResult() {
        return result;
    }
}

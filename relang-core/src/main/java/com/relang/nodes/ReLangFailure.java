package com.relang.nodes;

import com.oracle.truffle.api.interop.TruffleObject;

public final class ReLangFailure implements TruffleObject {
    private final String message;

    public ReLangFailure(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReLangFailure other)) return false;
        return message.equals(other.message);
    }

    @Override
    public int hashCode() { return message.hashCode(); }

    @Override
    public String toString() { return "Failure(\"" + message + "\")"; }
}

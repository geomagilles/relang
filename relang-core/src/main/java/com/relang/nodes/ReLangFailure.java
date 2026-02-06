package com.relang.nodes;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public final class ReLangFailure implements TruffleObject {
    private final String message;

    public ReLangFailure(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }

    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaObject() {
        return ReLangMetaType.FAILURE;
    }

    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return toString();
    }

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

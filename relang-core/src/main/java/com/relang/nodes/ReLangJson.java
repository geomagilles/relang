package com.relang.nodes;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public final class ReLangJson implements TruffleObject {
    private final String jsonText;

    public ReLangJson(String jsonText) {
        this.jsonText = jsonText;
    }

    public String getJsonText() { return jsonText; }

    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaObject() {
        return ReLangMetaType.JSON;
    }

    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReLangJson other)) return false;
        return jsonText.equals(other.jsonText);
    }

    @Override
    public int hashCode() { return jsonText.hashCode(); }

    @Override
    public String toString() { return jsonText; }
}

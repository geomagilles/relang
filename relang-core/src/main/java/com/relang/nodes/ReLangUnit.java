package com.relang.nodes;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * The singleton unit value in ReLang, representing "no meaningful value".
 */
@ExportLibrary(InteropLibrary.class)
public final class ReLangUnit implements TruffleObject {
    public static final ReLangUnit SINGLETON = new ReLangUnit();

    private ReLangUnit() {}

    @ExportMessage
    boolean isNull() {
        return false;
    }

    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaObject() {
        return ReLangMetaType.UNIT;
    }

    @ExportMessage
    Object toDisplayString(boolean allowSideEffects) {
        return "unit";
    }

    @Override
    public String toString() {
        return "unit";
    }
}

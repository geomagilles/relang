package com.relang.nodes;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * The singleton none value in ReLang, representing absence of a value.
 */
@ExportLibrary(InteropLibrary.class)
public final class ReLangNone implements TruffleObject {
    public static final ReLangNone SINGLETON = new ReLangNone();

    private ReLangNone() {}

    @ExportMessage
    boolean isNull() {
        return true;
    }

    @Override
    public String toString() {
        return "none";
    }
}

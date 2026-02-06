package com.relang;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.relang.nodes.ReLangMetaType;

/**
 * Language view wrapper for Java primitives (Long, Double, Boolean, String).
 * Delegates most InteropLibrary messages to the wrapped value but adds
 * hasMetaObject/getMetaObject so the LSP can show type information on hover.
 */
@ExportLibrary(value = InteropLibrary.class, delegateTo = "delegate")
public final class ReLangLanguageView implements TruffleObject {

    final Object delegate;
    private final ReLangMetaType metaType;

    ReLangLanguageView(Object delegate, ReLangMetaType metaType) {
        this.delegate = delegate;
        this.metaType = metaType;
    }

    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaObject() {
        return metaType;
    }

    @ExportMessage
    Object toDisplayString(boolean allowSideEffects,
                           @CachedLibrary("this.delegate") InteropLibrary lib) {
        try {
            return lib.toDisplayString(delegate, allowSideEffects);
        } catch (Exception e) {
            return delegate.toString();
        }
    }
}

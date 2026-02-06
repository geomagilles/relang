package com.relang;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * Represents a variable scope visible at a particular AST position.
 * Used by the Truffle LSP to show variable values on hover and in debugger views.
 */
@ExportLibrary(InteropLibrary.class)
public final class ReLangScope implements TruffleObject {

    private final Frame frame;
    private final FrameDescriptor descriptor;
    private final Node node;

    public ReLangScope(Frame frame, Node node) {
        this.frame = frame;
        this.node = node;
        RootNode root = node.getRootNode();
        this.descriptor = root != null ? root.getFrameDescriptor() : null;
    }

    @ExportMessage
    boolean isScope() {
        return true;
    }

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return new ScopeMembers(descriptor);
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        if (descriptor == null || frame == null) return false;
        for (int i = 0; i < descriptor.getNumberOfSlots(); i++) {
            Object slotName = descriptor.getSlotName(i);
            if (slotName != null && slotName.toString().equals(member)) {
                return frame.getValue(i) != null;
            }
        }
        return false;
    }

    @ExportMessage
    Object readMember(String member) throws UnsupportedMessageException {
        if (descriptor == null || frame == null) throw UnsupportedMessageException.create();
        for (int i = 0; i < descriptor.getNumberOfSlots(); i++) {
            Object slotName = descriptor.getSlotName(i);
            if (slotName != null && slotName.toString().equals(member)) {
                Object value = frame.getValue(i);
                if (value != null) return value;
            }
        }
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    Class<ReLang> getLanguage() {
        return ReLang.class;
    }

    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        RootNode root = node.getRootNode();
        if (root != null && root.getName() != null) {
            return root.getName();
        }
        return "scope";
    }

    /**
     * Array of scope member names for interop.
     */
    @ExportLibrary(InteropLibrary.class)
    static final class ScopeMembers implements TruffleObject {
        private final String[] names;

        ScopeMembers(FrameDescriptor descriptor) {
            if (descriptor == null) {
                this.names = new String[0];
            } else {
                var nameList = new java.util.ArrayList<String>();
                for (int i = 0; i < descriptor.getNumberOfSlots(); i++) {
                    Object slotName = descriptor.getSlotName(i);
                    if (slotName != null) {
                        nameList.add(slotName.toString());
                    }
                }
                this.names = nameList.toArray(new String[0]);
            }
        }

        @ExportMessage
        boolean hasArrayElements() { return true; }

        @ExportMessage
        long getArraySize() { return names.length; }

        @ExportMessage
        boolean isArrayElementReadable(long index) {
            return index >= 0 && index < names.length;
        }

        @ExportMessage
        Object readArrayElement(long index) {
            return names[(int) index];
        }
    }
}

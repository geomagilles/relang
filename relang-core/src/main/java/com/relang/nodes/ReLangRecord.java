package com.relang.nodes;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime representation of a user-defined record type instance.
 * Supports field access via Truffle interop and structural equality.
 */
@ExportLibrary(InteropLibrary.class)
public final class ReLangRecord implements TruffleObject {
    private final String typeName;
    private final Map<String, Object> fields;

    public ReLangRecord(String typeName, Map<String, Object> fields) {
        this.typeName = typeName;
        this.fields = new LinkedHashMap<>(fields);
    }

    public String getTypeName() { return typeName; }

    public Object getField(String name) {
        if (!fields.containsKey(name)) {
            throw new ReLangTypeError(null, "No field '" + name + "' in type " + typeName);
        }
        return fields.get(name);
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    // --- Truffle interop ---

    @ExportMessage
    boolean hasMembers() { return true; }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return new FieldNames(fields.keySet().toArray(new String[0]));
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        return fields.containsKey(member);
    }

    @ExportMessage
    Object readMember(String member) throws UnsupportedMessageException {
        if (!fields.containsKey(member)) {
            throw UnsupportedMessageException.create();
        }
        return fields.get(member);
    }

    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaObject() {
        return new ReLangMetaType(typeName);
    }

    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return toString();
    }

    // --- Structural equality ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReLangRecord other)) return false;
        return typeName.equals(other.typeName) && fields.equals(other.fields);
    }

    @Override
    public int hashCode() { return Objects.hash(typeName, fields); }

    @Override
    public String toString() {
        var sb = new StringBuilder(typeName).append(" { ");
        var first = true;
        for (var entry : fields.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            first = false;
        }
        sb.append(" }");
        return sb.toString();
    }

    // --- Inner class for field names (TruffleObject array) ---

    @ExportLibrary(InteropLibrary.class)
    static final class FieldNames implements TruffleObject {
        private final String[] names;

        FieldNames(String[] names) { this.names = names; }

        @ExportMessage
        boolean hasArrayElements() { return true; }

        @ExportMessage
        long getArraySize() { return names.length; }

        @ExportMessage
        boolean isArrayElementReadable(long index) { return index >= 0 && index < names.length; }

        @ExportMessage
        Object readArrayElement(long index) {
            return names[(int) index];
        }
    }
}

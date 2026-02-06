package com.relang.nodes;

import com.oracle.truffle.api.interop.TruffleObject;
import java.util.Arrays;

public final class ReLangBytes implements TruffleObject {
    private final byte[] data;

    public ReLangBytes(byte[] data) {
        this.data = data.clone();
    }

    public byte[] getData() { return data.clone(); }
    public int length() { return data.length; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReLangBytes other)) return false;
        return Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() { return Arrays.hashCode(data); }

    @Override
    public String toString() {
        var sb = new StringBuilder("b\"");
        for (byte b : data) {
            sb.append(String.format("\\x%02x", b & 0xFF));
        }
        sb.append('"');
        return sb.toString();
    }
}

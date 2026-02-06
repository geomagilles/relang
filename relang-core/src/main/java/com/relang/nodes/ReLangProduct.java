package com.relang.nodes;

import com.oracle.truffle.api.interop.TruffleObject;

import java.util.Arrays;

/**
 * Runtime representation of a product value (conjunction of values).
 * For example, {@code (42 & "ok")} creates a product with two components.
 * Supports structural equality.
 */
public final class ReLangProduct implements TruffleObject {
    private final Object[] components;

    public ReLangProduct(Object[] components) {
        this.components = components.clone();
    }

    public Object get(int index) {
        if (index < 0 || index >= components.length) {
            throw new ReLangTypeError(null,
                    "Product index " + index + " out of bounds (size: " + components.length + ")");
        }
        return components[index];
    }

    public int size() { return components.length; }

    public Object[] getComponents() { return components.clone(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReLangProduct other)) return false;
        return Arrays.deepEquals(components, other.components);
    }

    @Override
    public int hashCode() { return Arrays.deepHashCode(components); }

    @Override
    public String toString() {
        var sb = new StringBuilder("(");
        for (int i = 0; i < components.length; i++) {
            if (i > 0) sb.append(" & ");
            sb.append(components[i]);
        }
        sb.append(")");
        return sb.toString();
    }
}

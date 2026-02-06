package com.relang.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

/**
 * Converts any value to its String representation.
 * Used for string interpolation.
 */
@NodeChild("value")
public abstract class ToStringNode extends ReLangNode {

    @Specialization
    protected String fromLong(long value) {
        return Long.toString(value);
    }

    @Specialization
    protected String fromDouble(double value) {
        // Format cleanly: 3.0 instead of 3.0, but 3.14 stays 3.14
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            long longVal = (long) value;
            return Long.toString(longVal) + ".0";
        }
        return Double.toString(value);
    }

    @Specialization
    protected String fromBoolean(boolean value) {
        return Boolean.toString(value);
    }

    @Specialization
    protected String fromString(String value) {
        return value;
    }

    @Specialization
    protected String fromObject(Object value) {
        if (value instanceof ReLangNone) return "none";
        if (value instanceof ReLangUnit) return "unit";
        if (value instanceof ReLangDuration d) return d.toString();
        if (value instanceof ReLangTimestamp t) return t.toString();
        if (value instanceof ReLangBytes b) return b.toString();
        if (value instanceof ReLangJson j) return j.toString();
        if (value instanceof ReLangFailure f) return f.toString();
        return value.toString();
    }
}

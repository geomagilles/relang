package com.relang.nodes;

import com.oracle.truffle.api.interop.TruffleObject;

public final class ReLangDuration implements TruffleObject {
    private final long millis;

    public ReLangDuration(long millis) {
        this.millis = millis;
    }

    public long toMillis() { return millis; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReLangDuration other)) return false;
        return millis == other.millis;
    }

    @Override
    public int hashCode() { return Long.hashCode(millis); }

    @Override
    public String toString() {
        if (millis % 3_600_000 == 0) return (millis / 3_600_000) + "h";
        if (millis % 60_000 == 0) return (millis / 60_000) + "min";
        if (millis % 1_000 == 0) return (millis / 1_000) + "s";
        return millis + "ms";
    }
}

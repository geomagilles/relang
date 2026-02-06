package com.relang.nodes;

import com.oracle.truffle.api.interop.TruffleObject;
import java.time.Instant;

public final class ReLangTimestamp implements TruffleObject {
    private final long epochMillis;

    public ReLangTimestamp(long epochMillis) {
        this.epochMillis = epochMillis;
    }

    public static ReLangTimestamp now() {
        return new ReLangTimestamp(System.currentTimeMillis());
    }

    public long toEpochMillis() { return epochMillis; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReLangTimestamp other)) return false;
        return epochMillis == other.epochMillis;
    }

    @Override
    public int hashCode() { return Long.hashCode(epochMillis); }

    @Override
    public String toString() {
        return Instant.ofEpochMilli(epochMillis).toString();
    }
}

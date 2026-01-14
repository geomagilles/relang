package com.relang.nodes;

public class SuspendedResult {
    private final ResumableState state;

    public SuspendedResult(ResumableState state) {
        this.state = state;
    }

    public ResumableState getState() {
        return state;
    }
}

package com.relang.nodes;

import java.io.Serializable;

/**
 * Wrapper for ResumableState returned to the host when execution suspends.
 * Implements Serializable so it can be persisted and restored later.
 */
public class SuspendedResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final ResumableState state;

    public SuspendedResult(ResumableState state) {
        this.state = state;
    }

    public ResumableState getState() {
        return state;
    }
}

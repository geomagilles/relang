package com.relang.nodes;

import java.io.Serializable;

/**
 * Wrapper for ResumableState returned to the host when execution suspends.
 * Supports both Java serialization and JSON serialization.
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
    
    /**
     * Serialize this result to a JSON string.
     */
    public String toJson() {
        return state.toJson();
    }
    
    /**
     * Deserialize a SuspendedResult from a JSON string.
     */
    public static SuspendedResult fromJson(String json) {
        return new SuspendedResult(ResumableState.fromJson(json));
    }
}

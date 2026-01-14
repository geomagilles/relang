package com.relang.nodes;

import com.google.protobuf.InvalidProtocolBufferException;
import com.relang.proto.ResumableStateProtos.ResumableStateProto;

import java.io.Serializable;

/**
 * Wrapper for ResumableState returned to the host when execution suspends.
 * Supports multiple serialization formats:
 * - Java serialization (Serializable)
 * - JSON (toJson/fromJson)
 * - Protocol Buffers (toProtoBytes/fromProtoBytes)
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
    
    // ==================== JSON Serialization ====================
    
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
    
    // ==================== Protobuf Serialization ====================
    
    /**
     * Serialize this result to Protocol Buffer bytes.
     * More compact than JSON, better for production storage at scale.
     */
    public byte[] toProtoBytes() {
        return state.toProtoBytes();
    }
    
    /**
     * Serialize this result to a Protocol Buffer message.
     */
    public ResumableStateProto toProto() {
        return state.toProto();
    }
    
    /**
     * Deserialize a SuspendedResult from Protocol Buffer bytes.
     */
    public static SuspendedResult fromProtoBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return new SuspendedResult(ResumableState.fromProtoBytes(bytes));
    }
    
    /**
     * Deserialize a SuspendedResult from a Protocol Buffer message.
     */
    public static SuspendedResult fromProto(ResumableStateProto proto) {
        return new SuspendedResult(ResumableState.fromProto(proto));
    }
}

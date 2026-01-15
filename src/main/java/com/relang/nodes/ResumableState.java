package com.relang.nodes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.relang.proto.ResumableStateProtos;
import com.relang.proto.ResumableStateProtos.FrameStateProto;
import com.relang.proto.ResumableStateProtos.LocalValue;
import com.relang.proto.ResumableStateProtos.ResumableStateProto;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Represents the suspended execution state that can be serialized and restored later.
 * Contains a stack of FrameState objects representing the call stack at suspension.
 * 
 * Includes a source code hash to detect code changes between suspend and resume,
 * preventing subtle bugs from mismatched execution paths.
 * 
 * Supports multiple serialization formats:
 * - Java serialization (Serializable)
 * - JSON (toJson/fromJson)
 * - Protocol Buffers (toProto/fromProto)
 */
public class ResumableState implements Serializable {
    
    private static final long serialVersionUID = 2L;  // Bumped for sourceHash addition
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // SHA-256 hash of the source code at suspension time
    private String sourceHash;
    
    // Stack of frames stored as ArrayList (last element = top of stack)
    private final ArrayList<FrameState> frames = new ArrayList<>();
    
    /**
     * Compute SHA-256 hash of source code.
     */
    public static String computeSourceHash(String sourceCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sourceCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
    }
    
    public String getSourceHash() {
        return sourceHash;
    }
    
    /**
     * Verify that the given source code matches this state's hash.
     * @throws StateCodeMismatchException if the code has changed
     */
    public void verifySourceHash(String currentSourceCode) {
        if (sourceHash == null) {
            return;  // No hash stored, skip verification
        }
        String currentHash = computeSourceHash(currentSourceCode);
        if (!sourceHash.equals(currentHash)) {
            throw new StateCodeMismatchException(
                "Source code has changed since suspension. " +
                "Stored hash: " + sourceHash.substring(0, 16) + "..., " +
                "Current hash: " + currentHash.substring(0, 16) + "..."
            );
        }
    }

    public void pushFrame(FrameState frame) {
        frames.add(frame);
    }

    public FrameState popFrame() {
        if (frames.isEmpty()) {
            throw new IllegalStateException("Cannot pop from empty frame stack");
        }
        return frames.remove(frames.size() - 1);
    }

    public FrameState peekFrame() {
        if (frames.isEmpty()) {
            return null;
        }
        return frames.get(frames.size() - 1);
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }
    
    public int getFrameCount() {
        return frames.size();
    }
    
    /**
     * Serialize this state to a JSON string.
     */
    public String toJson() {
        JsonObject root = new JsonObject();
        
        // Include source hash for validation on resume
        if (sourceHash != null) {
            root.addProperty("sourceHash", sourceHash);
        }
        
        JsonArray framesArray = new JsonArray();
        for (FrameState frame : frames) {
            framesArray.add(frame.toJsonObject());
        }
        root.add("frames", framesArray);
        
        return GSON.toJson(root);
    }
    
    /**
     * Deserialize a ResumableState from a JSON string.
     */
    public static ResumableState fromJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        ResumableState state = new ResumableState();
        
        // Restore source hash if present
        if (root.has("sourceHash") && !root.get("sourceHash").isJsonNull()) {
            state.setSourceHash(root.get("sourceHash").getAsString());
        }
        
        JsonArray framesArray = root.getAsJsonArray("frames");
        for (JsonElement elem : framesArray) {
            state.pushFrame(FrameState.fromJsonObject(elem.getAsJsonObject()));
        }
        
        return state;
    }
    
    /**
     * Serialize this state to a Protocol Buffer message.
     */
    public ResumableStateProto toProto() {
        ResumableStateProto.Builder builder = ResumableStateProto.newBuilder();
        
        // Include source hash for validation on resume
        if (sourceHash != null) {
            builder.setSourceHash(sourceHash);
        }
        
        for (FrameState frame : frames) {
            builder.addFrames(frame.toProto());
        }
        
        return builder.build();
    }
    
    /**
     * Serialize this state to Protocol Buffer bytes.
     */
    public byte[] toProtoBytes() {
        return toProto().toByteArray();
    }
    
    /**
     * Deserialize a ResumableState from a Protocol Buffer message.
     */
    public static ResumableState fromProto(ResumableStateProto proto) {
        ResumableState state = new ResumableState();
        
        // Restore source hash if present
        if (proto.hasSourceHash()) {
            state.setSourceHash(proto.getSourceHash());
        }
        
        for (FrameStateProto frameProto : proto.getFramesList()) {
            state.pushFrame(FrameState.fromProto(frameProto));
        }
        
        return state;
    }
    
    /**
     * Deserialize a ResumableState from Protocol Buffer bytes.
     */
    public static ResumableState fromProtoBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProto(ResumableStateProto.parseFrom(bytes));
    }

    /**
     * Represents the state of a single stack frame.
     * Contains local variables and the execution path (statement indices).
     */
    public static class FrameState implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        // Local variables - only Long and Boolean values are supported
        private final HashMap<String, Object> locals;
        
        // Execution path indices stored as ArrayList (last element = top of stack)
        private final ArrayList<Integer> executionPath;

        public FrameState(Map<String, Object> locals, Stack<Integer> path) {
            this.locals = new HashMap<>();
            validateAndCopyLocals(locals);
            this.executionPath = new ArrayList<>(path);
        }
        
        /**
         * Constructor for deserialization or direct construction with Lists.
         */
        public FrameState(Map<String, Object> locals, List<Integer> path) {
            this.locals = new HashMap<>();
            validateAndCopyLocals(locals);
            this.executionPath = new ArrayList<>(path);
        }
        
        private void validateAndCopyLocals(Map<String, Object> source) {
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                Object value = entry.getValue();
                if (value == null || value instanceof Long || value instanceof Boolean) {
                    this.locals.put(entry.getKey(), value);
                } else {
                    throw new IllegalArgumentException(
                        "Local variable '" + entry.getKey() + "' has non-serializable type: " 
                        + value.getClass().getName() + ". Only Long and Boolean are supported.");
                }
            }
        }

        public Map<String, Object> getLocals() {
            return locals;
        }

        public void pushPathIndex(int index) {
            executionPath.add(index);
        }

        public int popPathIndex() {
            if (executionPath.isEmpty()) {
                return -1;
            }
            return executionPath.remove(executionPath.size() - 1);
        }

        public boolean hasPath() {
            return !executionPath.isEmpty();
        }
        
        public List<Integer> getExecutionPath() {
            return new ArrayList<>(executionPath);
        }
        
        /**
         * Convert this frame to a JSON object.
         */
        JsonObject toJsonObject() {
            JsonObject obj = new JsonObject();
            
            // Locals with type information
            JsonObject localsObj = new JsonObject();
            for (Map.Entry<String, Object> entry : locals.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Long) {
                    localsObj.addProperty(entry.getKey(), (Long) value);
                } else if (value instanceof Boolean) {
                    localsObj.addProperty(entry.getKey(), (Boolean) value);
                } else {
                    localsObj.add(entry.getKey(), null);
                }
            }
            obj.add("locals", localsObj);
            
            // Execution path
            JsonArray pathArray = new JsonArray();
            for (Integer idx : executionPath) {
                pathArray.add(idx);
            }
            obj.add("executionPath", pathArray);
            
            return obj;
        }
        
        /**
         * Create a FrameState from a JSON object.
         */
        static FrameState fromJsonObject(JsonObject obj) {
            Map<String, Object> locals = new HashMap<>();
            JsonObject localsObj = obj.getAsJsonObject("locals");
            
            for (String key : localsObj.keySet()) {
                JsonElement elem = localsObj.get(key);
                if (elem.isJsonNull()) {
                    locals.put(key, null);
                } else if (elem.getAsJsonPrimitive().isBoolean()) {
                    locals.put(key, elem.getAsBoolean());
                } else if (elem.getAsJsonPrimitive().isNumber()) {
                    locals.put(key, elem.getAsLong());
                }
            }
            
            List<Integer> path = new ArrayList<>();
            JsonArray pathArray = obj.getAsJsonArray("executionPath");
            for (JsonElement elem : pathArray) {
                path.add(elem.getAsInt());
            }
            
            return new FrameState(locals, path);
        }
        
        /**
         * Convert this frame to a Protocol Buffer message.
         */
        FrameStateProto toProto() {
            FrameStateProto.Builder builder = FrameStateProto.newBuilder();
            
            // Convert locals
            for (Map.Entry<String, Object> entry : locals.entrySet()) {
                LocalValue.Builder valueBuilder = LocalValue.newBuilder();
                Object value = entry.getValue();
                
                if (value instanceof Long) {
                    valueBuilder.setLongValue((Long) value);
                } else if (value instanceof Boolean) {
                    valueBuilder.setBoolValue((Boolean) value);
                }
                // null values are represented by an empty LocalValue (no field set)
                
                builder.putLocals(entry.getKey(), valueBuilder.build());
            }
            
            // Convert execution path
            for (Integer idx : executionPath) {
                builder.addExecutionPath(idx);
            }
            
            return builder.build();
        }
        
        /**
         * Create a FrameState from a Protocol Buffer message.
         */
        static FrameState fromProto(FrameStateProto proto) {
            Map<String, Object> locals = new HashMap<>();
            
            for (Map.Entry<String, LocalValue> entry : proto.getLocalsMap().entrySet()) {
                LocalValue value = entry.getValue();
                
                if (value.hasLongValue()) {
                    locals.put(entry.getKey(), value.getLongValue());
                } else if (value.hasBoolValue()) {
                    locals.put(entry.getKey(), value.getBoolValue());
                } else {
                    locals.put(entry.getKey(), null);
                }
            }
            
            List<Integer> path = new ArrayList<>(proto.getExecutionPathList());
            
            return new FrameState(locals, path);
        }
    }
}

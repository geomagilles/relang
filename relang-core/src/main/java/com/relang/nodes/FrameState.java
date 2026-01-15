package com.relang.nodes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.relang.proto.ResumableStateProtos.FrameStateProto;
import com.relang.proto.ResumableStateProtos.LocalValue;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Represents the state of a single stack frame for resumable execution.
 * Contains local variables and the execution path (statement indices).
 * <p>
 * Supports multiple serialization formats:
 * <ul>
 *   <li>Java serialization (Serializable)</li>
 *   <li>JSON (toJsonObject/fromJsonObject)</li>
 *   <li>Protocol Buffers (toProto/fromProto)</li>
 * </ul>
 * <p>
 * Only {@code Long} and {@code Boolean} local variable types are supported.
 */
public final class FrameState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HashMap<String, Object> locals;
    private final ArrayList<Integer> executionPath;

    /**
     * Creates a new FrameState from a map of locals and a path stack.
     *
     * @param locals local variable values (only Long and Boolean supported)
     * @param path   execution path as a stack of block indices
     * @throws IllegalArgumentException if locals contain unsupported types
     */
    public FrameState(Map<String, Object> locals, Stack<Integer> path) {
        this.locals = new HashMap<>();
        validateAndCopyLocals(locals);
        this.executionPath = new ArrayList<>(path);
    }

    /**
     * Creates a new FrameState from a map of locals and a path list.
     * Used for deserialization or direct construction.
     *
     * @param locals local variable values (only Long and Boolean supported)
     * @param path   execution path as a list of block indices
     * @throws IllegalArgumentException if locals contain unsupported types
     */
    public FrameState(Map<String, Object> locals, List<Integer> path) {
        this.locals = new HashMap<>();
        validateAndCopyLocals(locals);
        this.executionPath = new ArrayList<>(path);
    }

    private void validateAndCopyLocals(Map<String, Object> source) {
        for (var entry : source.entrySet()) {
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
        return executionPath.removeLast();
    }

    public boolean hasPath() {
        return !executionPath.isEmpty();
    }

    public List<Integer> getExecutionPath() {
        return new ArrayList<>(executionPath);
    }

    // ----- JSON Serialization -----

    JsonObject toJsonObject() {
        var obj = new JsonObject();

        var localsObj = new JsonObject();
        for (var entry : locals.entrySet()) {
            switch (entry.getValue()) {
                case Long l -> localsObj.addProperty(entry.getKey(), l);
                case Boolean b -> localsObj.addProperty(entry.getKey(), b);
                case null, default -> localsObj.add(entry.getKey(), null);
            }
        }
        obj.add("locals", localsObj);

        var pathArray = new JsonArray();
        for (Integer idx : executionPath) {
            pathArray.add(idx);
        }
        obj.add("executionPath", pathArray);

        return obj;
    }

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

    // ----- Protocol Buffer Serialization -----

    FrameStateProto toProto() {
        var builder = FrameStateProto.newBuilder();

        for (var entry : locals.entrySet()) {
            var valueBuilder = LocalValue.newBuilder();
            switch (entry.getValue()) {
                case Long l -> valueBuilder.setLongValue(l);
                case Boolean b -> valueBuilder.setBoolValue(b);
                case null, default -> { /* empty LocalValue represents null */ }
            }
            builder.putLocals(entry.getKey(), valueBuilder.build());
        }

        for (Integer idx : executionPath) {
            builder.addExecutionPath(idx);
        }

        return builder.build();
    }

    static FrameState fromProto(FrameStateProto proto) {
        Map<String, Object> locals = new HashMap<>();

        for (var entry : proto.getLocalsMap().entrySet()) {
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

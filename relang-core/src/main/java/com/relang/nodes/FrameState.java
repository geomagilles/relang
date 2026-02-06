package com.relang.nodes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
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
 * Supported local variable types: Long, Boolean, Double, String,
 * ReLangNone, ReLangUnit, AwaitableHandle.
 */
public final class FrameState implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    private final HashMap<String, Object> locals;
    private final ArrayList<Integer> executionPath;

    /**
     * Creates a new FrameState from a map of locals and a path stack.
     *
     * @param locals local variable values
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
     * @param locals local variable values
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
            var value = entry.getValue();
            if (value == null
                    || value instanceof Long
                    || value instanceof Boolean
                    || value instanceof Double
                    || value instanceof String
                    || value instanceof ReLangNone
                    || value instanceof ReLangUnit
                    || value instanceof AwaitableHandle) {
                this.locals.put(entry.getKey(), value);
            } else {
                throw new IllegalArgumentException(
                        "Local variable '" + entry.getKey() + "' has non-serializable type: "
                                + value.getClass().getName());
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
                case Double d -> localsObj.addProperty(entry.getKey(), d);
                case String s -> {
                    var strMarker = new JsonObject();
                    strMarker.addProperty("__type", "string");
                    strMarker.addProperty("value", s);
                    localsObj.add(entry.getKey(), strMarker);
                }
                case ReLangNone ignored -> {
                    var noneMarker = new JsonObject();
                    noneMarker.addProperty("__type", "none");
                    localsObj.add(entry.getKey(), noneMarker);
                }
                case ReLangUnit ignored -> {
                    var unitMarker = new JsonObject();
                    unitMarker.addProperty("__type", "unit");
                    localsObj.add(entry.getKey(), unitMarker);
                }
                case AwaitableHandle h -> {
                    var handleObj = new JsonObject();
                    handleObj.addProperty("__type", "awaitable");
                    handleObj.addProperty("id", h.getId());
                    localsObj.add(entry.getKey(), handleObj);
                }
                case null -> localsObj.add(entry.getKey(), JsonNull.INSTANCE);
                default -> localsObj.add(entry.getKey(), JsonNull.INSTANCE);
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

    /**
     * Deserialize from JSON without awaitable table (backward compatible).
     */
    static FrameState fromJsonObject(JsonObject obj) {
        return fromJsonObject(obj, null);
    }

    /**
     * Deserialize from JSON with optional awaitable table for resolving handle references.
     */
    static FrameState fromJsonObject(JsonObject obj, AwaitableTable awaitableTable) {
        Map<String, Object> locals = new HashMap<>();
        var localsObj = obj.getAsJsonObject("locals");

        for (var key : localsObj.keySet()) {
            var elem = localsObj.get(key);
            if (elem.isJsonNull()) {
                locals.put(key, null);
            } else if (elem.isJsonObject()) {
                var objElem = elem.getAsJsonObject();
                if (objElem.has("__type")) {
                    var type = objElem.get("__type").getAsString();
                    switch (type) {
                        case "none" -> locals.put(key, ReLangNone.SINGLETON);
                        case "unit" -> locals.put(key, ReLangUnit.SINGLETON);
                        case "string" -> locals.put(key, objElem.get("value").getAsString());
                        case "awaitable" -> {
                            var handleId = objElem.get("id").getAsString();
                            if (awaitableTable != null) {
                                var handle = awaitableTable.get(handleId);
                                if (handle != null) {
                                    locals.put(key, handle);
                                }
                            }
                            // If no table or handle not found, skip (will be null)
                        }
                        default -> locals.put(key, null);
                    }
                } else {
                    locals.put(key, null);
                }
            } else if (elem.getAsJsonPrimitive().isBoolean()) {
                locals.put(key, elem.getAsBoolean());
            } else if (elem.getAsJsonPrimitive().isNumber()) {
                // Distinguish Long from Double: if the number has a decimal point, it's a Double
                var numStr = elem.getAsJsonPrimitive().getAsString();
                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                    locals.put(key, elem.getAsDouble());
                } else {
                    locals.put(key, elem.getAsLong());
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        var pathArray = obj.getAsJsonArray("executionPath");
        for (var elem : pathArray) {
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
                case Double d -> valueBuilder.setDoubleValue(d);
                case String s -> valueBuilder.setStringValue(s);
                case ReLangNone ignored -> valueBuilder.setNoneValue(true);
                case ReLangUnit ignored -> valueBuilder.setUnitValue(true);
                case AwaitableHandle h -> valueBuilder.setAwaitableRef(h.getId());
                case null, default -> { /* empty LocalValue represents null */ }
            }
            builder.putLocals(entry.getKey(), valueBuilder.build());
        }

        for (Integer idx : executionPath) {
            builder.addExecutionPath(idx);
        }

        return builder.build();
    }

    /**
     * Deserialize from protobuf without awaitable table (backward compatible).
     */
    static FrameState fromProto(FrameStateProto proto) {
        return fromProto(proto, null);
    }

    /**
     * Deserialize from protobuf with optional awaitable table for resolving handle references.
     */
    static FrameState fromProto(FrameStateProto proto, AwaitableTable awaitableTable) {
        Map<String, Object> locals = new HashMap<>();

        for (var entry : proto.getLocalsMap().entrySet()) {
            var value = entry.getValue();
            switch (value.getValueCase()) {
                case LONG_VALUE -> locals.put(entry.getKey(), value.getLongValue());
                case BOOL_VALUE -> locals.put(entry.getKey(), value.getBoolValue());
                case DOUBLE_VALUE -> locals.put(entry.getKey(), value.getDoubleValue());
                case STRING_VALUE -> locals.put(entry.getKey(), value.getStringValue());
                case NONE_VALUE -> locals.put(entry.getKey(), ReLangNone.SINGLETON);
                case UNIT_VALUE -> locals.put(entry.getKey(), ReLangUnit.SINGLETON);
                case AWAITABLE_REF -> {
                    var handleId = value.getAwaitableRef();
                    if (awaitableTable != null) {
                        var handle = awaitableTable.get(handleId);
                        if (handle != null) {
                            locals.put(entry.getKey(), handle);
                        }
                    }
                }
                default -> locals.put(entry.getKey(), null);
            }
        }

        List<Integer> path = new ArrayList<>(proto.getExecutionPathList());
        return new FrameState(locals, path);
    }
}

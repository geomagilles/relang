package com.relang.nodes;

import com.google.gson.*;
import com.google.protobuf.InvalidProtocolBufferException;
import com.relang.proto.ResumableStateProtos.AwaitableHandleProto;
import com.relang.proto.ResumableStateProtos.FrameStateProto;
import com.relang.proto.ResumableStateProtos.LocalValue;
import com.relang.proto.ResumableStateProtos.ResumableStateProto;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;

/**
 * Represents the suspended execution state that can be serialized and restored later.
 * Contains a stack of FrameState objects representing the call stack at suspension,
 * an AwaitableTable tracking all awaitables, and the ID of the awaitable that
 * caused the last suspension (if any).
 * <p>
 * Includes a source code hash to detect code changes between suspend and resume,
 * preventing subtle bugs from mismatched execution paths.
 * <p>
 * Supports multiple serialization formats:
 * <ul>
 *   <li>Java serialization (Serializable)</li>
 *   <li>JSON (toJson/fromJson)</li>
 *   <li>Protocol Buffers (toProto/fromProto)</li>
 * </ul>
 */
public class ResumableState implements Serializable {

    @Serial
    private static final long serialVersionUID = 3L;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // Stack of frames stored as ArrayList (last element = top of stack)
    private final ArrayList<FrameState> frames = new ArrayList<>();
    // SHA-256 hash of the source code at suspension time
    private String sourceHash;
    // Table tracking all awaitables in this execution
    private AwaitableTable awaitableTable;
    // ID of the awaitable that caused the last suspension (null for manual checkpoint)
    private String awaitedHandleId;

    /**
     * Compute SHA-256 hash of source code.
     */
    public static String computeSourceHash(String sourceCode) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sourceCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Deserialize a ResumableState from a JSON string.
     */
    public static ResumableState fromJson(String json) {
        var root = JsonParser.parseString(json).getAsJsonObject();
        var state = new ResumableState();

        // Restore source hash if present
        if (root.has("sourceHash") && !root.get("sourceHash").isJsonNull()) {
            state.setSourceHash(root.get("sourceHash").getAsString());
        }

        // Restore awaitable table if present (must be done before frames for handle references)
        AwaitableTable table = null;
        if (root.has("awaitables") && !root.get("awaitables").isJsonNull()) {
            table = new AwaitableTable();
            var awaitablesObj = root.getAsJsonObject("awaitables");
            for (var key : awaitablesObj.keySet()) {
                var handleObj = awaitablesObj.getAsJsonObject(key);
                var id = handleObj.get("id").getAsString();
                var createdAt = handleObj.get("createdAt").getAsLong();
                var status = AwaitableHandle.Status.valueOf(handleObj.get("status").getAsString());
                Object result = null;
                if (handleObj.has("result") && !handleObj.get("result").isJsonNull()) {
                    result = deserializeResultFromJson(handleObj.get("result"));
                }
                table.register(new AwaitableHandle(id, createdAt, status, result));
            }
            state.setAwaitableTable(table);
        }

        // Restore awaited handle ID if present
        if (root.has("awaitedHandleId") && !root.get("awaitedHandleId").isJsonNull()) {
            state.setAwaitedHandleId(root.get("awaitedHandleId").getAsString());
        }

        // Restore frames (pass awaitable table for handle references)
        var framesArray = root.getAsJsonArray("frames");
        for (var elem : framesArray) {
            state.pushFrame(FrameState.fromJsonObject(elem.getAsJsonObject(), table));
        }

        return state;
    }

    /**
     * Deserialize a ResumableState from a Protocol Buffer message.
     */
    public static ResumableState fromProto(ResumableStateProto proto) {
        var state = new ResumableState();

        // Restore source hash if present
        if (proto.hasSourceHash()) {
            state.setSourceHash(proto.getSourceHash());
        }

        // Restore awaitable table (must be done before frames for handle references)
        AwaitableTable table = null;
        if (!proto.getAwaitablesMap().isEmpty()) {
            table = new AwaitableTable();
            for (var entry : proto.getAwaitablesMap().entrySet()) {
                var handleProto = entry.getValue();
                var status = AwaitableHandle.Status.valueOf(handleProto.getStatus());
                Object result = null;
                if (handleProto.hasResult()) {
                    result = deserializeResultFromProto(handleProto.getResult());
                }
                table.register(new AwaitableHandle(
                        handleProto.getId(),
                        handleProto.getCreatedAt(),
                        status,
                        result
                ));
            }
            state.setAwaitableTable(table);
        }

        // Restore awaited handle ID
        if (proto.hasAwaitedHandleId()) {
            state.setAwaitedHandleId(proto.getAwaitedHandleId());
        }

        for (var frameProto : proto.getFramesList()) {
            state.pushFrame(FrameState.fromProto(frameProto, table));
        }

        return state;
    }

    /**
     * Deserialize a ResumableState from Protocol Buffer bytes.
     */
    public static ResumableState fromProtoBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProto(ResumableStateProto.parseFrom(bytes));
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
    }

    public AwaitableTable getAwaitableTable() {
        return awaitableTable;
    }

    public void setAwaitableTable(AwaitableTable awaitableTable) {
        this.awaitableTable = awaitableTable;
    }

    public String getAwaitedHandleId() {
        return awaitedHandleId;
    }

    public void setAwaitedHandleId(String awaitedHandleId) {
        this.awaitedHandleId = awaitedHandleId;
    }

    /**
     * Verify that the given source code matches this state's hash.
     *
     * @throws StateCodeMismatchException if the code has changed
     */
    public void verifySourceHash(String currentSourceCode) {
        if (sourceHash == null) {
            return;  // No hash stored, skip verification
        }
        var currentHash = computeSourceHash(currentSourceCode);
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
        return frames.removeLast();
    }

    public FrameState peekFrame() {
        if (frames.isEmpty()) {
            return null;
        }
        return frames.getLast();
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
        var root = new JsonObject();

        // Include source hash for validation on resume
        if (sourceHash != null) {
            root.addProperty("sourceHash", sourceHash);
        }

        var framesArray = new JsonArray();
        for (var frame : frames) {
            framesArray.add(frame.toJsonObject());
        }
        root.add("frames", framesArray);

        // Include awaitable table
        if (awaitableTable != null && !awaitableTable.isEmpty()) {
            var awaitablesObj = new JsonObject();
            for (var entry : awaitableTable.getAll().entrySet()) {
                var handleObj = new JsonObject();
                var handle = entry.getValue();
                handleObj.addProperty("id", handle.getId());
                handleObj.addProperty("createdAt", handle.getCreatedAt());
                handleObj.addProperty("status", handle.getStatus().name());
                if (handle.getResult() != null) {
                    handleObj.add("result", serializeResultToJson(handle.getResult()));
                }
                awaitablesObj.add(entry.getKey(), handleObj);
            }
            root.add("awaitables", awaitablesObj);
        }

        // Include awaited handle ID
        if (awaitedHandleId != null) {
            root.addProperty("awaitedHandleId", awaitedHandleId);
        }

        return GSON.toJson(root);
    }

    /**
     * Serialize this state to a Protocol Buffer message.
     */
    public ResumableStateProto toProto() {
        var builder = ResumableStateProto.newBuilder();

        // Include source hash for validation on resume
        if (sourceHash != null) {
            builder.setSourceHash(sourceHash);
        }

        for (var frame : frames) {
            builder.addFrames(frame.toProto());
        }

        // Include awaitable table
        if (awaitableTable != null && !awaitableTable.isEmpty()) {
            for (var entry : awaitableTable.getAll().entrySet()) {
                var handle = entry.getValue();
                var handleBuilder = AwaitableHandleProto.newBuilder()
                        .setId(handle.getId())
                        .setCreatedAt(handle.getCreatedAt())
                        .setStatus(handle.getStatus().name());
                if (handle.getResult() != null) {
                    handleBuilder.setResult(serializeResultToProto(handle.getResult()));
                }
                builder.putAwaitables(entry.getKey(), handleBuilder.build());
            }
        }

        // Include awaited handle ID
        if (awaitedHandleId != null) {
            builder.setAwaitedHandleId(awaitedHandleId);
        }

        return builder.build();
    }

    /**
     * Serialize this state to Protocol Buffer bytes.
     */
    public byte[] toProtoBytes() {
        return toProto().toByteArray();
    }

    // ----- Result serialization helpers -----

    private static JsonElement serializeResultToJson(Object value) {
        return switch (value) {
            case Long l -> new JsonPrimitive(l);
            case Double d -> new JsonPrimitive(d);
            case Boolean b -> new JsonPrimitive(b);
            case String s -> new JsonPrimitive(s);
            case ReLangNone ignored -> JsonNull.INSTANCE;
            case ReLangUnit ignored -> {
                var marker = new JsonObject();
                marker.addProperty("__type", "unit");
                yield marker;
            }
            case null -> JsonNull.INSTANCE;
            default -> throw new IllegalArgumentException(
                    "Cannot serialize result of type: " + value.getClass().getName());
        };
    }

    private static Object deserializeResultFromJson(JsonElement elem) {
        if (elem.isJsonNull()) {
            return ReLangNone.SINGLETON;
        } else if (elem.isJsonObject()) {
            var obj = elem.getAsJsonObject();
            if (obj.has("__type") && "unit".equals(obj.get("__type").getAsString())) {
                return ReLangUnit.SINGLETON;
            }
            return null;
        } else if (elem.isJsonPrimitive()) {
            var prim = elem.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                return prim.getAsBoolean();
            } else if (prim.isNumber()) {
                // Check if it has a decimal point
                var numStr = prim.getAsString();
                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                    return prim.getAsDouble();
                }
                return prim.getAsLong();
            } else if (prim.isString()) {
                return prim.getAsString();
            }
        }
        return null;
    }

    private static LocalValue serializeResultToProto(Object value) {
        var builder = LocalValue.newBuilder();
        switch (value) {
            case Long l -> builder.setLongValue(l);
            case Double d -> builder.setDoubleValue(d);
            case Boolean b -> builder.setBoolValue(b);
            case String s -> builder.setStringValue(s);
            case ReLangNone ignored -> builder.setNoneValue(true);
            case ReLangUnit ignored -> builder.setUnitValue(true);
            case null -> builder.setNoneValue(true);
            default -> throw new IllegalArgumentException(
                    "Cannot serialize result of type: " + value.getClass().getName());
        }
        return builder.build();
    }

    private static Object deserializeResultFromProto(LocalValue value) {
        return switch (value.getValueCase()) {
            case LONG_VALUE -> value.getLongValue();
            case BOOL_VALUE -> value.getBoolValue();
            case DOUBLE_VALUE -> value.getDoubleValue();
            case STRING_VALUE -> value.getStringValue();
            case NONE_VALUE -> ReLangNone.SINGLETON;
            case UNIT_VALUE -> ReLangUnit.SINGLETON;
            default -> null;
        };
    }
}

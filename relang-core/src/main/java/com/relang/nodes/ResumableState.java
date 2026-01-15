package com.relang.nodes;

import com.google.gson.*;
import com.google.protobuf.InvalidProtocolBufferException;
import com.relang.proto.ResumableStateProtos.FrameStateProto;
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
 * Contains a stack of FrameState objects representing the call stack at suspension.
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
    private static final long serialVersionUID = 2L;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // Stack of frames stored as ArrayList (last element = top of stack)
    private final ArrayList<FrameState> frames = new ArrayList<>();
    // SHA-256 hash of the source code at suspension time
    private String sourceHash;

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

    public String getSourceHash() {
        return sourceHash;
    }

    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
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
}

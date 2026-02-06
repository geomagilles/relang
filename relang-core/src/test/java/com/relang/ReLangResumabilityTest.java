package com.relang;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.relang.nodes.AwaitableHandle;
import com.relang.nodes.ResumableState;
import com.relang.nodes.SuspendedResult;
import com.relang.proto.ResumableStateProtos.FrameStateProto;
import com.relang.proto.ResumableStateProtos.LocalValue;
import com.relang.proto.ResumableStateProtos.ResumableStateProto;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ReLangResumabilityTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .allowAllAccess(true)
                .allowHostAccess(HostAccess.ALL)
                .build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void testSimpleCheckpoint() {
        String src = """
                x = 10;
                checkpoint;
                x + 20;
                """;

        // First run: hits checkpoint, returns SuspendedResult
        Value result1 = context.eval("relang", src);

        assertTrue(result1.isHostObject(), "Result should be a SuspendedResult");
        SuspendedResult suspended = result1.asHostObject();
        assertNotNull(suspended.state());

        // Resume by passing state back via polyglot bindings
        context.getPolyglotBindings().putMember("resumeState", suspended);
        Value result2 = context.eval("relang", src);

        assertEquals(30, result2.asLong());
    }

    @Test
    void testCheckpointInFunction() {
        String src = """
                fn work() {
                    x = 10;
                    checkpoint;
                    return x + 20;
                }
                work();
                """;

        // First run: hits checkpoint
        Value result1 = context.eval("relang", src);

        assertTrue(result1.isHostObject(), "Result should be a SuspendedResult");
        SuspendedResult suspended = result1.asHostObject();

        // Resume
        context.getPolyglotBindings().putMember("resumeState", suspended);
        Value result2 = context.eval("relang", src);

        assertEquals(30, result2.asLong());
    }

    @Test
    void testCheckpointInNestedFunction() {
        String src = """
                fn inner() {
                    y = 5;
                    checkpoint;
                    return y * 2;
                }
                fn outer() {
                    x = 10;
                    result = inner();
                    return x + result;
                }
                outer();
                """;

        // First run: hits checkpoint in inner()
        Value result1 = context.eval("relang", src);

        assertTrue(result1.isHostObject(), "Result should be a SuspendedResult");
        SuspendedResult suspended = result1.asHostObject();

        // Verify we have frames for both outer() and inner()
        ResumableState state = suspended.state();
        assertFalse(state.isEmpty(), "State should have captured frames");

        // Resume
        context.getPolyglotBindings().putMember("resumeState", suspended);
        Value result2 = context.eval("relang", src);

        // inner() returns 5*2=10, outer() returns 10+10=20
        assertEquals(20, result2.asLong());
    }

    @Test
    void testMultipleCheckpoints() {
        String src = """
                x = 1;
                checkpoint;
                x = x + 10;
                checkpoint;
                x = x + 100;
                x;
                """;

        // First checkpoint
        Value result1 = context.eval("relang", src);
        assertTrue(result1.isHostObject());
        SuspendedResult suspended1 = result1.asHostObject();

        // Resume to second checkpoint
        context.getPolyglotBindings().putMember("resumeState", suspended1);
        Value result2 = context.eval("relang", src);
        assertTrue(result2.isHostObject());
        SuspendedResult suspended2 = result2.asHostObject();

        // Resume to completion
        context.getPolyglotBindings().putMember("resumeState", suspended2);
        Value result3 = context.eval("relang", src);

        assertEquals(111, result3.asLong());
    }

    @Test
    void testCheckpointPreservesLocalVariables() {
        String src = """
                a = 1;
                b = 2;
                c = 3;
                checkpoint;
                a + b + c;
                """;

        Value result1 = context.eval("relang", src);
        assertTrue(result1.isHostObject());
        SuspendedResult suspended = result1.asHostObject();

        context.getPolyglotBindings().putMember("resumeState", suspended);
        Value result2 = context.eval("relang", src);

        assertEquals(6, result2.asLong());
    }

    @Test
    void testSerializationRoundTrip() throws Exception {
        String src = """
                fn compute() {
                    x = 42;
                    y = 100;
                    checkpoint;
                    return x + y;
                }
                compute();
                """;

        // First run: hits checkpoint
        Value result1 = context.eval("relang", src);
        assertTrue(result1.isHostObject());
        SuspendedResult original = result1.asHostObject();

        // Serialize to bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }
        byte[] serialized = baos.toByteArray();

        // Verify we got some bytes
        assertTrue(serialized.length > 0, "Serialized data should not be empty");

        // Deserialize from bytes
        SuspendedResult deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            deserialized = (SuspendedResult) ois.readObject();
        }

        // Verify deserialized state
        assertNotNull(deserialized);
        assertNotNull(deserialized.state());
        assertFalse(deserialized.state().isEmpty());

        // Resume with deserialized state
        context.getPolyglotBindings().putMember("resumeState", deserialized);
        Value result2 = context.eval("relang", src);

        assertEquals(142, result2.asLong());
    }

    @Test
    void testSerializationWithNestedCalls() throws Exception {
        String src = """
                fn inner() {
                    a = 10;
                    checkpoint;
                    return a * 3;
                }
                fn outer() {
                    b = 5;
                    result = inner();
                    return b + result;
                }
                outer();
                """;

        // First run: hits checkpoint in inner()
        Value result1 = context.eval("relang", src);
        SuspendedResult original = result1.asHostObject();

        // Verify frame count (should have frames for: top-level, outer, inner)
        assertTrue(original.state().getFrameCount() >= 2,
                "Should have multiple frames for nested calls");

        // Serialize and deserialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        SuspendedResult deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            deserialized = (SuspendedResult) ois.readObject();
        }

        // Resume with deserialized state
        context.getPolyglotBindings().putMember("resumeState", deserialized);
        Value result2 = context.eval("relang", src);

        // inner() returns 10*3=30, outer() returns 5+30=35
        assertEquals(35, result2.asLong());
    }

    @Test
    void testJsonStructureSimple() {
        String src = """
                x = 42;
                checkpoint;
                x;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        String json = suspended.toJson();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        // Verify root structure
        assertTrue(root.has("frames"), "Root should have 'frames' array");
        JsonArray frames = root.getAsJsonArray("frames");

        // Top-level code has 1 frame
        assertEquals(1, frames.size(), "Should have 1 frame for top-level code");

        // Check the frame structure
        JsonObject frame = frames.get(0).getAsJsonObject();
        assertTrue(frame.has("locals"), "Frame should have 'locals'");
        assertTrue(frame.has("executionPath"), "Frame should have 'executionPath'");

        // Verify locals
        JsonObject locals = frame.getAsJsonObject("locals");
        assertTrue(locals.has("x"), "Locals should contain 'x'");
        assertEquals(42, locals.get("x").getAsLong(), "x should be 42");

        // Verify execution path
        JsonArray path = frame.getAsJsonArray("executionPath");
        assertNotNull(path, "executionPath should be an array");
        assertFalse(path.isEmpty(), "executionPath should not be empty");

        // Verify round-trip works
        SuspendedResult deserialized = SuspendedResult.fromJson(json);
        context.getPolyglotBindings().putMember("resumeState", deserialized);
        assertEquals(42, context.eval("relang", src).asLong());
    }

    @Test
    void testJsonStructureWithBooleans() {
        String src = """
                x = 1;
                flag = 1 < 2;
                checkpoint;
                if (flag) { x = x + 100; }
                x;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        String json = suspended.toJson();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray frames = root.getAsJsonArray("frames");

        // Find the frame with our variables
        JsonObject frameWithVars = frames.get(0).getAsJsonObject();
        JsonObject locals = frameWithVars.getAsJsonObject("locals");

        // Verify Long type
        assertTrue(locals.has("x"), "Locals should contain 'x'");
        assertTrue(locals.get("x").getAsJsonPrimitive().isNumber(), "x should be a number");
        assertEquals(1, locals.get("x").getAsLong());

        // Verify Boolean type
        assertTrue(locals.has("flag"), "Locals should contain 'flag'");
        assertTrue(locals.get("flag").getAsJsonPrimitive().isBoolean(), "flag should be a boolean");
        assertTrue(locals.get("flag").getAsBoolean(), "flag should be true");

        // Verify round-trip preserves types and execution works
        SuspendedResult deserialized = SuspendedResult.fromJson(json);
        context.getPolyglotBindings().putMember("resumeState", deserialized);
        assertEquals(101, context.eval("relang", src).asLong());
    }

    @Test
    void testJsonStructureNestedCalls() {
        String src = """
                fn inner() {
                    a = 10;
                    checkpoint;
                    return a * 3;
                }
                fn outer() {
                    b = 5;
                    result = inner();
                    return b + result;
                }
                outer();
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        String json = suspended.toJson();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray frames = root.getAsJsonArray("frames");

        // Should have multiple frames for nested calls
        assertTrue(frames.size() >= 3, "Should have at least 3 frames (inner, outer, top-level)");

        // Verify inner() frame has 'a'
        boolean foundA = false;
        boolean foundB = false;
        for (int i = 0; i < frames.size(); i++) {
            JsonObject frame = frames.get(i).getAsJsonObject();
            JsonObject locals = frame.getAsJsonObject("locals");
            if (locals.has("a")) {
                assertEquals(10, locals.get("a").getAsLong(), "a should be 10");
                foundA = true;
            }
            if (locals.has("b")) {
                assertEquals(5, locals.get("b").getAsLong(), "b should be 5");
                foundB = true;
            }
        }
        assertTrue(foundA, "Should find variable 'a' from inner()");
        assertTrue(foundB, "Should find variable 'b' from outer()");

        // Each frame should have valid structure
        for (int i = 0; i < frames.size(); i++) {
            JsonObject frame = frames.get(i).getAsJsonObject();
            assertTrue(frame.has("locals"), "Frame " + i + " should have 'locals'");
            assertTrue(frame.has("executionPath"), "Frame " + i + " should have 'executionPath'");
            assertTrue(frame.get("locals").isJsonObject(), "locals should be an object");
            assertTrue(frame.get("executionPath").isJsonArray(), "executionPath should be an array");
        }

        // Verify round-trip works
        SuspendedResult deserialized = SuspendedResult.fromJson(json);
        context.getPolyglotBindings().putMember("resumeState", deserialized);
        assertEquals(35, context.eval("relang", src).asLong());
    }

    @Test
    void testJsonStructureMultipleVariables() {
        String src = """
                a = 1;
                b = 2;
                c = 3;
                d = 4;
                e = 5;
                checkpoint;
                a + b + c + d + e;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        String json = suspended.toJson();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray frames = root.getAsJsonArray("frames");

        // Find frame with all variables
        JsonObject frameWithVars = frames.get(0).getAsJsonObject();
        JsonObject locals = frameWithVars.getAsJsonObject("locals");

        // Verify all variables present with correct values
        assertEquals(1, locals.get("a").getAsLong());
        assertEquals(2, locals.get("b").getAsLong());
        assertEquals(3, locals.get("c").getAsLong());
        assertEquals(4, locals.get("d").getAsLong());
        assertEquals(5, locals.get("e").getAsLong());

        // Verify round-trip
        SuspendedResult deserialized = SuspendedResult.fromJson(json);
        context.getPolyglotBindings().putMember("resumeState", deserialized);
        assertEquals(15, context.eval("relang", src).asLong());
    }

    @Test
    void testJsonExecutionPathStructure() {
        String src = """
                x = 1;
                x = x + 1;
                x = x + 1;
                checkpoint;
                x = x + 1;
                x;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        String json = suspended.toJson();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray frames = root.getAsJsonArray("frames");

        // Check execution path contains integers
        JsonObject frame = frames.get(0).getAsJsonObject();
        JsonArray path = frame.getAsJsonArray("executionPath");

        for (int i = 0; i < path.size(); i++) {
            assertTrue(path.get(i).getAsJsonPrimitive().isNumber(),
                    "Path element " + i + " should be a number");
        }

        // The path should point past the checkpoint (index 4 = after checkpoint at index 3)
        assertFalse(path.isEmpty(), "Path should not be empty");
        int resumeIndex = path.get(path.size() - 1).getAsInt();
        assertEquals(4, resumeIndex, "Should resume at statement after checkpoint");

        // Verify round-trip
        SuspendedResult deserialized = SuspendedResult.fromJson(json);
        context.getPolyglotBindings().putMember("resumeState", deserialized);
        assertEquals(4, context.eval("relang", src).asLong());
    }

    @Test
    void testJsonPrettyPrinted() {
        String src = """
                myVar = 42;
                checkpoint;
                myVar;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        String json = suspended.toJson();

        // Verify pretty-printed format
        assertTrue(json.contains("\n"), "JSON should be pretty-printed with newlines");
        assertTrue(json.contains("  "), "JSON should have indentation");

        // Should still be valid JSON
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertNotNull(root);
    }

    // ==================== Protobuf Serialization Tests ====================

    @Test
    void testProtoSerializationRoundTrip() throws Exception {
        String src = """
                fn compute() {
                    x = 42;
                    y = 100;
                    checkpoint;
                    return x + y;
                }
                compute();
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult original = result1.asHostObject();

        // Serialize to protobuf bytes
        byte[] protoBytes = original.toProtoBytes();

        // Verify we got some bytes (should be compact)
        assertTrue(protoBytes.length > 0, "Protobuf bytes should not be empty");
        assertTrue(protoBytes.length < 200, "Protobuf should be compact");

        // Deserialize from protobuf bytes
        SuspendedResult deserialized = SuspendedResult.fromProtoBytes(protoBytes);

        // Verify state preserved
        assertNotNull(deserialized);
        assertEquals(original.state().getFrameCount(), deserialized.state().getFrameCount());

        // Resume with deserialized state
        context.getPolyglotBindings().putMember("resumeState", deserialized);
        Value result2 = context.eval("relang", src);

        assertEquals(142, result2.asLong());
    }

    @Test
    void testProtoStructure() throws Exception {
        String src = """
                x = 42;
                flag = 1 < 2;
                checkpoint;
                x;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        // Get the proto message directly
        ResumableStateProto proto = suspended.toProto();

        // Verify structure
        assertEquals(1, proto.getFramesCount(), "Should have 1 frame");

        FrameStateProto frame = proto.getFrames(0);

        // Check locals
        assertTrue(frame.containsLocals("x"), "Should have local 'x'");
        assertTrue(frame.containsLocals("flag"), "Should have local 'flag'");

        LocalValue xValue = frame.getLocalsOrThrow("x");
        assertTrue(xValue.hasLongValue(), "x should be a long");
        assertEquals(42, xValue.getLongValue());

        LocalValue flagValue = frame.getLocalsOrThrow("flag");
        assertTrue(flagValue.hasBoolValue(), "flag should be a boolean");
        assertTrue(flagValue.getBoolValue());

        // Check execution path
        assertTrue(frame.getExecutionPathCount() > 0, "Should have execution path");

        // Verify round-trip works
        SuspendedResult deserialized = SuspendedResult.fromProto(proto);
        context.getPolyglotBindings().putMember("resumeState", deserialized);
        assertEquals(42, context.eval("relang", src).asLong());
    }

    @Test
    void testProtoWithNestedCalls() throws Exception {
        String src = """
                fn inner() {
                    a = 10;
                    checkpoint;
                    return a * 3;
                }
                fn outer() {
                    b = 5;
                    result = inner();
                    return b + result;
                }
                outer();
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult original = result1.asHostObject();

        // Serialize to protobuf
        byte[] protoBytes = original.toProtoBytes();
        ResumableStateProto proto = ResumableStateProto.parseFrom(protoBytes);

        // Verify multiple frames
        assertTrue(proto.getFramesCount() >= 3, "Should have at least 3 frames");

        // Find frames with our variables
        boolean foundA = false;
        boolean foundB = false;
        for (FrameStateProto frame : proto.getFramesList()) {
            if (frame.containsLocals("a")) {
                assertEquals(10, frame.getLocalsOrThrow("a").getLongValue());
                foundA = true;
            }
            if (frame.containsLocals("b")) {
                assertEquals(5, frame.getLocalsOrThrow("b").getLongValue());
                foundB = true;
            }
        }
        assertTrue(foundA, "Should find variable 'a' from inner()");
        assertTrue(foundB, "Should find variable 'b' from outer()");

        // Verify round-trip works
        SuspendedResult deserialized = SuspendedResult.fromProtoBytes(protoBytes);
        context.getPolyglotBindings().putMember("resumeState", deserialized);
        assertEquals(35, context.eval("relang", src).asLong());
    }

    @Test
    void testProtoSmallerThanJson() {
        String src = """
                a = 1;
                b = 2;
                c = 3;
                d = 4;
                e = 5;
                checkpoint;
                a + b + c + d + e;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        byte[] protoBytes = suspended.toProtoBytes();
        String json = suspended.toJson();

        // Protobuf should be significantly smaller
        assertTrue(protoBytes.length < json.length(),
                "Protobuf (" + protoBytes.length + " bytes) should be smaller than JSON (" + json.length() + " bytes)");

        // Typically 3-5x smaller
        double ratio = (double) json.length() / protoBytes.length;
        assertTrue(ratio > 2.0,
                "JSON should be at least 2x larger than protobuf (actual ratio: " + ratio + ")");
    }

    @Test
    void testProtoAndJsonProduceSameResult() throws Exception {
        String src = """
                fn work() {
                    x = 123;
                    flag = 1 == 1;
                    checkpoint;
                    if (flag) { return x * 2; }
                    return 0;
                }
                work();
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult original = result1.asHostObject();

        // Serialize both ways
        String json = original.toJson();
        byte[] protoBytes = original.toProtoBytes();

        // Deserialize both
        SuspendedResult fromJson = SuspendedResult.fromJson(json);
        SuspendedResult fromProto = SuspendedResult.fromProtoBytes(protoBytes);

        // Both should produce the same result when resumed
        context.getPolyglotBindings().putMember("resumeState", fromJson);
        long jsonResult = context.eval("relang", src).asLong();

        context.getPolyglotBindings().putMember("resumeState", fromProto);
        long protoResult = context.eval("relang", src).asLong();

        assertEquals(246, jsonResult);
        assertEquals(246, protoResult);
        assertEquals(jsonResult, protoResult, "JSON and Protobuf should produce identical results");
    }

    // ==================== Source Hash Validation Tests ====================

    @Test
    void testSourceHashIncludedInState() {
        String src = """
                x = 42;
                checkpoint;
                x;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        // Verify source hash is set
        String sourceHash = suspended.state().getSourceHash();
        assertNotNull(sourceHash, "Source hash should be set");
        assertEquals(64, sourceHash.length(), "SHA-256 hash should be 64 hex characters");
    }

    @Test
    void testSourceHashInJson() {
        String src = """
                x = 42;
                checkpoint;
                x;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        String json = suspended.toJson();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        // Verify JSON contains sourceHash
        assertTrue(root.has("sourceHash"), "JSON should contain sourceHash");
        String jsonHash = root.get("sourceHash").getAsString();
        assertEquals(64, jsonHash.length(), "SHA-256 hash should be 64 hex characters");

        // Verify round-trip preserves hash
        SuspendedResult restored = SuspendedResult.fromJson(json);
        assertEquals(jsonHash, restored.state().getSourceHash());
    }

    @Test
    void testSourceHashInProto() throws Exception {
        String src = """
                x = 42;
                checkpoint;
                x;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        ResumableStateProto proto = suspended.toProto();

        // Verify proto contains sourceHash
        assertTrue(proto.hasSourceHash(), "Proto should contain sourceHash");
        String protoHash = proto.getSourceHash();
        assertEquals(64, protoHash.length(), "SHA-256 hash should be 64 hex characters");

        // Verify round-trip preserves hash
        SuspendedResult restored = SuspendedResult.fromProto(proto);
        assertEquals(protoHash, restored.state().getSourceHash());
    }

    @Test
    void testSameCodeProducesSameHash() {
        String src = """
                x = 42;
                checkpoint;
                x;
                """;

        // Run twice with same code
        Value result1 = context.eval("relang", src);
        SuspendedResult suspended1 = result1.asHostObject();
        String hash1 = suspended1.state().getSourceHash();

        // Resume first, then run again
        context.getPolyglotBindings().putMember("resumeState", suspended1);
        context.eval("relang", src);

        // Clear and run fresh
        context.getPolyglotBindings().removeMember("resumeState");
        Value result2 = context.eval("relang", src);
        SuspendedResult suspended2 = result2.asHostObject();
        String hash2 = suspended2.state().getSourceHash();

        assertEquals(hash1, hash2, "Same code should produce same hash");
    }

    @Test
    void testDifferentCodeProducesDifferentHash() {
        String src1 = """
                x = 42;
                checkpoint;
                x;
                """;

        String src2 = """
                x = 43;
                checkpoint;
                x;
                """;

        Value result1 = context.eval("relang", src1);
        SuspendedResult suspended1 = result1.asHostObject();

        // Resume to clear state
        context.getPolyglotBindings().putMember("resumeState", suspended1);
        context.eval("relang", src1);
        context.getPolyglotBindings().removeMember("resumeState");

        Value result2 = context.eval("relang", src2);
        SuspendedResult suspended2 = result2.asHostObject();

        assertNotEquals(
                suspended1.state().getSourceHash(),
                suspended2.state().getSourceHash(),
                "Different code should produce different hash"
        );
    }

    @Test
    void testResumeWithChangedCodeFails() {
        String srcOriginal = """
                x = 42;
                checkpoint;
                x + 10;
                """;

        String srcModified = """
                x = 42;
                y = 1;
                checkpoint;
                x + 10;
                """;

        // Suspend with original code
        Value result1 = context.eval("relang", srcOriginal);
        SuspendedResult suspended = result1.asHostObject();

        // Try to resume with modified code - should fail
        context.getPolyglotBindings().putMember("resumeState", suspended);

        // Truffle wraps exceptions in PolyglotException
        PolyglotException ex = assertThrows(
                PolyglotException.class,
                () -> context.eval("relang", srcModified),
                "Resume with changed code should throw exception"
        );
        assertTrue(ex.getMessage().contains("StateCodeMismatchException")
                        || ex.getMessage().contains("Source code has changed"),
                "Exception should indicate source code mismatch");
    }

    @Test
    void testResumeWithSameCodeSucceeds() {
        String src = """
                x = 42;
                checkpoint;
                x + 10;
                """;

        // Suspend
        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        // Resume with same code - should succeed
        context.getPolyglotBindings().putMember("resumeState", suspended);
        Value result2 = context.eval("relang", src);

        assertEquals(52, result2.asLong());
    }

    // ==================== S5: Awaitables & Await Tests ====================

    @Test
    void testAwaitPendingSuspends() {
        String src = """
                let x = 10;
                let handle = pending();
                let result = await handle;
                x + result;
                """;

        // First run: should suspend on await of pending handle
        Value result1 = context.eval("relang", src);
        assertTrue(result1.isHostObject(), "Should suspend on pending await");
        SuspendedResult suspended = result1.asHostObject();

        // Verify awaitable table and awaited handle ID
        assertNotNull(suspended.state().getAwaitableTable(), "Should have awaitable table");
        assertNotNull(suspended.state().getAwaitedHandleId(), "Should record awaited handle ID");

        // Resolve the pending awaitable
        String awaitedId = suspended.state().getAwaitedHandleId();
        AwaitableHandle handle = suspended.state().getAwaitableTable().get(awaitedId);
        assertNotNull(handle, "Handle should be in the table");
        assertTrue(handle.isPending(), "Handle should be pending before resolve");
        handle.resolve(42L);

        // Resume
        context.getPolyglotBindings().putMember("resumeState", suspended);
        Value result2 = context.eval("relang", src);
        assertEquals(52, result2.asLong()); // 10 + 42
    }

    @Test
    void testAwaitResolvedDoesNotSuspend() {
        String src = "await resolved(42);";
        Value result = context.eval("relang", src);
        assertFalse(result.isHostObject(), "Should NOT suspend on resolved await");
        assertEquals(42, result.asLong());
    }

    @Test
    void testMultipleAwaitsSequential() {
        String src = """
                let a = pending();
                let b = pending();
                let x = await a;
                let y = await b;
                x + y;
                """;

        // First suspension (await a)
        Value result1 = context.eval("relang", src);
        assertTrue(result1.isHostObject());
        SuspendedResult suspended1 = result1.asHostObject();
        suspended1.state().getAwaitableTable()
                .get(suspended1.state().getAwaitedHandleId()).resolve(10L);

        // Resume -> second suspension (await b)
        context.getPolyglotBindings().putMember("resumeState", suspended1);
        Value result2 = context.eval("relang", src);
        assertTrue(result2.isHostObject());
        SuspendedResult suspended2 = result2.asHostObject();
        suspended2.state().getAwaitableTable()
                .get(suspended2.state().getAwaitedHandleId()).resolve(20L);

        // Resume -> completion
        context.getPolyglotBindings().putMember("resumeState", suspended2);
        Value result3 = context.eval("relang", src);
        assertEquals(30, result3.asLong());
    }

    @Test
    void testResolvedAwaitableNotReplayedOnResume() {
        String src = """
                let a = pending();
                let x = await a;
                let b = pending();
                let y = await b;
                x + y;
                """;

        // First suspension (await a)
        Value result1 = context.eval("relang", src);
        SuspendedResult suspended1 = result1.asHostObject();
        String awaitedId1 = suspended1.state().getAwaitedHandleId();
        suspended1.state().getAwaitableTable().get(awaitedId1).resolve(100L);

        // Resume -> hits await b (second suspension)
        context.getPolyglotBindings().putMember("resumeState", suspended1);
        Value result2 = context.eval("relang", src);
        SuspendedResult suspended2 = result2.asHostObject();

        // Verify 'a' is still resolved in the table (not re-created as pending)
        AwaitableHandle handleA = null;
        for (var h : suspended2.state().getAwaitableTable().getAll().values()) {
            if (h.isResolved() && Long.valueOf(100L).equals(h.getResult())) {
                handleA = h;
                break;
            }
        }
        assertNotNull(handleA, "Previously resolved awaitable should still be resolved");

        // Resolve b and complete
        suspended2.state().getAwaitableTable()
                .get(suspended2.state().getAwaitedHandleId()).resolve(200L);
        context.getPolyglotBindings().putMember("resumeState", suspended2);
        Value result3 = context.eval("relang", src);
        assertEquals(300, result3.asLong());
    }

    @Test
    void testAwaitableTableJsonRoundTrip() {
        String src = """
                let h = pending();
                let x = await h;
                x;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        // Resolve and serialize to JSON
        suspended.state().getAwaitableTable()
                .get(suspended.state().getAwaitedHandleId()).resolve(99L);
        String json = suspended.toJson();

        // Restore from JSON and resume
        SuspendedResult restored = SuspendedResult.fromJson(json);
        context.getPolyglotBindings().putMember("resumeState", restored);
        Value result2 = context.eval("relang", src);
        assertEquals(99, result2.asLong());
    }

    @Test
    void testAwaitableTableProtoRoundTrip() throws Exception {
        String src = """
                let h = pending();
                let x = await h;
                x;
                """;

        Value result1 = context.eval("relang", src);
        SuspendedResult suspended = result1.asHostObject();

        // Resolve and serialize to protobuf
        suspended.state().getAwaitableTable()
                .get(suspended.state().getAwaitedHandleId()).resolve(77L);
        byte[] protoBytes = suspended.toProtoBytes();

        // Restore from protobuf and resume
        SuspendedResult restored = SuspendedResult.fromProtoBytes(protoBytes);
        context.getPolyglotBindings().putMember("resumeState", restored);
        Value result2 = context.eval("relang", src);
        assertEquals(77, result2.asLong());
    }

    @Test
    void testFrameStateWithNewTypes() {
        // Verify FrameState can serialize Double, String, none
        String src = """
                let x = 3.14;
                let s = "hello";
                let n = none;
                checkpoint;
                x;
                """;

        Value result1 = context.eval("relang", src);
        assertTrue(result1.isHostObject());
        SuspendedResult suspended = result1.asHostObject();

        // Verify JSON round-trip
        String json = suspended.toJson();
        SuspendedResult restored = SuspendedResult.fromJson(json);
        context.getPolyglotBindings().putMember("resumeState", restored);
        Value result2 = context.eval("relang", src);
        assertEquals(3.14, result2.asDouble(), 0.001);
    }

    @Test
    void testFrameStateWithNewTypesProto() throws Exception {
        String src = """
                let x = 3.14;
                let s = "hello";
                let n = none;
                checkpoint;
                x;
                """;

        Value result1 = context.eval("relang", src);
        assertTrue(result1.isHostObject());
        SuspendedResult suspended = result1.asHostObject();

        // Verify protobuf round-trip
        byte[] protoBytes = suspended.toProtoBytes();
        SuspendedResult restored = SuspendedResult.fromProtoBytes(protoBytes);
        context.getPolyglotBindings().putMember("resumeState", restored);
        Value result2 = context.eval("relang", src);
        assertEquals(3.14, result2.asDouble(), 0.001);
    }

    @Test
    void testAwaitInNestedFunction() {
        String src = """
                fn inner() {
                    let h = pending();
                    return await h;
                }
                fn outer() {
                    let x = 10;
                    let result = inner();
                    return x + result;
                }
                outer();
                """;

        // First run: suspends in inner() on pending await
        Value result1 = context.eval("relang", src);
        assertTrue(result1.isHostObject(), "Should suspend");
        SuspendedResult suspended = result1.asHostObject();

        // Resolve the awaitable
        suspended.state().getAwaitableTable()
                .get(suspended.state().getAwaitedHandleId()).resolve(5L);

        // Resume -> should complete
        context.getPolyglotBindings().putMember("resumeState", suspended);
        Value result2 = context.eval("relang", src);
        assertEquals(15, result2.asLong()); // 10 + 5
    }

    @Test
    void testMixedCheckpointAndAwait() {
        // A program with both a manual checkpoint and an await
        String src = """
                let x = 10;
                checkpoint;
                let h = pending();
                let y = await h;
                x + y;
                """;

        // First suspension: checkpoint
        Value result1 = context.eval("relang", src);
        assertTrue(result1.isHostObject(), "Should suspend at checkpoint");
        SuspendedResult suspended1 = result1.asHostObject();

        // Resume past checkpoint -> second suspension: await pending
        context.getPolyglotBindings().putMember("resumeState", suspended1);
        Value result2 = context.eval("relang", src);
        assertTrue(result2.isHostObject(), "Should suspend at await");
        SuspendedResult suspended2 = result2.asHostObject();

        // Resolve and resume to completion
        suspended2.state().getAwaitableTable()
                .get(suspended2.state().getAwaitedHandleId()).resolve(20L);
        context.getPolyglotBindings().putMember("resumeState", suspended2);
        Value result3 = context.eval("relang", src);
        assertEquals(30, result3.asLong()); // 10 + 20
    }

    @Test
    void testHashValidationWorksAcrossSerializationFormats() throws Exception {
        String srcOriginal = """
                x = 42;
                checkpoint;
                x;
                """;

        String srcModified = """
                x = 99;
                checkpoint;
                x;
                """;

        // Suspend and serialize to JSON
        Value result1 = context.eval("relang", srcOriginal);
        SuspendedResult original = result1.asHostObject();
        String json = original.toJson();
        byte[] protoBytes = original.toProtoBytes();

        // Deserialize from JSON and try to resume with different code
        SuspendedResult fromJson = SuspendedResult.fromJson(json);
        context.getPolyglotBindings().putMember("resumeState", fromJson);
        PolyglotException jsonEx = assertThrows(
                PolyglotException.class,
                () -> context.eval("relang", srcModified),
                "JSON-deserialized state should validate hash"
        );
        assertTrue(jsonEx.getMessage().contains("StateCodeMismatchException")
                        || jsonEx.getMessage().contains("Source code has changed"),
                "JSON exception should indicate source code mismatch");

        // Deserialize from Proto and try to resume with different code
        SuspendedResult fromProto = SuspendedResult.fromProtoBytes(protoBytes);
        context.getPolyglotBindings().putMember("resumeState", fromProto);
        PolyglotException protoEx = assertThrows(
                PolyglotException.class,
                () -> context.eval("relang", srcModified),
                "Proto-deserialized state should validate hash"
        );
        assertTrue(protoEx.getMessage().contains("StateCodeMismatchException")
                        || protoEx.getMessage().contains("Source code has changed"),
                "Proto exception should indicate source code mismatch");
    }
}

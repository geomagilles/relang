package com.relang;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.relang.nodes.ResumableState;
import com.relang.nodes.SuspendedResult;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
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
        assertNotNull(suspended.getState());

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
        ResumableState state = suspended.getState();
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
        assertNotNull(deserialized.getState());
        assertFalse(deserialized.getState().isEmpty());

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
        assertTrue(original.getState().getFrameCount() >= 2, 
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
        assertTrue(path.size() > 0, "executionPath should not be empty");

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
}

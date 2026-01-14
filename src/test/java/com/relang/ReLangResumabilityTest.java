package com.relang;

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
}

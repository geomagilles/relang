package com.relang;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the native ReLang interpreter.
 * These tests execute the native binary and verify its behavior.
 */
public class NativeInterpreterTest {

    private static final int EX_OK = 0;
    private static final int EX_ERROR = 1;
    private static final int EX_TEMPFAIL = 75;  // Suspended at checkpoint

    private static Path nativeBinary;
    private static Path samplesDir;

    @BeforeAll
    static void setUp() {
        var binaryPath = System.getProperty("relang.native.binary");
        assertNotNull(binaryPath, "relang.native.binary system property must be set");

        nativeBinary = Path.of(binaryPath);
        assertTrue(Files.exists(nativeBinary), "Native binary not found at: " + nativeBinary);
        assertTrue(Files.isExecutable(nativeBinary), "Native binary is not executable: " + nativeBinary);

        samplesDir = Path.of("src/test/resources/samples");
        assertTrue(Files.isDirectory(samplesDir), "Samples directory not found: " + samplesDir);
    }

    @Test
    void testSimpleProgram() throws Exception {
        var result = runRelang(samplesDir.resolve("hello.re"));

        assertEquals(EX_OK, result.exitCode());
        assertTrue(result.stdout().contains("Result: 30"), "Expected result 30, got: " + result.stdout());
    }

    @Test
    void testFibonacci() throws Exception {
        var result = runRelang(samplesDir.resolve("fibonacci.re"));

        assertEquals(EX_OK, result.exitCode());
        assertTrue(result.stdout().contains("Result: 55"), "Expected fibonacci(10) = 55, got: " + result.stdout());
    }

    @Test
    void testFileNotFound() throws Exception {
        var result = runRelang(Path.of("nonexistent.re"));

        assertEquals(EX_ERROR, result.exitCode());
        assertTrue(result.stderr().contains("File not found"), "Expected file not found error, got: " + result.stderr());
    }

    @Test
    void testSyntaxError() throws Exception {
        var result = runRelang(samplesDir.resolve("syntax_error.re"));

        assertEquals(EX_ERROR, result.exitCode());
        assertFalse(result.stderr().isEmpty(), "Expected error output for syntax error");
    }

    @Test
    void testCheckpointSuspendsExecution(@TempDir Path tempDir) throws Exception {
        var stateFile = tempDir.resolve("state.json");

        var result = runRelang(samplesDir.resolve("checkpoint.re"),
                "--state-out", stateFile.toString());

        assertEquals(EX_TEMPFAIL, result.exitCode(), "Expected EX_TEMPFAIL (75) for checkpoint suspension");
        assertTrue(Files.exists(stateFile), "State file should be created");
        assertTrue(result.stdout().contains("State saved to:"), "Should confirm state was saved");

        var stateContent = Files.readString(stateFile);
        assertTrue(stateContent.contains("sourceHash"), "State should contain sourceHash");
        assertTrue(stateContent.contains("frames"), "State should contain frames");
    }

    @Test
    void testCheckpointResume(@TempDir Path tempDir) throws Exception {
        var stateFile = tempDir.resolve("state.json");

        // First run: hits checkpoint and suspends
        var suspendResult = runRelang(samplesDir.resolve("checkpoint.re"),
                "--state-out", stateFile.toString());

        assertEquals(EX_TEMPFAIL, suspendResult.exitCode());
        assertTrue(Files.exists(stateFile));

        // Second run: resume from checkpoint
        var resumeResult = runRelang(samplesDir.resolve("checkpoint.re"),
                "--state-in", stateFile.toString());

        assertEquals(EX_OK, resumeResult.exitCode(), "Resume should complete successfully");
        // process(5): result = 5*2 = 10, then checkpoint, then result = 10+10 = 20
        assertTrue(resumeResult.stdout().contains("Result: 20"),
                "Expected result 20 after resume, got: " + resumeResult.stdout());
    }

    @Test
    void testCheckpointProtobufFormat(@TempDir Path tempDir) throws Exception {
        var stateFile = tempDir.resolve("state.pb");

        // Suspend with protobuf format
        var suspendResult = runRelang(samplesDir.resolve("checkpoint.re"),
                "--state-out", stateFile.toString(),
                "--state-format", "protobuf");

        assertEquals(EX_TEMPFAIL, suspendResult.exitCode());
        assertTrue(Files.exists(stateFile));

        // Resume with protobuf format
        var resumeResult = runRelang(samplesDir.resolve("checkpoint.re"),
                "--state-in", stateFile.toString(),
                "--state-format", "protobuf");

        assertEquals(EX_OK, resumeResult.exitCode());
        assertTrue(resumeResult.stdout().contains("Result: 20"));
    }

    @Test
    void testInvalidStateFormat() throws Exception {
        var result = runRelang(samplesDir.resolve("hello.re"),
                "--state-format", "invalid");

        assertEquals(EX_ERROR, result.exitCode());
        assertTrue(result.stderr().contains("Invalid state format"));
    }

    @Test
    void testHelpOption() throws Exception {
        var result = runRelang("--help");

        assertEquals(EX_OK, result.exitCode());
        assertTrue(result.stdout().contains("ReLang - A resumable programming language"));
        assertTrue(result.stdout().contains("--state-in"));
        assertTrue(result.stdout().contains("--state-out"));
        assertTrue(result.stdout().contains("Exit codes:"));
    }

    @Test
    void testInlineProgram(@TempDir Path tempDir) throws Exception {
        // Create a temporary program file
        var program = tempDir.resolve("inline.re");
        Files.writeString(program, "let x = 42;");

        var result = runRelang(program);

        assertEquals(EX_OK, result.exitCode());
        assertTrue(result.stdout().contains("Result: 42"));
    }

    @Test
    void testArithmeticOperations(@TempDir Path tempDir) throws Exception {
        var program = tempDir.resolve("arithmetic.re");
        Files.writeString(program, """
                let a = 10 + 5;
                let b = a * 2;
                let c = b - 3;
                let result = c / 3;
                """);

        var result = runRelang(program);

        assertEquals(EX_OK, result.exitCode());
        // (10+5)*2 = 30, 30-3 = 27, 27/3 = 9
        assertTrue(result.stdout().contains("Result: 9"), "Got: " + result.stdout());
    }

    @Test
    void testWhileLoop(@TempDir Path tempDir) throws Exception {
        var program = tempDir.resolve("loop.re");
        Files.writeString(program, """
                let sum = 0;
                let i = 1;
                while (i < 6) {
                    sum = sum + i;
                    i = i + 1;
                }
                let result = sum;
                """);

        var result = runRelang(program);

        assertEquals(EX_OK, result.exitCode());
        // 1+2+3+4+5 = 15
        assertTrue(result.stdout().contains("Result: 15"), "Got: " + result.stdout());
    }

    @Test
    void testConditional(@TempDir Path tempDir) throws Exception {
        var program = tempDir.resolve("conditional.re");
        Files.writeString(program, """
                let x = 10;
                let result = 0;
                if (x < 5) {
                    result = 1;
                } else {
                    result = 2;
                }
                """);

        var result = runRelang(program);

        assertEquals(EX_OK, result.exitCode());
        assertTrue(result.stdout().contains("Result: 2"), "Got: " + result.stdout());
    }

    @Test
    void testFunctionDefinitionAndCall(@TempDir Path tempDir) throws Exception {
        var program = tempDir.resolve("function.re");
        Files.writeString(program, """
                fn double(n: Int): Int {
                    return n * 2;
                }
                let result = double(21);
                """);

        var result = runRelang(program);

        assertEquals(EX_OK, result.exitCode());
        assertTrue(result.stdout().contains("Result: 42"), "Got: " + result.stdout());
    }

    @Test
    void testRecursiveFunction(@TempDir Path tempDir) throws Exception {
        var program = tempDir.resolve("factorial.re");
        Files.writeString(program, """
                fn factorial(n: Int): Int {
                    if (n < 2) {
                        return 1;
                    } else {
                        return n * factorial(n - 1);
                    }
                }
                let result = factorial(5);
                """);

        var result = runRelang(program);

        assertEquals(EX_OK, result.exitCode());
        // 5! = 120
        assertTrue(result.stdout().contains("Result: 120"), "Got: " + result.stdout());
    }

    @Test
    void testStateFileNotFound(@TempDir Path tempDir) throws Exception {
        var result = runRelang(samplesDir.resolve("hello.re"),
                "--state-in", tempDir.resolve("nonexistent.json").toString());

        assertEquals(EX_ERROR, result.exitCode());
        assertTrue(result.stderr().contains("State file not found") ||
                        result.stderr().contains("Error loading state"),
                "Expected state file not found error, got: " + result.stderr());
    }

    private ProcessResult runRelang(Path programFile, String... args) throws Exception {
        var command = new String[args.length + 2];
        command[0] = nativeBinary.toString();
        command[1] = programFile.toString();
        System.arraycopy(args, 0, command, 2, args.length);
        return execute(command);
    }

    private ProcessResult runRelang(String... args) throws Exception {
        var command = new String[args.length + 1];
        command[0] = nativeBinary.toString();
        System.arraycopy(args, 0, command, 1, args.length);
        return execute(command);
    }

    private ProcessResult execute(String... command) throws Exception {
        var builder = new ProcessBuilder(command)
                .redirectErrorStream(false);

        var process = builder.start();
        var completed = process.waitFor(30, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            fail("Process timed out after 30 seconds");
        }

        var stdout = new String(process.getInputStream().readAllBytes());
        var stderr = new String(process.getErrorStream().readAllBytes());

        return new ProcessResult(process.exitValue(), stdout, stderr);
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}

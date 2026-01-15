package com.relang.compiler;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for compiled ReLang programs.
 * These tests compile programs using native-image and verify the resulting executables.
 * <p>
 * Note: These tests are slow (~30s per compilation) and require GraalVM with native-image.
 * They are automatically skipped if native-image is not available.
 */
@EnabledIf("isNativeImageAvailable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CompiledProgramTest {

    private static final int EX_OK = 0;
    private static final int EX_ERROR = 1;
    private static final int EX_TEMPFAIL = 75;

    private static String compilerClasspath;
    private static Path tempDir;
    private static Path fibonacciBinary;
    private static Path checkpointBinary;

    static boolean isNativeImageAvailable() {
        return findNativeImage() != null;
    }

    private static String findNativeImage() {
        // Try GRAALVM_HOME
        var graalHome = System.getenv("GRAALVM_HOME");
        if (graalHome != null) {
            var nativeImage = Path.of(graalHome, "bin", "native-image");
            if (Files.isExecutable(nativeImage)) {
                return nativeImage.toString();
            }
        }

        // Try JAVA_HOME
        var javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            var nativeImage = Path.of(javaHome, "bin", "native-image");
            if (Files.isExecutable(nativeImage)) {
                return nativeImage.toString();
            }
        }

        // Try PATH
        var path = System.getenv("PATH");
        if (path != null) {
            for (var dir : path.split(File.pathSeparator)) {
                var nativeImage = Path.of(dir, "native-image");
                if (Files.isExecutable(nativeImage)) {
                    return nativeImage.toString();
                }
            }
        }

        return null;
    }

    @BeforeAll
    static void setUp(@TempDir Path temp) throws Exception {
        tempDir = temp;

        compilerClasspath = System.getProperty("relang.compiler.classpath");
        assertNotNull(compilerClasspath, "relang.compiler.classpath system property must be set");

        Path samplesDir = Path.of("src/test/resources/samples");
        assertTrue(Files.isDirectory(samplesDir), "Samples directory not found: " + samplesDir);

        // Compile test programs once (this is slow but we reuse them)
        System.out.println("Compiling fibonacci program...");
        fibonacciBinary = compileProgram(samplesDir.resolve("fibonacci.re"), "fibonacci");
        assertNotNull(fibonacciBinary, "Fibonacci binary should be compiled");

        System.out.println("Compiling checkpoint program...");
        checkpointBinary = compileProgram(samplesDir.resolve("checkpoint.re"), "checkpoint");
        assertNotNull(checkpointBinary, "Checkpoint binary should be compiled");
    }

    private static Path compileProgram(Path source, String outputName) throws Exception {
        var outputPath = tempDir.resolve(outputName);

        var command = new String[]{
                "java", "-cp", compilerClasspath,
                "com.relang.compiler.ReLangCompiler",
                source.toString(),
                "-o", outputPath.toString()
        };

        var builder = new ProcessBuilder(command)
                .redirectErrorStream(false);

        var process = builder.start();
        var completed = process.waitFor(120, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            fail("Compilation timed out after 120 seconds");
        }

        var stdout = new String(process.getInputStream().readAllBytes());
        var stderr = new String(process.getErrorStream().readAllBytes());

        if (process.exitValue() != 0) {
            System.err.println("Compilation failed:");
            System.err.println("stdout: " + stdout);
            System.err.println("stderr: " + stderr);
            fail("Compilation failed with exit code: " + process.exitValue());
        }

        assertTrue(Files.exists(outputPath), "Compiled binary should exist: " + outputPath);
        assertTrue(Files.isExecutable(outputPath), "Compiled binary should be executable: " + outputPath);

        return outputPath;
    }

    @Test
    @Order(1)
    void testFibonacciExecution() throws Exception {
        var result = runProgram(fibonacciBinary);

        assertEquals(EX_OK, result.exitCode(), "Exit code should be 0");
        assertTrue(result.stdout().contains("Result: 55"),
                "Expected fibonacci(10) = 55, got: " + result.stdout());
    }

    @Test
    @Order(2)
    void testCompiledProgramHelp() throws Exception {
        var result = runProgram(fibonacciBinary, "--help");

        assertEquals(EX_OK, result.exitCode());
        assertTrue(result.stdout().contains("Compiled ReLang program"));
        assertTrue(result.stdout().contains("--state-in"));
        assertTrue(result.stdout().contains("--state-out"));
        assertTrue(result.stdout().contains("Exit codes"));
    }

    @Test
    @Order(3)
    void testCheckpointSuspension() throws Exception {
        var stateFile = tempDir.resolve("state.json");

        var result = runProgram(checkpointBinary, "--state-out", stateFile.toString());

        assertEquals(EX_TEMPFAIL, result.exitCode(), "Should suspend at checkpoint with exit code 75");
        assertTrue(Files.exists(stateFile), "State file should be created");
        assertTrue(result.stdout().contains("State saved to:"), "Should confirm state was saved");

        var stateContent = Files.readString(stateFile);
        assertTrue(stateContent.contains("sourceHash"), "State should contain sourceHash");
        assertTrue(stateContent.contains("frames"), "State should contain frames");
    }

    @Test
    @Order(4)
    void testCheckpointResume() throws Exception {
        var stateFile = tempDir.resolve("state_resume.json");

        // First run: suspend at checkpoint
        var suspendResult = runProgram(checkpointBinary, "--state-out", stateFile.toString());
        assertEquals(EX_TEMPFAIL, suspendResult.exitCode());
        assertTrue(Files.exists(stateFile));

        // Second run: resume from checkpoint
        var resumeResult = runProgram(checkpointBinary, "--state-in", stateFile.toString());

        assertEquals(EX_OK, resumeResult.exitCode(), "Resume should complete successfully");
        // process(5): result = 5*2 = 10, then checkpoint, then result = 10+10 = 20
        assertTrue(resumeResult.stdout().contains("Result: 20"),
                "Expected result 20 after resume, got: " + resumeResult.stdout());
    }

    @Test
    @Order(5)
    void testProtobufStateFormat() throws Exception {
        var stateFile = tempDir.resolve("state.pb");

        // Suspend with protobuf format
        var suspendResult = runProgram(checkpointBinary,
                "--state-out", stateFile.toString(),
                "--state-format", "protobuf");

        assertEquals(EX_TEMPFAIL, suspendResult.exitCode());
        assertTrue(Files.exists(stateFile));

        // Resume with protobuf format
        var resumeResult = runProgram(checkpointBinary,
                "--state-in", stateFile.toString(),
                "--state-format", "protobuf");

        assertEquals(EX_OK, resumeResult.exitCode());
        assertTrue(resumeResult.stdout().contains("Result: 20"));
    }

    @Test
    @Order(6)
    void testInvalidStateFormat() throws Exception {
        var result = runProgram(fibonacciBinary, "--state-format", "invalid");

        assertEquals(EX_ERROR, result.exitCode());
        assertTrue(result.stderr().contains("Invalid state format"));
    }

    @Test
    @Order(7)
    void testStateFileNotFound() throws Exception {
        var result = runProgram(fibonacciBinary,
                "--state-in", tempDir.resolve("nonexistent.json").toString());

        assertEquals(EX_ERROR, result.exitCode());
        assertTrue(result.stderr().contains("State file not found") ||
                        result.stderr().contains("Error loading state"),
                "Expected state file not found error, got: " + result.stderr());
    }

    @Test
    @Order(8)
    void testUnknownOption() throws Exception {
        var result = runProgram(fibonacciBinary, "--unknown-option");

        assertEquals(EX_ERROR, result.exitCode());
        assertTrue(result.stderr().contains("Unknown option"),
                "Expected unknown option error, got: " + result.stderr());
    }

    private ProcessResult runProgram(Path binary, String... args) throws Exception {
        var command = new String[args.length + 1];
        command[0] = binary.toString();
        System.arraycopy(args, 0, command, 1, args.length);

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

    private record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}

package com.relang.compiler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ReLang compiler CLI.
 * These tests verify argument parsing, help output, and syntax validation
 * without actually running native-image (to keep tests fast).
 */
public class CompilerTest {

    private static String compilerClasspath;
    private static Path samplesDir;

    @BeforeAll
    static void setUp() {
        compilerClasspath = System.getProperty("relang.compiler.classpath");
        assertNotNull(compilerClasspath, "relang.compiler.classpath system property must be set");

        samplesDir = Path.of("src/test/resources/samples");
        assertTrue(Files.isDirectory(samplesDir), "Samples directory not found: " + samplesDir);
    }

    @Test
    void testHelpOption() throws Exception {
        var result = runCompiler("--help");

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("ReLang Compiler"));
        assertTrue(result.stdout().contains("-o, --output"));
        assertTrue(result.stdout().contains("-v, --verbose"));
        assertTrue(result.stdout().contains("--help"));
    }

    @Test
    void testShortHelpOption() throws Exception {
        var result = runCompiler("-h");

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("ReLang Compiler"));
    }

    @Test
    void testNoArguments() throws Exception {
        var result = runCompiler();

        assertEquals(1, result.exitCode());
        assertTrue(result.stdout().contains("ReLang Compiler") || result.stderr().contains("No input file"),
                "Expected help or error message");
    }

    @Test
    void testFileNotFound() throws Exception {
        var result = runCompiler("nonexistent.re");

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("not found") || result.stderr().contains("Input file"),
                "Expected file not found error, got: " + result.stderr());
    }

    @Test
    void testSyntaxError() throws Exception {
        var result = runCompiler(samplesDir.resolve("syntax_error.re").toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Syntax error") || result.stderr().contains("error"),
                "Expected syntax error, got: " + result.stderr());
    }

    @Test
    void testUnknownOption() throws Exception {
        var result = runCompiler("--unknown-option", "input.re");

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Unknown option"),
                "Expected unknown option error, got: " + result.stderr());
    }

    @Test
    void testMultipleInputFiles() throws Exception {
        var result = runCompiler("file1.re", "file2.re");

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Multiple input files"),
                "Expected multiple input files error, got: " + result.stderr());
    }

    @Test
    void testOutputOptionWithoutValue() throws Exception {
        var result = runCompiler("-o");

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("requires") || result.stderr().contains("output"),
                "Expected output option error, got: " + result.stderr());
    }

    @Test
    void testValidSyntaxShowsProgress(@TempDir Path tempDir) throws Exception {
        // This test will fail when trying to find native-image, but that's OK
        // We just want to verify that syntax validation passes
        var result = runCompiler(
                samplesDir.resolve("hello.re").toString(),
                "-o", tempDir.resolve("test").toString());

        // Should fail with native-image not found (unless running on GraalVM)
        // but should NOT fail with syntax error
        assertFalse(result.stderr().contains("Syntax error"),
                "Should not have syntax error for valid program: " + result.stderr());
    }

    private ProcessResult runCompiler(String... args) throws Exception {
        var command = new String[args.length + 4];
        command[0] = "java";
        command[1] = "-cp";
        command[2] = compilerClasspath;
        command[3] = "com.relang.compiler.ReLangCompiler";
        System.arraycopy(args, 0, command, 4, args.length);

        var builder = new ProcessBuilder(command)
                .redirectErrorStream(false);

        var process = builder.start();
        var completed = process.waitFor(60, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            fail("Process timed out after 60 seconds");
        }

        var stdout = new String(process.getInputStream().readAllBytes());
        var stderr = new String(process.getErrorStream().readAllBytes());

        return new ProcessResult(process.exitValue(), stdout, stderr);
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}

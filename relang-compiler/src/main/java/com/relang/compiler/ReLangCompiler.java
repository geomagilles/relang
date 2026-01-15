package com.relang.compiler;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * Compiler for ReLang programs.
 * Produces standalone native executables from .re source files.
 * <p>
 * Requires: GraalVM with native-image installed and GRAALVM_HOME set,
 * or native-image available in PATH.
 */
public class ReLangCompiler {

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            System.exit(1);
        }

        String inputFile = null;
        String outputFile = null;
        boolean verbose = false;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        outputFile = args[++i];
                    } else {
                        System.err.println("Error: -o requires an output file name");
                        System.exit(1);
                    }
                    break;
                case "-v":
                case "--verbose":
                    verbose = true;
                    break;
                case "-h":
                case "--help":
                    printHelp();
                    return;
                default:
                    if (args[i].startsWith("-")) {
                        System.err.println("Unknown option: " + args[i]);
                        System.exit(1);
                    } else if (inputFile == null) {
                        inputFile = args[i];
                    } else {
                        System.err.println("Error: Multiple input files specified");
                        System.exit(1);
                    }
                    break;
            }
        }

        if (inputFile == null) {
            System.err.println("Error: No input file specified");
            System.exit(1);
        }

        // Derive output name from input if not specified
        if (outputFile == null) {
            String baseName = new File(inputFile).getName();
            if (baseName.endsWith(".re")) {
                baseName = baseName.substring(0, baseName.length() - 3);
            }
            outputFile = baseName;
        }

        try {
            compile(inputFile, outputFile, verbose);
        } catch (CompilationException e) {
            System.err.println("Compilation error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    public static void compile(String inputFile, String outputFile, boolean verbose)
            throws CompilationException, IOException, InterruptedException {

        Path inputPath = Path.of(inputFile);
        if (!Files.exists(inputPath)) {
            throw new CompilationException("Input file not found: " + inputFile);
        }

        // Read and validate source
        String sourceCode = Files.readString(inputPath);
        if (verbose) {
            System.out.println("Validating syntax...");
        }
        validateSyntax(sourceCode);

        // Find native-image command
        String nativeImageCmd = findNativeImage();
        if (nativeImageCmd == null) {
            throw new CompilationException(
                    "native-image not found. Please ensure GraalVM is installed and either:\n" +
                            "  - GRAALVM_HOME is set, or\n" +
                            "  - native-image is in PATH");
        }

        if (verbose) {
            System.out.println("Using native-image: " + nativeImageCmd);
        }

        // Create temporary directory
        Path tempDir = Files.createTempDirectory("relangc-");
        try {
            // Write embedded source as a resource file
            Path resourceDir = tempDir.resolve("resources");
            Files.createDirectories(resourceDir);
            Files.writeString(resourceDir.resolve("embedded_source.re"), sourceCode);

            // Create a JAR with the embedded source
            Path jarFile = tempDir.resolve("app.jar");
            createJarWithSource(jarFile, resourceDir);

            if (verbose) {
                System.out.println("Building native executable...");
            }

            // Build classpath from current runtime
            String classpath = buildClasspath(jarFile);

            // Run native-image
            List<String> command = new ArrayList<>();
            command.add(nativeImageCmd);
            command.add("--no-fallback");
            command.add("-H:+ReportExceptionStackTraces");
            command.add("-H:+UnlockExperimentalVMOptions");
            // Initialize Truffle and ReLang classes at build time
            command.add("--initialize-at-build-time=com.relang");
            command.add("--initialize-at-build-time=org.antlr.v4.runtime");
            command.add("--initialize-at-build-time=com.google.gson");
            command.add("--initialize-at-build-time=com.google.protobuf");
            command.add("-cp");
            command.add(classpath);
            command.add("-o");
            command.add(outputFile);
            command.add("com.relang.compiler.EmbeddedRunner");

            if (verbose) {
                System.out.println("Command: " + String.join(" ", command));
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new CompilationException("native-image failed with exit code: " + exitCode);
            }

            System.out.println("Compiled: " + Path.of(outputFile).toAbsolutePath());

        } finally {
            // Clean up temp directory
            if (!verbose) {
                deleteDirectory(tempDir);
            } else {
                System.out.println("Temp directory preserved: " + tempDir);
            }
        }
    }

    private static void validateSyntax(String sourceCode) throws CompilationException {
        try (Context context = Context.newBuilder("relang")
                .allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false")
                .build()) {

            Source source = Source.newBuilder("relang", sourceCode, "input.re").build();
            context.parse(source);

        } catch (Exception e) {
            throw new CompilationException("Syntax error: " + e.getMessage());
        }
    }

    private static String findNativeImage() {
        // Try GRAALVM_HOME first
        String graalHome = System.getenv("GRAALVM_HOME");
        if (graalHome != null) {
            Path nativeImage = Path.of(graalHome, "bin", "native-image");
            if (Files.isExecutable(nativeImage)) {
                return nativeImage.toString();
            }
        }

        // Try JAVA_HOME (might be GraalVM)
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            Path nativeImage = Path.of(javaHome, "bin", "native-image");
            if (Files.isExecutable(nativeImage)) {
                return nativeImage.toString();
            }
        }

        // Try PATH
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                Path nativeImage = Path.of(dir, "native-image");
                if (Files.isExecutable(nativeImage)) {
                    return nativeImage.toString();
                }
            }
        }

        return null;
    }

    private static void createJarWithSource(Path jarFile, Path resourceDir) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS,
                "com.relang.compiler.EmbeddedRunner");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()), manifest)) {
            // Add the embedded source
            Path sourceFile = resourceDir.resolve("embedded_source.re");
            jos.putNextEntry(new JarEntry("embedded_source.re"));
            Files.copy(sourceFile, jos);
            jos.closeEntry();
        }
    }

    private static String buildClasspath(Path additionalJar) throws CompilationException {
        List<String> classpathEntries = new ArrayList<>();

        // Add the additional JAR with embedded source
        classpathEntries.add(additionalJar.toString());

        // Get classpath from system property (when running from Gradle/IDE)
        String javaClasspath = System.getProperty("java.class.path");
        if (javaClasspath != null && !javaClasspath.isEmpty()) {
            for (String entry : javaClasspath.split(File.pathSeparator)) {
                if (!classpathEntries.contains(entry)) {
                    classpathEntries.add(entry);
                }
            }
        }

        if (classpathEntries.size() <= 1) {
            throw new CompilationException("Could not determine runtime classpath");
        }

        return String.join(File.pathSeparator, classpathEntries);
    }

    private static void deleteDirectory(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private static void printHelp() {
        System.out.println("ReLang Compiler - Compile .re files to native executables");
        System.out.println();
        System.out.println("Usage: relangc [options] <input.re>");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -o, --output <file>   Output executable name (default: input name without .re)");
        System.out.println("  -v, --verbose         Verbose output");
        System.out.println("  -h, --help            Show this help");
        System.out.println();
        System.out.println("Requirements:");
        System.out.println("  GraalVM with native-image installed.");
        System.out.println("  Set GRAALVM_HOME or ensure native-image is in PATH.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  relangc program.re                Compile to ./program");
        System.out.println("  relangc program.re -o myapp       Compile to ./myapp");
        System.out.println("  relangc -v program.re             Compile with verbose output");
    }

    public static class CompilationException extends Exception {
        public CompilationException(String message) {
            super(message);
        }
    }
}

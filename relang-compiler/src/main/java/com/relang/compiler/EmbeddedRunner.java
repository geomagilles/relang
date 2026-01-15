package com.relang.compiler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.relang.nodes.SuspendedResult;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generic runner for compiled ReLang executables.
 * The source code is embedded as a resource and executed at runtime.
 */
public class EmbeddedRunner {

    // Exit codes following sysexits.h conventions
    public static final int EX_OK = 0;
    public static final int EX_ERROR = 1;
    public static final int EX_TEMPFAIL = 75;  // Suspended at checkpoint

    private static final String EMBEDDED_SOURCE_RESOURCE = "/embedded_source.re";

    public static void main(String[] args) {
        String stateIn = null;
        String stateOut = null;
        String stateFormat = "json";

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--state-in":
                    if (i + 1 < args.length) {
                        stateIn = args[++i];
                    }
                    break;
                case "--state-out":
                    if (i + 1 < args.length) {
                        stateOut = args[++i];
                    }
                    break;
                case "--state-format":
                    if (i + 1 < args.length) {
                        stateFormat = args[++i];
                        if (!stateFormat.equals("json") && !stateFormat.equals("protobuf")) {
                            System.err.println("Invalid state format: " + stateFormat);
                            System.err.println("Valid formats: json, protobuf");
                            System.exit(EX_ERROR);
                        }
                    }
                    break;
                case "--help":
                case "-h":
                    printHelp();
                    return;
                default:
                    if (args[i].startsWith("-")) {
                        System.err.println("Unknown option: " + args[i]);
                        System.exit(EX_ERROR);
                    }
                    break;
            }
        }

        int exitCode = run(stateIn, stateOut, stateFormat);
        System.exit(exitCode);
    }

    private static int run(String stateIn, String stateOut, String stateFormat) {
        // Load embedded source
        String sourceCode;
        try {
            sourceCode = loadEmbeddedSource();
        } catch (IOException e) {
            System.err.println("Error loading embedded source: " + e.getMessage());
            return EX_ERROR;
        }

        // Load state if specified
        SuspendedResult resumeState = null;
        if (stateIn != null) {
            try {
                resumeState = loadState(stateIn, stateFormat);
            } catch (Exception e) {
                System.err.println("Error loading state: " + e.getMessage());
                return EX_ERROR;
            }
        }

        try (Context context = Context.newBuilder("relang")
                .allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false")
                .build()) {

            // Inject resume state if available
            if (resumeState != null) {
                context.getPolyglotBindings().putMember("resumeState", resumeState);
            }

            Source source = Source.newBuilder("relang", sourceCode, "embedded.re").build();
            Value result = context.eval(source);

            // Check if execution was suspended
            if (result != null && result.isHostObject()) {
                Object hostObj = result.asHostObject();
                if (hostObj instanceof SuspendedResult suspended) {
                    // Save state if output file specified
                    if (stateOut != null) {
                        try {
                            saveState(suspended, stateOut, stateFormat);
                            System.out.println("State saved to: " + stateOut);
                        } catch (Exception e) {
                            System.err.println("Error saving state: " + e.getMessage());
                            return EX_ERROR;
                        }
                    }
                    return EX_TEMPFAIL;  // Suspended
                }
            }

            if (result != null && !result.isNull()) {
                System.out.println("Result: " + result);
            }
            return EX_OK;

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return EX_ERROR;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return EX_ERROR;
        }
    }

    private static String loadEmbeddedSource() throws IOException {
        try (InputStream is = EmbeddedRunner.class.getResourceAsStream(EMBEDDED_SOURCE_RESOURCE)) {
            if (is == null) {
                throw new IOException("Embedded source not found");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        }
    }

    private static SuspendedResult loadState(String path, String format) throws IOException {
        Path stateFile = Path.of(path);
        if (!Files.exists(stateFile)) {
            throw new IOException("State file not found: " + path);
        }

        if (format.equals("protobuf")) {
            byte[] bytes = Files.readAllBytes(stateFile);
            try {
                return SuspendedResult.fromProtoBytes(bytes);
            } catch (InvalidProtocolBufferException e) {
                throw new IOException("Invalid protobuf state file: " + e.getMessage());
            }
        } else {
            String json = Files.readString(stateFile);
            return SuspendedResult.fromJson(json);
        }
    }

    private static void saveState(SuspendedResult state, String path, String format) throws IOException {
        Path stateFile = Path.of(path);

        if (format.equals("protobuf")) {
            Files.write(stateFile, state.toProtoBytes());
        } else {
            Files.writeString(stateFile, state.toJson());
        }
    }

    private static void printHelp() {
        System.out.println("Compiled ReLang program");
        System.out.println();
        System.out.println("Usage: <program> [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --state-in <file>     Load execution state before running");
        System.out.println("  --state-out <file>    Save state when checkpoint is hit");
        System.out.println("  --state-format <fmt>  State format: json (default) | protobuf");
        System.out.println("  --help, -h            Show this help");
        System.out.println();
        System.out.println("Exit codes:");
        System.out.println("  0   Program completed successfully");
        System.out.println("  1   Runtime or parse error");
        System.out.println("  75  Suspended at checkpoint (state saved)");
    }
}

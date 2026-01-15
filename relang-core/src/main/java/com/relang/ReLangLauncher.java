package com.relang;

import com.google.protobuf.InvalidProtocolBufferException;
import com.relang.nodes.SuspendedResult;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedReader;

public class ReLangLauncher {

    // Exit codes following sysexits.h conventions
    public static final int EX_OK = 0;
    public static final int EX_ERROR = 1;
    public static final int EX_TEMPFAIL = 75;  // Suspended at checkpoint

    public static void main(String[] args) {
        boolean lspMode = false;
        boolean inspectMode = false;
        String filePath = null;
        String stateIn = null;
        String stateOut = null;
        String stateFormat = "json";
        int lspPort = 8123;
        int inspectPort = 4711;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--lsp":
                    lspMode = true;
                    break;
                case "--lsp.port":
                    if (i + 1 < args.length) {
                        lspPort = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--inspect":
                    inspectMode = true;
                    break;
                case "--inspect.port":
                    if (i + 1 < args.length) {
                        inspectPort = Integer.parseInt(args[++i]);
                    }
                    break;
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
                    if (!args[i].startsWith("-")) {
                        filePath = args[i];
                    }
                    break;
            }
        }

        if (lspMode) {
            startLspServer(lspPort);
        } else if (filePath != null) {
            int exitCode = runFile(filePath, inspectMode, inspectPort, stateIn, stateOut, stateFormat);
            System.exit(exitCode);
        } else {
            runRepl();
        }
    }

    private static void startLspServer(int port) {
        System.out.println("Starting ReLang LSP server on port " + port + "...");
        System.out.println("Connect your IDE to localhost:" + port);
        System.out.println("Press Ctrl+C to stop.");

        try (Context context = Context.newBuilder("relang")
                .allowAllAccess(true)
                .allowExperimentalOptions(true)
                .option("lsp", "true")
                .option("lsp.Delegates", "")
                .build()) {

            // Keep the context alive for LSP
            synchronized (context) {
                try {
                    context.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to start LSP server: " + e.getMessage());
            System.err.println("\nNote: LSP requires GraalVM with tools support.");
            System.err.println("Make sure you're using GraalVM and have the 'lsp' tool available.");
            e.printStackTrace();
        }
    }

    private static int runFile(String filePath, boolean inspect, int inspectPort,
                                String stateIn, String stateOut, String stateFormat) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("File not found: " + filePath);
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

        Context.Builder builder = Context.newBuilder("relang")
                .allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false");

        if (inspect) {
            builder.allowExperimentalOptions(true)
                   .option("inspect", "true")
                   .option("inspect.Port", String.valueOf(inspectPort));
            System.out.println("Debugger listening on port " + inspectPort);
            System.out.println("Connect Chrome DevTools to: chrome://inspect");
        }

        try (Context context = builder.build()) {
            // Inject resume state if available
            if (resumeState != null) {
                context.getPolyglotBindings().putMember("resumeState", resumeState);
            }

            Source source = Source.newBuilder("relang", file).build();
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
            System.err.println("Error reading file: " + e.getMessage());
            return EX_ERROR;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return EX_ERROR;
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

    private static void runRepl() {
        System.out.println("ReLang REPL (type 'exit' to quit)");
        System.out.println("================================");

        try (Context context = Context.newBuilder("relang")
                .allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            String line;
            while (true) {
                System.out.print("relang> ");
                line = reader.readLine();

                if (line == null || line.equals("exit")) {
                    break;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    Value result = context.eval("relang", line);
                    if (result != null && !result.isNull()) {
                        System.out.println("=> " + result);
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("ReLang - A resumable programming language");
        System.out.println();
        System.out.println("Usage: relang [options] [file.re]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --state-in <file>     Load execution state before running");
        System.out.println("  --state-out <file>    Save state when checkpoint is hit");
        System.out.println("  --state-format <fmt>  State format: json (default) | protobuf");
        System.out.println("  --lsp                 Start LSP server (for IDE integration)");
        System.out.println("  --lsp.port <port>     LSP server port (default: 8123)");
        System.out.println("  --inspect             Enable debugger");
        System.out.println("  --inspect.port <p>    Debugger port (default: 4711)");
        System.out.println("  --help, -h            Show this help");
        System.out.println();
        System.out.println("Exit codes:");
        System.out.println("  0   Program completed successfully");
        System.out.println("  1   Runtime or parse error");
        System.out.println("  75  Suspended at checkpoint (state saved)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  relang program.re                          Run a file");
        System.out.println("  relang program.re --state-out state.json   Run with checkpointing");
        System.out.println("  relang program.re --state-in state.json    Resume from checkpoint");
        System.out.println("  relang --lsp                               Start LSP server");
        System.out.println("  relang --inspect prog.re                   Run with debugger");
        System.out.println("  relang                                     Start REPL");
    }
}

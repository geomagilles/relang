package com.relang;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class ReLangLauncher {

    public static void main(String[] args) {
        boolean lspMode = false;
        boolean inspectMode = false;
        String filePath = null;
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
            runFile(filePath, inspectMode, inspectPort);
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

    private static void runFile(String filePath, boolean inspect, int inspectPort) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("File not found: " + filePath);
            System.exit(1);
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
            Source source = Source.newBuilder("relang", file).build();
            Value result = context.eval(source);

            if (result != null && !result.isNull()) {
                System.out.println("Result: " + result);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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
        System.out.println("  --lsp              Start LSP server (for IDE integration)");
        System.out.println("  --lsp.port <port>  LSP server port (default: 8123)");
        System.out.println("  --inspect          Enable debugger");
        System.out.println("  --inspect.port <p> Debugger port (default: 4711)");
        System.out.println("  --help, -h         Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  relang program.re          Run a file");
        System.out.println("  relang --lsp               Start LSP server");
        System.out.println("  relang --inspect prog.re   Run with debugger");
        System.out.println("  relang                     Start REPL");
    }
}

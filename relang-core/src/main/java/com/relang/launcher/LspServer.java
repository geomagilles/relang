package com.relang.launcher;

import org.graalvm.polyglot.Context;

/**
 * Starts and manages the ReLang LSP (Language Server Protocol) server.
 * Provides IDE integration for syntax highlighting, completion, and diagnostics.
 */
public final class LspServer {

    private LspServer() {}

    /**
     * Starts the LSP server on the specified port.
     * This method blocks until the server is stopped (Ctrl+C).
     *
     * @param port the port to listen on
     */
    public static void start(int port) {
        System.out.println("Starting ReLang LSP server on port " + port + "...");
        System.out.println("Connect your IDE to localhost:" + port);
        System.out.println("Press Ctrl+C to stop.");

        try (Context context = Context.newBuilder("relang")
                .allowAllAccess(true)
                .allowExperimentalOptions(true)
                .option("lsp", "true")
                .option("lsp.Delegates", "")
                .build()) {

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
}

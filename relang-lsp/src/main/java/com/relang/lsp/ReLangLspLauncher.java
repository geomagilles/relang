package com.relang.lsp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * Entry point for the standalone ReLang LSP server.
 */
public final class ReLangLspLauncher {

    private static final int DEFAULT_PORT = 8123;
    private static final String DEFAULT_HOST = "127.0.0.1";

    private ReLangLspLauncher() {}

    public static void main(String[] args) {
        var parsed = parseArgs(args);
        if (parsed == null) {
            return;
        }

        startServer(parsed.host, parsed.port);
    }

    private record ParsedArgs(
            String host,
            int port
    ) {}

    private static ParsedArgs parseArgs(String[] args) {
        var host = DEFAULT_HOST;
        var port = DEFAULT_PORT;

        for (var i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--host" -> {
                    if (i + 1 >= args.length) {
                        fail("Missing value for --host");
                    }
                    host = args[++i];
                }
                case "--port" -> {
                    if (i + 1 >= args.length) {
                        fail("Missing value for --port");
                    }
                    port = parsePort(args[++i]);
                }
                case "--help", "-h" -> {
                    printHelp();
                    return null;
                }
                default -> fail("Unknown option: " + args[i]);
            }
        }

        return new ParsedArgs(host, port);
    }

    private static int parsePort(String value) {
        try {
            var port = Integer.parseInt(value);
            if (port <= 0 || port > 65535) {
                fail("Port out of range: " + value);
            }
            return port;
        } catch (NumberFormatException e) {
            fail("Invalid port: " + value);
            return DEFAULT_PORT;
        }
    }

    private static void fail(String message) {
        System.err.println(message);
        printHelp();
        System.exit(1);
    }

    private static void startServer(String host, int port) {
        var address = host + ":" + port;
        System.out.println("Starting ReLang LSP server on " + address + "...");
        System.out.println("Press Ctrl+C to stop.");

        try (var serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(host, port));
            while (true) {
                var client = serverSocket.accept();
                Thread.ofPlatform()
                        .name("relang-lsp-session")
                        .daemon(true)
                        .start(() -> runSession(client));
            }
        } catch (Exception e) {
            System.err.println("Failed to start LSP server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runSession(java.net.Socket client) {
        try (var session = new ReLangLspSession(client)) {
            session.run();
        } catch (IOException e) {
            System.err.println("LSP session failed: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("""
                ReLang LSP Server

                Usage: relang-lsp [options]

                Options:
                  --host <host>  Host to bind (default: 127.0.0.1)
                  --port <port>  Port to bind (default: 8123)
                  --help, -h     Show this help

                Examples:
                  ./gradlew :relang-lsp:run
                  ./gradlew :relang-lsp:run --args="--port 8123"
                  ./gradlew :relang-lsp:run --args="--host 127.0.0.1 --port 8124"
                """);
    }
}

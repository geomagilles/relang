package com.relang;

import com.relang.launcher.ExitCodes;
import com.relang.launcher.FileRunner;
import com.relang.launcher.Repl;

/**
 * Main entry point for the ReLang interpreter.
 * Dispatches to REPL or file execution based on command-line arguments.
 */
public class ReLangLauncher {

    /** @deprecated Use {@link ExitCodes#OK} instead. */
    @Deprecated(forRemoval = true)
    public static final int EX_OK = ExitCodes.OK;

    /** @deprecated Use {@link ExitCodes#ERROR} instead. */
    @Deprecated(forRemoval = true)
    public static final int EX_ERROR = ExitCodes.ERROR;

    /** @deprecated Use {@link ExitCodes#SUSPENDED} instead. */
    @Deprecated(forRemoval = true)
    public static final int EX_TEMPFAIL = ExitCodes.SUSPENDED;

    public static void main(String[] args) {
        var parsed = parseArgs(args);
        if (parsed == null) {
            return; // Help was printed
        }

        if (parsed.filePath != null) {
            var config = new FileRunner.Config(
                    parsed.filePath,
                    parsed.inspect,
                    parsed.inspectPort,
                    parsed.stateIn,
                    parsed.stateOut,
                    parsed.stateFormat
            );
            System.exit(FileRunner.run(config));
        } else {
            Repl.run();
        }
    }

    private record ParsedArgs(
            boolean inspect,
            int inspectPort,
            String filePath,
            String stateIn,
            String stateOut,
            String stateFormat
    ) {}

    private static ParsedArgs parseArgs(String[] args) {
        boolean inspect = false;
        int inspectPort = 4711;
        String filePath = null;
        String stateIn = null;
        String stateOut = null;
        String stateFormat = "json";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--inspect" -> inspect = true;
                case "--inspect.port" -> {
                    if (i + 1 < args.length) inspectPort = Integer.parseInt(args[++i]);
                }
                case "--state-in" -> {
                    if (i + 1 < args.length) stateIn = args[++i];
                }
                case "--state-out" -> {
                    if (i + 1 < args.length) stateOut = args[++i];
                }
                case "--state-format" -> {
                    if (i + 1 < args.length) {
                        stateFormat = args[++i];
                        if (!stateFormat.equals("json") && !stateFormat.equals("protobuf")) {
                            System.err.println("Invalid state format: " + stateFormat);
                            System.err.println("Valid formats: json, protobuf");
                            System.exit(ExitCodes.ERROR);
                        }
                    }
                }
                case "--help", "-h" -> {
                    printHelp();
                    return null;
                }
                default -> {
                    if (args[i].startsWith("-")) {
                        System.err.println("Unknown option: " + args[i]);
                        System.err.println("LSP is now started via the relang-lsp module.");
                        System.exit(ExitCodes.ERROR);
                    } else {
                        filePath = args[i];
                    }
                }
            }
        }

        return new ParsedArgs(inspect, inspectPort, filePath, stateIn, stateOut, stateFormat);
    }

    private static void printHelp() {
        System.out.println("""
                ReLang - A resumable programming language

                Usage: relang [options] [file.re]

                Options:
                  --state-in <file>     Load execution state before running
                  --state-out <file>    Save state when checkpoint is hit
                  --state-format <fmt>  State format: json (default) | protobuf
                  --inspect             Enable debugger
                  --inspect.port <p>    Debugger port (default: 4711)
                  --help, -h            Show this help

                Exit codes:
                  0   Program completed successfully
                  1   Runtime or parse error
                  75  Suspended at checkpoint (state saved)

                Examples:
                  relang program.re                          Run a file
                  relang program.re --state-out state.json   Run with checkpointing
                  relang program.re --state-in state.json    Resume from checkpoint
                  relang --inspect prog.re                   Run with debugger
                  relang                                     Start REPL""");
    }
}

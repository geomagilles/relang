package com.relang.launcher;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Interactive REPL (Read-Eval-Print Loop) for ReLang.
 * Allows users to evaluate ReLang expressions interactively.
 */
public final class Repl {

    private static final String PROMPT = "relang> ";
    private static final String EXIT_COMMAND = "exit";

    private Repl() {}

    /**
     * Starts the interactive REPL session.
     * Exits when the user types "exit" or EOF is reached.
     */
    public static void run() {
        System.out.println("ReLang REPL (type 'exit' to quit)");
        System.out.println("================================");

        try (Context context = Context.newBuilder("relang")
                .allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            String line;
            while (true) {
                System.out.print(PROMPT);
                line = reader.readLine();

                if (line == null || EXIT_COMMAND.equals(line)) {
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
}

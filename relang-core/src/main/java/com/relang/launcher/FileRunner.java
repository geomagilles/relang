package com.relang.launcher;

import com.google.protobuf.InvalidProtocolBufferException;
import com.relang.nodes.SuspendedResult;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Executes ReLang source files with support for checkpointing and resumption.
 */
public final class FileRunner {

    private FileRunner() {}

    /**
     * Configuration for file execution.
     */
    public record Config(
            String filePath,
            boolean inspect,
            int inspectPort,
            String stateIn,
            String stateOut,
            String stateFormat
    ) {
        public Config(String filePath) {
            this(filePath, false, 4711, null, null, "json");
        }
    }

    /**
     * Runs a ReLang source file with the given configuration.
     *
     * @param config execution configuration
     * @return exit code (0 = success, 1 = error, 75 = suspended)
     */
    public static int run(Config config) {
        File file = new File(config.filePath());
        if (!file.exists()) {
            System.err.println("File not found: " + config.filePath());
            return ExitCodes.ERROR;
        }

        SuspendedResult resumeState = null;
        if (config.stateIn() != null) {
            try {
                resumeState = loadState(config.stateIn(), config.stateFormat());
            } catch (Exception e) {
                System.err.println("Error loading state: " + e.getMessage());
                return ExitCodes.ERROR;
            }
        }

        var builder = Context.newBuilder("relang")
                .allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false");

        if (config.inspect()) {
            builder.allowExperimentalOptions(true)
                   .option("inspect", "true")
                   .option("inspect.Port", String.valueOf(config.inspectPort()));
            System.out.println("Debugger listening on port " + config.inspectPort());
            System.out.println("Connect Chrome DevTools to: chrome://inspect");
        }

        try (Context context = builder.build()) {
            if (resumeState != null) {
                context.getPolyglotBindings().putMember("resumeState", resumeState);
            }

            Source source = Source.newBuilder("relang", file).build();
            Value result = context.eval(source);

            if (result != null && result.isHostObject()) {
                Object hostObj = result.asHostObject();
                if (hostObj instanceof SuspendedResult suspended) {
                    if (config.stateOut() != null) {
                        try {
                            saveState(suspended, config.stateOut(), config.stateFormat());
                            System.out.println("State saved to: " + config.stateOut());
                        } catch (Exception e) {
                            System.err.println("Error saving state: " + e.getMessage());
                            return ExitCodes.ERROR;
                        }
                    }
                    return ExitCodes.SUSPENDED;
                }
            }

            if (result != null && !result.isNull()) {
                System.out.println("Result: " + result);
            }
            return ExitCodes.OK;

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return ExitCodes.ERROR;
        } catch (PolyglotException e) {
            System.err.println("Error: " + e.getMessage());
            return ExitCodes.ERROR;
        } catch (Exception e) {
            var details = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            System.err.println("Internal error: " + details);
            return ExitCodes.ERROR;
        }
    }

    private static SuspendedResult loadState(String path, String format) throws IOException {
        Path stateFile = Path.of(path);
        if (!Files.exists(stateFile)) {
            throw new IOException("State file not found: " + path);
        }

        if ("protobuf".equals(format)) {
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

        if ("protobuf".equals(format)) {
            Files.write(stateFile, state.toProtoBytes());
        } else {
            Files.writeString(stateFile, state.toJson());
        }
    }
}

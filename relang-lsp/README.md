# ReLang LSP

Standalone Language Server Protocol (LSP) server for ReLang, providing real-time editor support including type error diagnostics, syntax error reporting, and language intelligence features.

## Overview

This module provides a standalone LSP server that integrates with the GraalVM Truffle framework. The server listens on a configurable TCP port and provides real-time language features to IDE extensions and LSP-compatible editors.

The LSP server leverages GraalVM's built-in Truffle LSP instrument, eliminating the need for custom LSP protocol implementation. When the server receives ReLang source code:

1. The ReLang parser processes the code with ANTLR4
2. The static type checker (`ReLangTypeChecker`) validates types
3. If errors are detected, they are converted to LSP diagnostics via `LspDiagnosticsHelper`
4. The Truffle LSP instrument publishes diagnostics to connected clients (editors)

## Features

The LSP server provides:

- **Real-time type error diagnostics** — Type mismatches, missing parameters, incorrect return types appear as red squiggles in your editor
- **Syntax error reporting** — ANTLR parser errors are reported with precise line and column information
- **Basic language intelligence** — Symbol information and other Truffle-provided features (hover, go-to-definition)
- **IDE integration** — Works with VS Code (via `relang-vscode` extension), IntelliJ (via `relang-intellij` plugin), and any LSP-compatible editor

## Prerequisites

- **GraalVM 25 or later** — Required for Truffle LSP instrument support
- **Java 25+** — GraalVM-based JVM
- **Built relang-core module** — The core language implementation must be compiled first

## Running the Server

### Default Configuration

Start the server with default settings (bind to `127.0.0.1:8123`):

```bash
./gradlew :relang-lsp:run
```

Expected output:

```
Starting ReLang LSP server on 127.0.0.1:8123...
Press Ctrl+C to stop.
```

### Custom Port

Run on a different port:

```bash
./gradlew :relang-lsp:run --args="--port 8124"
```

### Custom Host and Port

Bind to a specific network interface:

```bash
./gradlew :relang-lsp:run --args="--host 0.0.0.0 --port 8124"
```

Use `0.0.0.0` to listen on all available network interfaces (useful for remote development).

### Help

Display available options:

```bash
./gradlew :relang-lsp:run --args="--help"
```

Output:

```
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
```

## IDE Integration

### VS Code

The `relang-vscode` extension provides LSP client support. To test:

1. **Start the LSP server** (in a terminal from the project root):
   ```bash
   ./gradlew :relang-lsp:run
   ```

2. **Launch VS Code with the extension**:
   ```bash
   cd relang-vscode
   code .
   ```

3. **Open Extension Development Host** — Press `F5` to launch a development instance with your extension loaded

4. **Open a ReLang file** (e.g., `relang-core/src/test/resources/samples/arithmetic.re`)

5. **Verify LSP features** — You should see:
   - Syntax highlighting (keywords, operators, identifiers)
   - Error diagnostics (red squiggles for type errors)
   - Code completion (press `Ctrl+Space`)
   - Hover information

For details, see [relang-vscode README](../relang-vscode/README.md).

### IntelliJ IDEA / PyCharm / WebStorm

The `relang-intellij` plugin supports LSP via the LSP4IJ framework. To test:

1. **Start the LSP server**:
   ```bash
   ./gradlew :relang-lsp:run
   ```

2. **Build and install the plugin** — See [relang-intellij README](../relang-intellij/README.md)

3. **Open a ReLang file** in the IDE — Diagnostics and language features should appear automatically

### Generic LSP Client

Any editor or tool supporting LSP over TCP can connect to the ReLang LSP server. Configuration typically requires:

- **Server address**: `127.0.0.1` (or custom host from `--host` option)
- **Server port**: `8123` (or custom port from `--port` option)
- **Language ID**: `relang`
- **File extensions**: `.re`

## Testing the LSP

### Interactive Testing

Follow these steps to verify the LSP server is working:

1. **Start the LSP server** in one terminal:
   ```bash
   ./gradlew :relang-lsp:run
   ```

2. **In another terminal, create a test file** with type errors:
   ```bash
   cat > test_errors.re << 'EOF'
   fn add(a: Int, b: Int): Int = a + b;

   // Type error: Bool instead of Int
   let x: Bool = add(1, 2);

   // Missing parameter type
   fn bad(x) = x + 1;

   // Return type mismatch
   fn wrong(): Int = true;
   EOF
   ```

3. **Open the file in your IDE** (with LSP connected) — You should see red squiggles on:
   - Line 4: Type mismatch (expected `Bool`, got `Int`)
   - Line 7: Missing type annotation on parameter `x`
   - Line 10: Return type mismatch (expected `Int`, got `Bool`)

### What a Type Error Looks Like

In VS Code, navigate to the **Problems** panel (`View` → `Problems`). You will see entries like:

```
test_errors.re:4:12 - error: [relang] Type mismatch: expected Bool, got Int
test_errors.re:7:7 - error: [relang] Parameter 'x' must have a type annotation
test_errors.re:10:24 - error: [relang] Return type mismatch: expected Int, got Bool
```

Each error entry is clickable and navigates to the problematic code.

### Valid Code Example

For comparison, here is valid ReLang code with no errors:

```relang
fn factorial(n: Int): Int {
    if n < 2 { 1 } else { n * factorial(n - 1) }
}

fn main(): Int = factorial(5);

let result = main();
```

When opened in an LSP-connected editor, this file should show no diagnostics.

### Raw TCP Testing (Advanced)

For debugging the LSP protocol directly, you can test with netcat:

```bash
# Start server in one terminal
./gradlew :relang-lsp:run

# In another terminal, connect with netcat
nc 127.0.0.1 8123
```

The server expects the LSP JSON-RPC protocol. For most use cases, using an IDE extension is simpler.

## Architecture

### Component Overview

```
┌────────────────────────────────────────────────────────┐
│ ReLangLspLauncher (main entry point)                   │
│ - Parses --host and --port arguments                   │
│ - Creates GraalVM Polyglot Context with lsp option     │
├────────────────────────────────────────────────────────┤
│ GraalVM Polyglot Context                               │
│ - Language: "relang"                                   │
│ - Option: lsp = "127.0.0.1:8123" (configurable)        │
├────────────────────────────────────────────────────────┤
│ Truffle LSP Instrument (built-in)                      │
│ - Receives source code from editor clients             │
│ - Delegates to ReLang parser                           │
├────────────────────────────────────────────────────────┤
│ ReLang Parser (ANTLR4)                                 │
│ - Converts source → ANTLR parse tree → Truffle AST    │
├────────────────────────────────────────────────────────┤
│ ReLangTypeChecker                                      │
│ - Static type analysis (4-pass algorithm)              │
│ - Detects type errors                                  │
├────────────────────────────────────────────────────────┤
│ LspDiagnosticsHelper                                   │
│ - Converts TypeError objects to LSP Diagnostic objects │
│ - Publishes via DiagnosticsNotification                │
├────────────────────────────────────────────────────────┤
│ IDE Extension (e.g., VS Code, IntelliJ)               │
│ - LSP client that displays diagnostics                 │
└────────────────────────────────────────────────────────┘
```

### Key Classes

**`ReLangLspLauncher.java`** (this module)

Entry point that:
- Parses command-line arguments (`--host`, `--port`, `--help`)
- Creates a GraalVM Polyglot Context with the LSP instrument enabled
- Listens indefinitely on the specified address until Ctrl+C

**`ReLangTypeChecker`** (in relang-core)

Static type checker that:
- Validates that all expressions have concrete types
- Enforces mandatory type annotations on function parameters
- Infers return types for non-recursive functions
- Detects type mismatches, arity errors, and undefined variables
- Accumulates errors and returns a list of `TypeError` objects

**`LspDiagnosticsHelper`** (in relang-core)

Adapter that:
- Converts `TypeError` objects to LSP `Diagnostic` objects
- Creates `DiagnosticsNotification` messages for the Truffle LSP instrument
- Only loaded when the LSP tool is available at runtime
- Falls back to `ReLangTypeCheckException` in CLI mode

### Error Handling Flow

1. **CLI Mode** (e.g., `./gradlew :relang-core:run --args "script.re"`):
   - Parser detects errors → `ReLangTypeCheckException` is thrown
   - Exception message printed to stderr
   - Program exits with error code 1

2. **LSP Mode** (this module):
   - Parser detects errors → `LspDiagnosticsHelper.buildDiagnosticsNotification()` called
   - Returns `DiagnosticsNotification` with error list
   - Truffle LSP instrument publishes to connected editor clients
   - Server continues running; editor displays red squiggles

## Build

Build the LSP server JAR:

```bash
./gradlew :relang-lsp:build
```

The compiled JAR is located at:

```
relang-lsp/build/libs/relang-lsp.jar
```

You can also build the entire project:

```bash
./gradlew build
```

## Troubleshooting

### Error: "LSP requires GraalVM with the LSP tool available"

**Cause**: You are using a JDK that does not have the GraalVM LSP tool installed.

**Solution**:
- Install GraalVM 25+ (not a standard OpenJDK)
- Verify your `JAVA_HOME` environment variable points to GraalVM:
  ```bash
  java -version
  ```
  Output should contain "GraalVM".

### Port Already in Use

**Error**: `java.net.BindException: Address already in use`

**Cause**: Another process is using the default port 8123.

**Solution**:
- Use a different port:
  ```bash
  ./gradlew :relang-lsp:run --args="--port 8124"
  ```
- Or kill the existing process:
  ```bash
  lsof -i :8123      # Find process using port 8123
  kill -9 <PID>      # Kill the process
  ```

### No Diagnostics Appearing in Editor

**Symptoms**: LSP server is running, but editor shows no error squiggles.

**Diagnostics**:

1. **Verify the server is running**:
   ```bash
   netstat -an | grep 8123  # macOS/Linux
   netstat -ano | find "8123"  # Windows
   ```

2. **Check editor settings** — Ensure LSP is enabled:
   - VS Code: Settings → search "relang.lsp.enabled" → set to `true`
   - IntelliJ: File → Settings → Languages & Frameworks → LSP → enabled

3. **Check editor output logs**:
   - VS Code: `View` → `Output` → select "ReLang Language Server"
   - IntelliJ: `View` → `Tool Windows` → `LSP Console`

4. **Test with a simple error**:
   ```relang
   fn test(): Int = true;  // Should error: expected Int, got Bool
   ```
   Save the file and check if red squiggle appears.

### Server Crashes on Startup

**Error**: Stack trace or "Failed to start LSP server"

**Diagnostics**:

1. **Verify relang-core is built**:
   ```bash
   ./gradlew :relang-core:build
   ```

2. **Check GraalVM installation**:
   ```bash
   $JAVA_HOME/bin/java -version
   ```

3. **Look at detailed error output** — Run with explicit error printing:
   ```bash
   ./gradlew :relang-lsp:run 2>&1 | tee lsp.log
   ```

4. **Check system logs** for permission errors or out-of-memory conditions

## Module Dependencies

- **relang-core** — The ReLang language implementation (parser, type checker, runtime)
- **graalvm.polyglot** — GraalVM Polyglot API for language interoperability
- **graalvm.tools.lsp** — GraalVM LSP instrument (available at runtime only)

## Related Modules

- **relang-vscode** — VS Code extension and LSP client for ReLang
- **relang-intellij** — IntelliJ plugin with LSP support via LSP4IJ
- **relang-core** — Core language implementation used by the LSP server
- **relang-textmate** — Shared TextMate grammar for syntax highlighting

## Further Reading

- [ReLang Specifications](../specs/v0.1/README.md) — Language syntax and semantics
- [ReLang Core README](../relang-core/README.md) — Type checker and parser details
- [VS Code Extension README](../relang-vscode/README.md) — Editor integration

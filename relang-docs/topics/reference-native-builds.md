# Native Builds Reference

Technical reference for building and distributing ReLang as native executables.

## Overview

ReLang provides two native executables built using GraalVM Native Image:

| Binary | Purpose | Usage |
|--------|---------|-------|
| `relang` | Native interpreter | Run `.re` files with optional checkpointing |
| `relangc` | Compiler | Compile `.re` files to standalone native executables |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     relang (interpreter)                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ ReLang      │  │ Truffle     │  │ GraalVM Native      │ │
│  │ Language    │──│ Framework   │──│ Image Runtime       │ │
│  │ (AST nodes) │  │ (JIT)       │  │ (AOT compiled)      │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     relangc (compiler)                      │
│  ┌─────────────┐     ┌──────────────────────────────────┐  │
│  │ Source.re   │────▶│ Native Image with Embedded Source │  │
│  └─────────────┘     └──────────────────────────────────┘  │
│                              │                              │
│                              ▼                              │
│                      ┌──────────────┐                      │
│                      │ myprogram    │ (standalone binary)  │
│                      └──────────────┘                      │
└─────────────────────────────────────────────────────────────┘
```

## Building from Source

### Prerequisites

- Java 21+ (GraalVM recommended)
- GraalVM Native Image component
- Gradle 8.x

### Build Commands

```bash
# Build native interpreter
./gradlew :relang-native:nativeCompile

# Build compiler
./gradlew :relang-compiler:nativeCompile

# Output locations
ls relang-native/build/native/nativeCompile/relang
ls relang-compiler/build/native/nativeCompile/relangc
```

## Interpreter (`relang`)

### Binary Characteristics

| Property | Value |
|----------|-------|
| Size | ~40-60 MB |
| Startup | ~10-50ms |
| Memory | Lower than JVM mode |
| JIT | Disabled (AOT only) |

### Interpreter Usage

```bash
# Run a program
relang program.re

# Run with checkpointing
relang program.re --state-out state.json

# Resume from checkpoint
relang program.re --state-in state.json --state-out state.json

# Start REPL
relang
```

### Exit Codes

| Code | Constant | Meaning |
|------|----------|---------|
| `0` | `EX_OK` | Program completed successfully |
| `1` | `EX_ERROR` | Runtime or parse error |
| `75` | `EX_TEMPFAIL` | Suspended at checkpoint |

### State File Options

```bash
--state-in <file>       # Load execution state before running
--state-out <file>      # Save state when checkpoint is hit
--state-format <fmt>    # json (default) | protobuf
```

## Compiler (`relangc`)

### How It Works

The compiler produces standalone native executables by:

1. **Reading** the `.re` source file
2. **Validating** syntax (parsing to AST)
3. **Embedding** the source as a resource
4. **Building** a native image with the embedded source
5. **Output** a self-contained executable

### Compiler Usage

```bash
# Compile a program
relangc program.re -o myprogram

# Run the compiled binary
./myprogram

# Compiled binaries support checkpointing
./myprogram --state-out state.json
./myprogram --state-in state.json
```

### Compiled Binary Characteristics

| Property | Value |
|----------|-------|
| Size | ~30-50 MB |
| Startup | <10ms |
| Dependencies | None (self-contained) |
| State support | Full (`--state-in/out`) |

## GraalVM Native Image

### What is Native Image?

GraalVM Native Image compiles Java bytecode ahead-of-time (AOT) into a standalone executable. This provides:

- **Fast startup**: No JVM warmup required
- **Lower memory**: No JIT compiler overhead
- **Self-contained**: Single binary, no JRE needed

### How ReLang Uses It

ReLang is built on the Truffle language implementation framework. When compiled to native image:

1. The Truffle AST interpreter is compiled to native code
2. The ReLang language implementation is embedded
3. Source code is either read at runtime (interpreter) or embedded (compiler output)

### Trade-offs

| Aspect | JVM Mode | Native Mode |
|--------|----------|-------------|
| Startup | ~500ms | ~10ms |
| Peak performance | Higher (JIT) | Slightly lower (AOT) |
| Memory usage | Higher | Lower |
| Binary size | JRE required | ~40-60MB standalone |
| Debugging | Full support | Limited |

## Build Configuration

### Gradle Plugin

ReLang uses the [GraalVM Native Build Tools](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html) Gradle plugin:

```gradle
plugins {
    id 'org.graalvm.buildtools.native' version '0.11.1'
}

graalvmNative {
    binaries {
        main {
            imageName = 'relang'
            mainClass = 'com.relang.ReLangLauncher'
            buildArgs.addAll([
                '--no-fallback',
                '-H:+ReportExceptionStackTraces'
            ])
        }
    }
}
```

### Reflection Configuration

Native Image requires explicit configuration for reflection. ReLang registers:

- `SuspendedResult` - State wrapper class
- `ResumableState` - Execution state
- `ResumableState.FrameState` - Per-frame state
- ANTLR generated parser classes
- Protobuf message classes

Configuration file: `META-INF/native-image/reflect-config.json`

```json
[
  {
    "name": "com.relang.nodes.SuspendedResult",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  }
]
```

## Truffle Framework Integration

### Language Registration

ReLang registers with Truffle via annotation:

```java
@TruffleLanguage.Registration(
    id = "relang",
    name = "ReLang",
    defaultMimeType = "application/x-relang"
)
public final class ReLang extends TruffleLanguage<ReLangContext> {
    // ...
}
```

### Polyglot API

The launcher uses the Polyglot API to execute ReLang code:

```java
try (Context context = Context.newBuilder("relang")
        .allowAllAccess(true)
        .build()) {

    Value result = context.eval("relang", sourceCode);

    // Handle result...
}
```

### State Passing

State is passed via polyglot bindings:

```java
// Inject state before evaluation
context.getPolyglotBindings().putMember("resumeState", suspendedResult);

// Execute (will resume from checkpoint)
Value result = context.eval("relang", sourceCode);
```

## Distribution

### Packaging

Native binaries can be distributed as:

- Standalone executable (single file)
- Archive (tar.gz, zip) with documentation
- Package manager (Homebrew, apt, etc.)

### Cross-compilation

GraalVM Native Image builds for the current platform. For cross-platform distribution:

- Build on each target platform
- Use CI/CD matrix builds (GitHub Actions, etc.)

### Supported Platforms

| Platform | Architecture | Status |
|----------|--------------|--------|
| Linux | x86_64 | Supported |
| Linux | aarch64 | Supported |
| macOS | x86_64 | Supported |
| macOS | aarch64 (M1/M2) | Supported |
| Windows | x86_64 | Supported |

## Troubleshooting

### Build Failures

**Missing GraalVM Native Image:**
```
Error: Cannot find native-image executable
```
Solution: Install GraalVM and add to PATH, or use `gu install native-image`.

**Reflection errors at runtime:**
```
Error: Class X has not been registered for reflection
```
Solution: Add the class to `reflect-config.json`.

### Runtime Issues

**State file not found:**
```
Error: Cannot read state file: state.json
```
Solution: Ensure file exists and is readable.

**Source code changed:**
```
Error: Source code has changed since suspension
```
Solution: Cannot resume with modified code. Start fresh or use original source.

## See Also

- [CLI Reference](reference-cli.md)
- [State File Format](reference-state-format.md)
- [How Resumability Works](explanation-resumability.md)
- [GraalVM Native Image Docs](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Truffle Framework](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/)

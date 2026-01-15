# ReLang Native

Native image build configuration for the `relang` interpreter.

## Overview

This module uses GraalVM Native Image to compile the ReLang interpreter into a standalone native executable. The resulting binary:

- Starts in ~10ms (vs ~500ms for JVM)
- Has lower memory footprint
- Requires no JVM installation
- Is fully self-contained (~40-60MB)

## Prerequisites

- GraalVM 25+ with Native Image component
- Set `GRAALVM_HOME` or ensure `native-image` is in PATH

### Installing Native Image

```bash
# If using SDKMAN
sdk install java 25.0.0-graal

# Or install native-image component
gu install native-image
```

## Building

```bash
# Build native interpreter
./gradlew :relang-native:nativeCompile

# Output location
ls -la relang-native/build/native/nativeCompile/relang
```

Build time: ~1-2 minutes depending on hardware.

## Usage

```bash
# Run a program
./relang-native/build/native/nativeCompile/relang program.re

# Run with checkpointing
./relang-native/build/native/nativeCompile/relang program.re --state-out state.json

# Resume from checkpoint
./relang-native/build/native/nativeCompile/relang program.re --state-in state.json

# Start REPL
./relang-native/build/native/nativeCompile/relang

# Show help
./relang-native/build/native/nativeCompile/relang --help
```

### Command-Line Options

```
Usage: relang [options] [file.re]

Options:
  --state-in <file>     Load execution state before running
  --state-out <file>    Save state when checkpoint is hit
  --state-format <fmt>  State format: json (default) | protobuf
  --lsp                 Start LSP server
  --inspect             Enable debugger
  --help, -h            Show help

Exit codes:
  0   Program completed successfully
  1   Runtime or parse error
  75  Suspended at checkpoint
```

## Build Configuration

### Gradle Configuration

```gradle
plugins {
    id 'java'
    alias(libs.plugins.graalvm.native)
}

dependencies {
    implementation project(':relang-core')
}

graalvmNative {
    binaries {
        main {
            imageName = 'relang'
            mainClass = 'com.relang.ReLangLauncher'
            buildArgs.addAll([
                '--no-fallback',
                '-H:+ReportExceptionStackTraces',
                '--initialize-at-build-time=com.relang',
                '--initialize-at-build-time=org.antlr.v4.runtime',
                '--initialize-at-build-time=com.google.gson',
                '--initialize-at-build-time=com.google.protobuf'
            ])
        }
    }
}
```

### Reflection Configuration

Native Image requires explicit reflection configuration. The following classes are registered in `src/main/resources/META-INF/native-image/reflect-config.json`:

- `com.relang.nodes.SuspendedResult`
- `com.relang.nodes.ResumableState`
- `com.relang.nodes.ResumableState$FrameState`
- `com.relang.nodes.StateCodeMismatchException`

## Performance Characteristics

| Property | Value |
|----------|-------|
| Binary size | ~40-60 MB |
| Startup time | ~10-50ms |
| Memory usage | Lower than JVM |
| JIT compilation | Disabled (AOT only) |
| Peak performance | Slightly lower than JVM with JIT |

## Trade-offs

### Advantages

- **Fast startup**: No JVM warmup required
- **Lower memory**: No JIT compiler overhead at runtime
- **Self-contained**: Single binary, no JRE needed
- **Easy distribution**: Just copy the binary

### Disadvantages

- **Build time**: Native compilation takes 1-2 minutes
- **Peak performance**: AOT compilation can't optimize as aggressively as JIT
- **Debugging**: More limited than JVM mode
- **Platform-specific**: Must build separately for each target platform

## Troubleshooting

### Build Failures

**Missing native-image:**
```
Error: Cannot find native-image executable
```
Solution: Install GraalVM and run `gu install native-image`, or set `GRAALVM_HOME`.

**Reflection errors:**
```
Error: Class X has not been registered for reflection
```
Solution: Add the class to `reflect-config.json`.

**Out of memory:**
```
Error: OutOfMemoryError during native-image build
```
Solution: Increase heap with `-J-Xmx8g` in buildArgs.

### Runtime Issues

**State file not found:**
```
Error: Cannot read state file: state.json
```
Solution: Ensure the file exists and path is correct.

**Source code mismatch:**
```
Error: Source code has changed since suspension
```
Solution: The source must be identical between suspend and resume.

## Distribution

The native binary can be distributed as:

1. **Standalone executable** - Just copy the binary
2. **Archive** - tar.gz or zip with documentation
3. **Package manager** - Homebrew, apt, etc.

### Cross-Platform

Native Image builds for the current platform only. For multi-platform distribution:

```bash
# Build on each target platform
# macOS arm64
./gradlew :relang-native:nativeCompile

# Linux x86_64 (on Linux machine)
./gradlew :relang-native:nativeCompile

# Windows (on Windows machine)
./gradlew :relang-native:nativeCompile
```

Or use CI/CD with matrix builds.

## Supported Platforms

| Platform | Architecture | Status |
|----------|--------------|--------|
| Linux | x86_64 | Supported |
| Linux | aarch64 | Supported |
| macOS | x86_64 | Supported |
| macOS | aarch64 (Apple Silicon) | Supported |
| Windows | x86_64 | Supported |

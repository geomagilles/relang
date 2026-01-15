# ReLang Compiler

Compiler that produces standalone native executables from ReLang source files.

## Overview

The `relangc` compiler takes a `.re` source file and produces a self-contained native executable. The compiled binary:

- Runs without any external dependencies
- Supports checkpoint/resume with `--state-in/--state-out`
- Has fast startup (~10ms)
- Is ~27MB in size

## Prerequisites

- GraalVM 25+ with Native Image component
- `GRAALVM_HOME` set, or `native-image` in PATH

```bash
# Verify native-image is available
native-image --version
```

## Building the Compiler

```bash
# Build the compiler module
./gradlew :relang-compiler:build

# Or build a native compiler binary
./gradlew :relang-compiler:nativeCompile
```

## Usage

### Using Gradle (JVM mode)

```bash
# Compile a ReLang program
./gradlew :relang-compiler:run --args="program.re -o myprogram"

# Compile with verbose output
./gradlew :relang-compiler:run --args="program.re -o myprogram -v"
```

### Using Native Compiler Binary

```bash
# After building native compiler
./relang-compiler/build/native/nativeCompile/relangc program.re -o myprogram
```

### Command-Line Options

```
Usage: relangc [options] <input.re>

Options:
  -o, --output <file>   Output executable name (default: input name without .re)
  -v, --verbose         Verbose output (shows build progress)
  -h, --help            Show help

Examples:
  relangc program.re                Compile to ./program
  relangc program.re -o myapp       Compile to ./myapp
  relangc -v program.re             Compile with verbose output
```

## Running Compiled Programs

Compiled executables support the same state management options as the interpreter:

```bash
# Run the compiled program
./myprogram

# Run with checkpoint output
./myprogram --state-out state.json

# Resume from checkpoint
./myprogram --state-in state.json

# Combined (resume and save new checkpoint)
./myprogram --state-in state.json --state-out state.json

# Use protobuf format (more compact)
./myprogram --state-out state.pb --state-format protobuf

# Show help
./myprogram --help
```

### Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Program completed successfully |
| `1` | Runtime or parse error |
| `75` | Suspended at checkpoint (state saved) |

## How It Works

The compiler works by:

1. **Parsing** - Validates the source syntax using the ReLang parser
2. **Embedding** - Creates a JAR with the source as a resource
3. **Compiling** - Invokes `native-image` with the runtime classpath
4. **Output** - Produces a standalone native executable

```
source.re
    │
    ▼
┌─────────────────┐
│ Syntax Check    │  ← Validates source compiles
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Create JAR      │  ← Embeds source + EmbeddedRunner
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ native-image    │  ← GraalVM AOT compilation
└────────┬────────┘
         │
         ▼
    myprogram (native executable)
```

## Architecture

### Key Classes

| Class | Purpose |
|-------|---------|
| `ReLangCompiler` | Main compiler entry point, orchestrates build |
| `EmbeddedRunner` | Generic runner embedded in compiled executables |

### EmbeddedRunner

The `EmbeddedRunner` class is compiled into every output executable. At runtime it:

1. Loads the embedded source from resources
2. Creates a Truffle context
3. Injects resume state if `--state-in` provided
4. Executes the program
5. Saves state if suspended and `--state-out` provided

## Example

### Source File

```
// counter.re
fn count() {
    x = 0;
    while (x < 5) {
        x = x + 1;
        checkpoint;
    }
    return x;
}

count()
```

### Compilation

```bash
./gradlew :relang-compiler:run --args="counter.re -o counter"
```

Output:
```
Validating syntax...
Building native executable...
Compiled: /path/to/counter
```

### Execution with Checkpoints

```bash
# First run - suspends at x=1
./counter --state-out state.json
echo $?  # 75

# Resume - suspends at x=2
./counter --state-in state.json --state-out state.json
echo $?  # 75

# ... continue until completion
./counter --state-in state.json
echo $?  # 0
# Output: Result: 5
```

## Compiled Binary Characteristics

| Property | Value |
|----------|-------|
| Size | ~27 MB |
| Startup | <10ms |
| Dependencies | None (self-contained) |
| State support | Full (`--state-in/out`) |
| Platform | Build platform only |

## Troubleshooting

### Compilation Failures

**native-image not found:**
```
Compilation error: native-image not found. Please ensure GraalVM is installed...
```
Solution: Install GraalVM, set `GRAALVM_HOME`, or add `native-image` to PATH.

**Syntax error:**
```
Compilation error: Syntax error: ...
```
Solution: Fix the syntax error in your source file.

**native-image build failed:**
```
Compilation error: native-image failed with exit code: 1
```
Solution: Run with `-v` for verbose output to see the native-image error.

### Runtime Issues

**Embedded source not found:**
```
Error loading embedded source: Embedded source not found
```
This indicates a build issue. Recompile the program.

**State file issues:**
```
Error loading state: State file not found: state.json
```
Ensure the state file exists at the specified path.

## Limitations

- Cross-compilation is not supported (build on target platform)
- Compile time is ~20-30 seconds per program
- Each compiled binary includes the full ReLang runtime (~27MB)

## Development

### Building from Source

```bash
# Compile the compiler module
./gradlew :relang-compiler:compileJava

# Run tests
./gradlew :relang-compiler:test

# Build fat JAR
./gradlew :relang-compiler:jar
```

### Module Structure

```
relang-compiler/
├── build.gradle
├── README.md
└── src/main/java/com/relang/compiler/
    ├── ReLangCompiler.java    # Main compiler
    └── EmbeddedRunner.java    # Runtime for compiled programs
```

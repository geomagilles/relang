# ReLang

A resumable programming language built on GraalVM Truffle with checkpoint-based execution state persistence.

## Overview

ReLang is an experimental programming language that can suspend execution at any point, serialize its complete state to disk, and resume later from exactly where it left off. This enables:

- **Long-running computations** that survive process restarts
- **Workflow orchestration** with durable execution
- **Debugging** with time-travel capabilities
- **Distributed computing** with state migration

## Quick Start

### Prerequisites

- Java 21+ (GraalVM recommended)
- Gradle 8.x (wrapper included)

### Run a Program

```bash
# Build the project
./gradlew build

# Run a ReLang program
./gradlew :relang-core:run --args="examples/hello.re"

# Start the REPL
./gradlew :relang-core:run
```

### Checkpoint and Resume

```bash
# Create a test program with checkpoints
cat > test.re << 'EOF'
x = 0;
while (x < 5) {
    x = x + 1;
    checkpoint;
}
x
EOF

# Run until first checkpoint (exits with code 75)
./gradlew :relang-core:run --args="test.re --state-out state.json"

# Resume and continue (exits 75 again at next checkpoint)
./gradlew :relang-core:run --args="test.re --state-in state.json --state-out state.json"

# Keep resuming until completion (exits with code 0)
./gradlew :relang-core:run --args="test.re --state-in state.json"
```

## Language Features

```
// Variables and arithmetic
x = 42;
y = x * 2 + 10;

// Functions
fn factorial(n) {
    if (n < 2) {
        return 1;
    }
    return n * factorial(n - 1);
}

// Control flow
if (x < 100) {
    result = factorial(5);
} else {
    result = 0;
}

// Loops
sum = 0;
i = 0;
while (i < 10) {
    sum = sum + i;
    i = i + 1;
}

// Checkpoints - suspend execution here
checkpoint;

// The last expression is the program result
sum
```

### Types

| Type | Description | Examples |
|------|-------------|----------|
| `long` | 64-bit signed integer | `0`, `42`, `-1` |
| `boolean` | Boolean value | `true`, `false` |

### Operators

| Category | Operators |
|----------|-----------|
| Arithmetic | `+`, `-`, `*`, `/` |
| Comparison | `<`, `==` |

### Keywords

```
fn  if  else  while  return  checkpoint  true  false
```

## Project Structure

```
relang/
├── relang-core/        # Language implementation (Truffle AST, parser, runtime)
├── relang-native/      # Native image build for `relang` interpreter
├── relang-compiler/    # Compiler (`relangc`) to produce native executables
├── relang-textmate/    # TextMate grammar for syntax highlighting
├── relang-vscode/      # VS Code extension
├── relang-intellij/    # IntelliJ plugin
└── relang-docs/        # Writerside documentation
```

## Building Native Executables

### Native Interpreter (`relang`)

Build a standalone native interpreter binary:

```bash
./gradlew :relang-native:nativeCompile

# Run directly
./relang-native/build/native/nativeCompile/relang program.re
```

### Compiler (`relangc`)

Compile ReLang programs to standalone native executables:

```bash
# Build the compiler (requires native-image in PATH)
./gradlew :relang-compiler:run --args="program.re -o myprogram"

# Run the compiled program
./myprogram
./myprogram --state-out state.json  # With checkpointing
```

## IDE Support

### VS Code

```bash
cd relang-vscode
npm install
npm run copy-grammar
code .
# Press F5 to launch Extension Development Host
```

### IntelliJ IDEA

```bash
./gradlew :relang-intellij:runIde
```

Both IDEs support:
- Syntax highlighting
- LSP integration (code completion, diagnostics)
- File type recognition

## Command-Line Reference

```
Usage: relang [options] [file.re]

Options:
  --state-in <file>     Load execution state before running
  --state-out <file>    Save state when checkpoint is hit
  --state-format <fmt>  State format: json (default) | protobuf
  --lsp                 Start LSP server (for IDE integration)
  --inspect             Enable Chrome DevTools debugger
  --help, -h            Show help

Exit codes:
  0   Program completed successfully
  1   Runtime or parse error
  75  Suspended at checkpoint (state saved)
```

## Architecture

ReLang is built on GraalVM's Truffle framework:

```
Source Code (.re)
       │
       ▼
┌─────────────────┐
│  ANTLR Parser   │  ← Lexer/parser from ReLang.g4
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Truffle AST    │  ← ReLangNode, AddNode, IfNode, etc.
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Truffle Runtime │  ← Interpreter + JIT compilation
└─────────────────┘
```

### Resumability

When `checkpoint;` is executed:

1. **Unwind**: Exception propagates up, each node captures its state
2. **Serialize**: Call stack + variables saved to JSON/Protobuf
3. **Exit**: Process exits with code 75

When resuming with `--state-in`:

1. **Load**: State deserialized from file
2. **Rewind**: Execution jumps to saved positions
3. **Continue**: Normal execution resumes

## Development

### Build Commands

```bash
./gradlew build              # Build all modules
./gradlew test               # Run tests
./gradlew :relang-core:run   # Run launcher
```

### Generate Parser

```bash
./gradlew :relang-core:generateGrammarSource
```

### Run Tests

```bash
./gradlew :relang-core:test
./gradlew :relang-core:test --tests "com.relang.ReLangTest.testArithmetic"
```

## Documentation

Full documentation is available in the `relang-docs` module using JetBrains Writerside:

- [Getting Started Tutorial](relang-docs/topics/tutorial-getting-started.md)
- [Language Syntax Reference](relang-docs/topics/reference-language-syntax.md)
- [CLI Reference](relang-docs/topics/reference-cli.md)
- [How Resumability Works](relang-docs/topics/explanation-resumability.md)

## License

[Add your license here]

## Contributing

[Add contribution guidelines here]

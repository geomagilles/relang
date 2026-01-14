# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.relang.ReLangTest"

# Run a single test method
./gradlew test --tests "com.relang.ReLangTest.testArithmetic"

# Generate ANTLR parser from grammar
./gradlew generateGrammarSource

# Build JAR
./gradlew jar

# Run the launcher
./gradlew run
```

## Architecture

ReLang is a custom programming language built on GraalVM's Truffle framework with support for resumable execution via checkpoints.

### Core Components

**Language Registration** (`ReLang.java`): The Truffle language entry point registered with id `relang`. Handles parsing and context creation.

**Context** (`ReLangContext.java`): Per-context state including function registry and resumption state for checkpoint/resume functionality.

**Parser Pipeline**:
- `ReLang.g4` - ANTLR4 grammar defining the language syntax
- `ReLangTruffleParser.java` - Converts ANTLR parse tree to Truffle AST nodes

**AST Nodes** (in `nodes/` package):
- `ReLangNode` - Abstract base class with `executeGeneric(frame, state)` for resumable execution
- `ReLangRootNode` - Function entry point, handles frame state save/restore during suspend/resume
- `ReLangBlockNode` - Statement sequence execution with checkpoint path tracking
- Arithmetic: `AddNode`, `SubNode`, `MulNode`, `DivNode`
- Control flow: `ReLangIfNode`, `ReLangWhileNode`, `ReLangReturnNode`
- Variables: `ReLangReadLocalVarNode`, `ReLangWriteLocalVarNode`, `ReLangReadArgumentNode`
- Functions: `ReLangInvokeNode`

### Resumability System

The language implements checkpoint-based resumable execution:

1. **Checkpoint** (`ReLangCheckpointNode`): Creates a `SuspendedResult` containing execution state
2. **ResumableState**: Stack of `FrameState` objects representing the call stack at suspension
3. **FrameState**: Contains local variables and execution path (block indices) for a single frame
4. **Unwinding**: When checkpoint is hit, state bubbles up through `ReLangBlockNode` and `ReLangRootNode`, each adding their frame state
5. **Rewinding**: On resume, state is passed down; nodes restore locals and skip to the saved execution path index

Resume is triggered by setting `resumeState` in polyglot bindings before re-evaluation.

### Language Features

- Types: `long`, `boolean`
- Operators: `+`, `-`, `*`, `/`, `<`, `==`
- Control flow: `if`/`else`, `while`, `return`
- Functions: `fn name(params) { body }`
- Checkpoints: `checkpoint;` statement for suspending execution

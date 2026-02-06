# ReLang Core

The core language implementation for ReLang, built on GraalVM's Truffle framework.

## Overview

This module contains:

- **ANTLR Grammar** (`ReLang.g4`) - Language syntax definition
- **Truffle AST Nodes** - Executable node classes for all language constructs
- **Parser** - Converts ANTLR parse tree to Truffle AST
- **Runtime** - Context, function registry, and execution engine
- **Resumability** - Checkpoint/resume state management
- **Launcher** - CLI entry point with REPL and file execution

## Building

```bash
# Build the module
./gradlew :relang-core:build

# Generate ANTLR parser from grammar
./gradlew :relang-core:generateGrammarSource

# Generate Protobuf classes
./gradlew :relang-core:generateProto

# Run tests
./gradlew :relang-core:test
```

## Running

```bash
# Run a file
./gradlew :relang-core:run --args="path/to/program.re"

# Start REPL
./gradlew :relang-core:run

# Run with checkpointing
./gradlew :relang-core:run --args="program.re --state-out state.json"

# Resume from checkpoint
./gradlew :relang-core:run --args="program.re --state-in state.json"

# Enable debugger
./gradlew :relang-core:run --args="--inspect program.re"
```

## Architecture

### Package Structure

```
com.relang/
├── ReLang.java              # Truffle language registration
├── ReLangContext.java       # Per-context state (function registry, resume state)
├── ReLangLauncher.java      # CLI entry point
├── ReLangTruffleParser.java # ANTLR to Truffle AST converter
├── nodes/
│   ├── ReLangNode.java      # Abstract base node
│   ├── ReLangRootNode.java  # Function entry point
│   ├── ReLangBlockNode.java # Statement sequence
│   ├── ReLangIfNode.java    # If/else control flow
│   ├── ReLangWhileNode.java # While loop
│   ├── ReLangReturnNode.java
│   ├── ReLangCheckpointNode.java
│   ├── ReLangInvokeNode.java
│   ├── ReLangReadLocalVarNode.java
│   ├── ReLangWriteLocalVarNode.java
│   ├── ReLangReadArgumentNode.java
│   ├── AddNode.java, SubNode.java, MulNode.java, DivNode.java
│   ├── LessThanNode.java, EqualsNode.java
│   ├── ResumableState.java  # Serializable execution state
│   ├── SuspendedResult.java # Wrapper returned on checkpoint
│   └── ReLangSuspendException.java
└── proto/
    └── ResumableStateProtos.java  # Generated Protobuf classes
```

### Truffle Integration

ReLang registers with Truffle using annotations:

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

### Node Execution

Each AST node implements `executeGeneric(VirtualFrame frame)`:

```java
public class AddNode extends ReLangNode {
    @Child private ReLangNode left;
    @Child private ReLangNode right;

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        long l = (long) left.executeGeneric(frame);
        long r = (long) right.executeGeneric(frame);
        return l + r;
    }
}
```

### Resumability Implementation

When `checkpoint;` is executed:

1. `ReLangCheckpointNode` throws `ReLangSuspendException`
2. `ReLangBlockNode` catches it, adds current statement index to path
3. `ReLangRootNode` catches it, captures local variables
4. Exception bubbles up, collecting state from each frame
5. Launcher receives `SuspendedResult`, serializes to file

On resume:

1. Launcher loads state from file, injects via polyglot bindings
2. `ReLangRootNode` pops frame state, restores local variables
3. `ReLangBlockNode` reads path index, skips to saved position
4. Execution continues normally

### State Serialization

Two formats supported:

**JSON** (human-readable):
```json
{
  "sourceHash": "a1b2c3...",
  "frames": [
    {
      "locals": {"x": 42, "flag": true},
      "executionPath": [1, 2]
    }
  ]
}
```

**Protocol Buffers** (compact binary):
```protobuf
message ResumableStateProto {
  string source_hash = 1;
  repeated FrameStateProto frames = 2;
}
```

## Grammar

The language is defined in `src/main/antlr/ReLang.g4`:

```antlr
program     : function* statement* EOF ;

function    : 'fn' IDENTIFIER '(' params? ')' block ;

statement   : assignment
            | ifStmt
            | whileStmt
            | returnStmt
            | 'checkpoint' ';'
            | expr ';'
            ;

expr        : expr ('*'|'/') expr
            | expr ('+'|'-') expr
            | expr ('<'|'==') expr
            | IDENTIFIER '(' args? ')'
            | IDENTIFIER
            | NUMBER
            | 'true' | 'false'
            | '(' expr ')'
            ;
```

## Dependencies

| Dependency | Purpose |
|------------|---------|
| GraalVM Truffle API | Language implementation framework |
| GraalVM Polyglot | Host integration |
| ANTLR 4 | Parser generator |
| Gson | JSON serialization |
| Protobuf | Binary serialization |
| JUnit 5 | Testing |

## Testing

```bash
# Run all tests
./gradlew :relang-core:test

# Run specific test class
./gradlew :relang-core:test --tests "com.relang.ReLangTest"

# Run specific test method
./gradlew :relang-core:test --tests "com.relang.ReLangTest.testArithmetic"
```

### Test Files

Sample programs are in `src/test/resources/samples/`:
- `factorial.re` - Recursive factorial
- `fibonacci.re` - Fibonacci sequence
- `checkpoint_loop.re` - Checkpoint in a loop

## LSP Server

LSP is provided by the dedicated `relang-lsp` module:

```bash
./gradlew :relang-lsp:run --args="--port 8123"
```

Features:
- Syntax error diagnostics
- Code completion
- Hover documentation
- Go-to-definition

## Extending

### Adding a New Node

1. Create node class extending `ReLangNode`:
   ```java
   public class MyNode extends ReLangNode {
       @Child private ReLangNode operand;

       @Override
       public Object executeGeneric(VirtualFrame frame) {
           // Implementation
       }
   }
   ```

2. Update `ReLangTruffleParser` to create the node from ANTLR

3. If needed, update `ReLang.g4` and regenerate parser

### Adding a New Type

1. Update `ReLangTypeSystem` with the new type
2. Add type checks in relevant nodes
3. Update `ResumableState.FrameState` for serialization

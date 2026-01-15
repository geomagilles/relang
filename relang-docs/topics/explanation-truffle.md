# Truffle Architecture

This document explains how ReLang is built on GraalVM's Truffle framework and how Truffle enables high-performance language implementation.

## What is Truffle?

Truffle is a framework for building programming language interpreters. Instead of writing a traditional compiler, you:

1. Define an **Abstract Syntax Tree (AST)** representation
2. Write **execute methods** for each node type
3. Let Truffle **JIT compile** your interpreter to native code

## Why Truffle?

Traditional language implementation requires:
- Lexer and parser
- AST representation
- Type checking
- Intermediate representation (IR)
- Optimization passes
- Code generation
- Runtime (GC, stack management)

With Truffle, you only need:
- Lexer and parser (ANTLR)
- AST nodes with execute methods

Truffle provides everything else automatically.

## ReLang Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Source Code (.re)                    │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                 ANTLR Lexer/Parser                      │
│                   (ReLang.g4)                           │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              ReLangTruffleParser                        │
│         (ANTLR tree → Truffle AST)                      │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                   Truffle AST                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │ ReLangRootNode                                   │   │
│  │  └── ReLangBlockNode                            │   │
│  │       ├── ReLangWriteLocalVarNode               │   │
│  │       ├── ReLangIfNode                          │   │
│  │       │    ├── condition: LessThanNode          │   │
│  │       │    └── thenBranch: ReLangBlockNode      │   │
│  │       └── ReLangInvokeNode                      │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              Truffle Execution Engine                   │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Interpreter (first executions)                   │   │
│  │      │                                          │   │
│  │      ▼ (after warmup)                           │   │
│  │ Graal JIT Compiler → Native Code                │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## Key Truffle Concepts

### Node Classes

Every AST node extends Truffle's `Node` class:

```java
public abstract class ReLangNode extends Node {
    public abstract Object executeGeneric(VirtualFrame frame);
}
```

### The @Child Annotation

Marks child nodes that Truffle should manage:

```java
public class ReLangIfNode extends ReLangNode {
    @Child private ReLangNode condition;
    @Child private ReLangNode thenBranch;
    @Child private ReLangNode elseBranch;
}
```

Truffle uses this for:
- Tree traversal
- Node replacement (specialization)
- Compilation boundaries

### VirtualFrame

Truffle's optimized stack frame:

```java
public Object executeGeneric(VirtualFrame frame) {
    // Read variable from slot 0
    Object value = frame.getObject(0);

    // Write to slot 1
    frame.setObject(1, result);
}
```

Benefits:
- Array-based (fast access)
- JIT-optimizable (becomes registers)
- Type-specialized slots

### Specialization

The Truffle DSL generates optimized code paths:

```java
@NodeChild("left")
@NodeChild("right")
public abstract class AddNode extends ReLangNode {

    @Specialization
    protected long addLongs(long left, long right) {
        return left + right;
    }

    // Could add more specializations for other types
}
```

Truffle generates `AddNodeGen` with:
- Automatic child evaluation
- Type checking
- Deoptimization on type changes

### DirectCallNode

Efficient function calls:

```java
public class ReLangInvokeNode extends ReLangNode {
    @Child private DirectCallNode callNode;

    public Object executeGeneric(VirtualFrame frame) {
        if (callNode == null) {
            callNode = insert(DirectCallNode.create(target));
        }
        return callNode.call(args);
    }
}
```

Truffle can:
- Inline the called function
- Devirtualize the call
- Optimize across boundaries

## Type System

Defined with a simple annotation:

```java
@TypeSystem({ long.class, boolean.class })
public class ReLangTypeSystem {
}
```

Truffle generates `ReLangTypeSystemGen` with type checking and conversion methods.

## Compilation Pipeline

### Phase 1: Interpretation

First executions run in the interpreter:
- Collects profiling information
- Identifies hot code paths
- Records type information

### Phase 2: Compilation

After warmup, Graal compiles hot methods:
- Uses profiling data for optimization
- Inlines frequently called functions
- Eliminates unnecessary checks

### Phase 3: Deoptimization

If assumptions are violated:
- Falls back to interpreter
- Recompiles with new information
- Adapts to changing program behavior

## How This Enables Resumability

Truffle's architecture makes resumability possible:

### Frame Access

`VirtualFrame` provides named slots:

```java
// Get all variable names and values
FrameDescriptor descriptor = frame.getFrameDescriptor();
for (int i = 0; i < descriptor.getNumberOfSlots(); i++) {
    String name = descriptor.getSlotName(i);
    Object value = frame.getValue(i);
    // Save to state
}
```

### Exception-Based Control Flow

Java exceptions work naturally with Truffle:

```java
try {
    child.executeGeneric(frame);
} catch (ReLangSuspendException e) {
    // Capture state, re-throw
}
```

### Node Tree Structure

The AST structure maps directly to execution path:

```java
// In ReLangBlockNode
for (int i = 0; i < statements.length; i++) {
    // 'i' is our position - save it on suspend
    statements[i].executeGeneric(frame);
}
```

## Performance Characteristics

| Aspect | Truffle/Graal | Traditional Interpreter |
|--------|---------------|------------------------|
| Startup | ~100ms | Instant |
| Peak performance | Near-native | 10-100x slower |
| Memory | Higher (JIT) | Lower |
| Warmup | Required | None |

## Native Image

GraalVM can compile Truffle languages to native binaries:

```bash
native-image --language:relang -jar relang.jar -o relang
```

Benefits:
- Fast startup (~10ms)
- Lower memory
- Self-contained binary

Tradeoff:
- No runtime JIT (AOT only)
- Slightly lower peak performance

## See Also

- [How Resumability Works](explanation-resumability.md)
- [GraalVM Documentation](https://www.graalvm.org/docs/)
- [Truffle Language Implementation Framework](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/)

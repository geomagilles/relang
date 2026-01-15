# How Resumability Works

This document explains the internal mechanics of ReLang's checkpoint and resume system.

## Overview

ReLang's resumability is built on a simple but powerful idea: when execution hits a `checkpoint;` statement, we capture the entire program state and allow it to be serialized. Later, we can restore that state and continue execution exactly where we left off.

## The Two Phases

### 1. Unwinding (Suspension)

When `checkpoint;` is executed:

```
Program execution
       │
       ▼
┌──────────────────┐
│ ReLangCheckpoint │ ← throws ReLangSuspendException
│      Node        │
└──────────────────┘
       │ exception propagates up
       ▼
┌──────────────────┐
│  ReLangBlock     │ ← catches, records statement index
│      Node        │   re-throws with path info
└──────────────────┘
       │
       ▼
┌──────────────────┐
│  ReLangRoot      │ ← catches, captures local variables
│      Node        │   adds FrameState to ResumableState
└──────────────────┘
       │ (repeat for each frame in call stack)
       ▼
┌──────────────────┐
│  TopLevelRoot    │ ← catches, wraps in SuspendedResult
│      Node        │   returns to host
└──────────────────┘
       │
       ▼
   Host receives SuspendedResult
   Serializes to file
```

Each level of the call stack contributes its state:
- **BlockNode**: Records which statement we were executing
- **RootNode**: Captures all local variable values

### 2. Rewinding (Resumption)

When resuming with saved state:

```
Host loads state from file
       │
       ▼
┌──────────────────┐
│  TopLevelRoot    │ ← sets resumeState in context
│      Node        │
└──────────────────┘
       │
       ▼
┌──────────────────┐
│  ReLangRoot      │ ← pops FrameState from stack
│      Node        │   restores local variables
└──────────────────┘
       │
       ▼
┌──────────────────┐
│  ReLangBlock     │ ← reads saved path index
│      Node        │   skips to that statement
└──────────────────┘
       │
       ▼
   Execution continues normally
```

## Key Components

### ReLangSuspendException

A special exception that carries the `ResumableState`:

```java
public class ReLangSuspendException extends RuntimeException {
    private final ResumableState state;

    // As it propagates up, each node adds its frame state
    public void addFrameState(FrameState frame) {
        state.pushFrame(frame);
    }
}
```

### ResumableState

A stack of `FrameState` objects representing the call stack:

```java
public class ResumableState {
    private String sourceHash;           // For validation
    private ArrayList<FrameState> frames; // Call stack

    // Bottom of stack = outermost function
    // Top of stack = innermost function (where checkpoint was)
}
```

### FrameState

State of a single function invocation:

```java
public class FrameState {
    private Map<String, Object> locals;      // Variable values
    private List<Integer> executionPath;     // Position in blocks
}
```

### Execution Path

The execution path tracks position within nested blocks:

```
fn example() {
    a = 1;              // Statement 0
    if (condition) {    // Statement 1
        b = 2;          //   Block statement 0
        while (x) {     //   Block statement 1
            c = 3;      //     While-block statement 0
            checkpoint; //     While-block statement 1  ← HERE
            d = 4;      //     While-block statement 2
        }
        e = 5;          //   Block statement 2
    }
    f = 6;              // Statement 2
}
```

If suspended at the checkpoint, execution path = `[1, 1, 1]`:
- `1`: In if-block (statement 1 of function)
- `1`: In while-block (statement 1 of if-block)
- `1`: At statement 1 of while-block

## How Nodes Participate

### ReLangCheckpointNode

Simply throws the exception:

```java
public Object executeGeneric(VirtualFrame frame) {
    throw new ReLangSuspendException(new ResumableState());
}
```

### ReLangBlockNode

Tracks position and records path on suspension:

```java
public Object executeGeneric(VirtualFrame frame) {
    int startIndex = getResumeIndex();  // 0 normally, or saved index

    for (int i = startIndex; i < statements.length; i++) {
        try {
            statements[i].executeGeneric(frame);
        } catch (ReLangSuspendException e) {
            e.addPathIndex(i);  // Record where we were
            throw e;
        }
    }
}
```

### ReLangRootNode

Captures/restores variables:

```java
public Object execute(VirtualFrame frame) {
    // REWINDING: Restore state if resuming
    if (hasResumeState()) {
        FrameState myState = popFrameState();
        restoreLocals(frame, myState);
    }

    try {
        return bodyNode.executeGeneric(frame);
    } catch (ReLangSuspendException e) {
        // UNWINDING: Capture this frame's state
        Map<String, Object> locals = captureLocals(frame);
        e.addFrameState(new FrameState(locals, e.getPath()));
        throw e;
    }
}
```

## Source Code Validation

To prevent subtle bugs from code changes:

1. **On suspend**: Compute SHA-256 hash of source code
2. **Store hash** in state file
3. **On resume**: Re-compute hash, compare to stored
4. **Mismatch**: Reject resume with error

This catches:
- Accidental code modifications
- Version mismatches in distributed systems
- Copy-paste errors

## Serialization Formats

### JSON

Human-readable, good for debugging:

```json
{
  "sourceHash": "a1b2c3...",
  "frames": [
    {"locals": {"x": 42}, "executionPath": [1, 2]}
  ]
}
```

### Protocol Buffers

Compact binary format:

```protobuf
message ResumableStateProto {
  string source_hash = 1;
  repeated FrameStateProto frames = 2;
}
```

## Limitations

### What Can Be Checkpointed

- Local variables (`long`, `boolean`)
- Call stack position
- Control flow state (which statement in each block)

### What Cannot Be Checkpointed

- External resources (file handles, network connections)
- Native/host objects
- Truffle-internal state (specializations, inline caches)

### Code Changes

The source code must be identical between suspend and resume. Even whitespace changes will invalidate the state.

## Design Decisions

### Why Exceptions?

Using exceptions for suspension:
- Natural stack unwinding
- Each frame can intercept and add state
- No need for explicit continuation passing

### Why Not Continuations?

First-class continuations would be more powerful but:
- Complex to implement correctly
- Harder to serialize
- Exception-based approach is sufficient for checkpointing

### Why Hash the Source?

- Simple to implement
- Catches most dangerous mismatches
- Alternative (version numbers) requires manual maintenance

## See Also

- [State File Format](reference-state-format.md) - Technical format details
- [Truffle Architecture](explanation-truffle.md) - How Truffle enables this
- [How to Suspend and Resume](howto-suspend-resume.md) - Practical usage

# ReLang Language Context

Current specification state and design constraints for ReLang.

## Design Phase: Active Development

**ReLang is a work in progress.** Key implications:

- **No backward compatibility concerns** — We can change anything
- **Complete latitude** for design decisions — Nothing is sacred
- **Bold changes welcome** — If something isn't right, fix it
- **Spec is a starting point** — Not a final constraint

The filename `relang-awaitables-final.md` is misleading — it represents **base work**, not a finished design. Everything is open for revision if there's a good reason.

## Core Purpose: Orchestrator

**ReLang is an orchestrator** — it coordinates external work, it doesn't do the work itself.

Key characteristics:
- Coordinates awaitables (external effects) via `and`/`or`
- Resumable from any checkpoint given saved state
- Does not execute in isolation — requires infrastructure

### Infrastructure Dependency

ReLang requires a **durable execution infrastructure** to run in production:

| Execution Mode | Use Case | Infrastructure |
|----------------|----------|----------------|
| End-to-end | Testing, development | Minimal — run to completion |
| Production | Durable workflows | Message processing, outbox pattern, checkpoint storage |

Production infrastructure typically includes:
- **Message processing**: Trigger workflow steps from queues
- **Outbox pattern**: Reliable event publication
- **Checkpoint storage**: Persist state for resume
- **Action executors**: Run external effects reliably

ReLang defines the **orchestration logic**; the infrastructure provides **durability and reliability**.

## Current Specification

The working spec is at `tmp/relang-awaitables-final.md`. Use as **reference and starting point**, not as constraint.

## Current Design Commitments

These represent the current direction, but **can be changed** if analysis reveals better alternatives:

### Single Execution Point

**A ReLang script has exactly ONE execution point at any time.**

This is a core implementation requirement:
- Single-threaded workflow execution (one "program counter")
- Parallelism exists only in external effects (awaitables doing work)
- Workflow code never runs concurrently with itself
- No race conditions within workflow logic
- No need for locks, mutexes, or synchronization primitives

Awaitables represent in-flight external operations, but the workflow code that coordinates them runs sequentially. When you write `t1 and t2`, both awaitables may be executing externally in parallel, but the ReLang code proceeds to a single next statement.

### Code Stability Constraint

**Code must not change during workflow lifetime.** Checkpoint state is coupled to the code that produced it — changing code risks state incompatibility on resume. This constraint may be relaxed in future design work, but for now workflows run to completion on the code version that started them.

### Schema Evolution Direction

**Inspiration: Protocol Buffers.** When schema evolution is addressed, protobuf's model will guide the approach — field numbers, additive-only changes, never reuse/remove IDs, optional fields with defaults. See [DISTRIBUTED-SYSTEMS.md](DISTRIBUTED-SYSTEMS.md) for details.

1. `*T` denotes an awaitable whose successful resolution yields `T`
2. Coordination operators are `and` (all succeed) and `or` (first wins)
3. Data operators are `&` (product) and `|` (sum)
4. `await()` preserves data shape — it unwraps `*`, not `&`/`|`
5. Single failure channel: `await e : T | Failure`

## Type System Summary

### Awaitable Types

| Type | Meaning | Has Identity | Await Result |
|------|---------|--------------|--------------|
| `*S` | Single operation | Yes (`id`) | `S \| Failure` |
| `*(A & B)` | Coordinated (all) | No | `(A & B) \| Failure` |
| `*(A \| B)` | Coordinated (any) | No | `A \| B \| [Failure]` |

### Data Types

**Products** — All components present:
```relang
A & B & C        // Type
(a & b & c)      // Value
let (x & y) = v  // Destructuring
```

**Sums** — One alternative:
```relang
A | B | C        // Type
match v { a: A => ..., b: B => ... }  // Discrimination
```

### Coordination Operators

**`and`** — All must succeed (fail-fast):
```relang
*A and *B : *(A & B)
// First failure → composite fails, remaining cancelled
```

**`or`** — First success wins (fail-last):
```relang
*A or *B : *(A | B)
// First success → composite succeeds, remaining cancelled
// All fail → [Failure] returned
```

## Failure Model

### Core Invariant

```relang
await e : T | Failure
```

All await expressions return either success or `Failure`.

### Failure Structure

```relang
struct Failure {
  error: ErrorData
  originId: String?
  occurredAt: Timestamp?
}
```

### Action-Family Error Contracts

Each action family defines sealed error types:

```relang
sealed HttpError
struct Timeout : HttpError { duration: Duration }
struct DnsFailure : HttpError { host: String }
struct HttpStatusError : HttpError { status: Int, body: Bytes? }
struct Cancelled : HttpError {}
```

**Contract**: If an HTTP action fails, `failure.error` is guaranteed to be `HttpError`.

### Failure Propagation

The `!` operator unwraps success or raises failure:
```relang
(await e)! : T   // Unwraps or raises Failure
```

## Cancellation Model

### Principles

1. Cancellation is a **request**, not a guarantee
2. Observed only at **resolution time**
3. Coordination defines **default propagation**

### API

```relang
cancel(t)    // Request cancellation (non-blocking)
shield(t)    // Block propagated cancellation
```

### Coordination Defaults

- `and`: Cancels remaining on first failure
- `or`: Cancels remaining on first success

`shield(t)` blocks propagated cancellation but not explicit `cancel(t)`.

## Timeout and Retry

### Timeout

```relang
timeout(t, d) : *T
```

- If `t` resolves before `d`: same result
- If `d` elapses: issues `cancel(t)`, returns `Failure(Timeout)`

### Retry

```relang
retry(t, policy) : *T
```

**Restriction**: Only for action awaitables, not coordination.

**Composition**:
```relang
retry(timeout(action, 2s), policy)      // Per-attempt timeout
timeout(retry(action, policy), 10s)     // Overall deadline
```

## Awaitable Interface

Single awaitables expose:
```relang
*S {
  id: String
  createdAt: Timestamp
  isResolved(): Bool
  isSucceeded(): Bool
  isFailed(): Bool
  resolvedAt(): Timestamp?
  data(): S?
  errors(): [Failure]?
  await(): S | Failure
}
```

Composite awaitables do **not** have `id`.

## Execution Semantics

### Normative Defaults

1. Awaitables are **eagerly started**
2. `and` is **fail-fast** (cancels on first failure)
3. `or` is **fail-last** (cancels on first success)
4. Failure lists preserve **temporal order**
5. Execution is **resumable from any checkpoint**

## List Coordination

For homogeneous lists:
```relang
and([*S]) : *[S]        // All succeed → list of results
or([*S])  : *S          // First success → single result
```

Await results:
```relang
and([*S]).await() : [S] | Failure
or([*S]).await()  : S | [Failure]
```

## Open Design Questions

Everything is potentially open for revision. Current areas of active exploration:

1. **Syntax for common patterns** (e.g., try/finally equivalent)
2. **Workflow-level constructs** (signals, queries, child workflows)
3. **Generic types** (polymorphism over awaitables)
4. **Module system** (imports, visibility)
5. **Standard library scope**
6. **Any aspect of current spec** — If analysis reveals a better approach, change it

When addressing these, prioritize **overall language consistency** over preserving existing decisions. If changing a "commitment" improves the language, propose the change with full ripple-effect analysis.

## Terminology

Use these terms consistently:

| Term | Meaning | Avoid |
|------|---------|-------|
| awaitable | In-flight effect (`*T`) | task, future, promise |
| coordinate | Combine awaitables | join, merge, combine |
| resolve | Complete (succeed or fail) | finish, complete |
| fail-fast | Cancel on first failure | early termination |
| fail-last | Require all to fail | all-or-nothing |
| action | External operation with side effects | activity, task |
| workflow | Durable execution context | process, saga |

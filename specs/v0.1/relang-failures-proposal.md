# ReLang Failure Model

*Error Data Model for Durable Execution and Propagation*

---

## 1. Overview

In ReLang, every `await` returns either success data or a `Failure` value:

```relang
await expr : T | Failure
```

The model must represent:

1. Leaf failures (an action failed),
2. Propagated failures (a parent await observes child failure),
3. Aggregated failures (`or` where all branches fail).

The key requirement is to preserve **causality** without ambiguity, especially for nested function chains.

---

## 2. Core Types

### 2.1 Failure Envelope

```relang
type Failure {
    id: String
    kind: FailureKind
    sourceId: String?           // awaitable id observed at this failure boundary
    failedAt: Timestamp
    execution: ExecutionRef

    // Exactly one of these may be set (or neither for leaf failures)
    cause: Failure?             // single-cause wrapping
    causes: [Failure]?          // multi-cause aggregation
}
```

| Field | Meaning |
|-------|---------|
| `id` | Stable identifier of this failure instance |
| `kind` | Failure classification at this boundary |
| `sourceId` | Awaitable id that failed at this boundary |
| `failedAt` | Timestamp at this boundary |
| `execution` | Which execution observed/produced this failure |
| `cause` | Underlying single failure (wrapping case) |
| `causes` | Underlying multiple failures (aggregation case) |

### 2.2 Execution Reference

```relang
type ExecutionRef {
    executionId: String
    functionName: String?
    parentExecutionId: String?
}
```

### 2.3 Invariants (Normative)

1. `cause` and `causes` cannot be set together.
2. `FunctionFailed` requires `cause != none`.
3. `AllFailed` requires `causes != none` and `causes.length > 0`.
4. Leaf kinds (`ActionFailed`, `ActionTimedOut`, `FunctionTimedOut`, `FunctionCancelled`) must have `cause == none` and `causes == none`.
5. Failure graphs are acyclic.
6. For `AllFailed`, `causes` are ordered by lexical branch order in source code.

---

## 3. Failure Kinds

```relang
sealed FailureKind

// Action-level (leaf)
type ActionFailed : FailureKind {
    actionType: String              // "http", "db", "grpc", ...
    actionName: String?             // method/operation if known
    error: ActionError              // family-specific payload
    attempts: [Attempt]
}

type ActionTimedOut : FailureKind {
    actionType: String
    actionName: String?
    timeout: Duration
    attempts: [Attempt]
}

// Function-level (wrapping / leaf)
type FunctionFailed : FailureKind {
    functionName: String
    childExecutionId: String
}

type FunctionTimedOut : FailureKind {
    functionName: String
    timeout: Duration
}

type FunctionCancelled : FailureKind {
    functionName: String
    cancelledAt: Timestamp
}

// Coordination-level (aggregation)
type AllFailed : FailureKind {
    operator: String      // "or"
    branchCount: Int
}
```

---

## 4. Attempts and Action Families

```relang
type Attempt {
    index: Int                      // 0-based
    startedAt: Timestamp
    endedAt: Timestamp
    workerId: String
    error: ActionError?
}
```

Each action family defines a sealed error family (`HttpError`, `DbError`, `GrpcError`, etc.).

Contract:

1. If `kind` is `ActionFailed` or `ActionTimedOut`,
2. then `kind.error` (or attempt errors) belongs to that action family error set.

---

## 5. Propagation Semantics

### 5.1 Leaf Failure Creation

When an external action fails terminally, runtime creates a leaf `Failure`:

1. `kind = ActionFailed` or `ActionTimedOut`,
2. `cause = none`, `causes = none`.

### 5.2 Propagation in Same Execution (`!`)

`x!` propagates the same `Failure` value upward in the current execution.  
No wrapping is introduced by `!` alone.

### 5.3 Inline Function Propagation (`f(...)`)

For an inline function call:

1. Caller and callee share the same execution context,
2. no distributed boundary is crossed,
3. propagated failures remain the same failure value (same `id`, same causal payload),
4. no `FunctionFailed` wrapper is created.

This keeps local/in-process propagation transparent.

### 5.4 Propagation Across Spawn Boundary (`spawn f(...)`)

If child execution returns `Failure`, parent `await spawn child(...)` yields a new wrapping failure:

1. parent `kind = FunctionFailed`,
2. parent `cause = <child failure>`,
3. parent `execution = parent execution ref`.

This makes the cross-execution boundary explicit while preserving the original root cause.

### 5.5 Aggregation (`or`)

For `await (a or b or c)`:

1. first success wins,
2. if all fail, runtime returns:
   1. `kind = AllFailed { operator: "or", branchCount: 3 }`,
   2. `causes = [fa, fb, fc]` in lexical branch order.

If one branch succeeds:

1. the language result is the winner value,
2. loser failures/cancellations do not alter language result shape,
3. loser operational details remain available via runtime observability.

### 5.6 `and` (Fail-Fast)

For `await (a and b)`:

1. first observed failure is returned as-is,
2. no `AllFailed` wrapper is created.

### 5.7 Summary: Inline vs Distributed

| Call mode | Execution boundary | Parent-observed failure |
|-----------|--------------------|-------------------------|
| `f(...)` (inline) | none | same failure value |
| `await spawn f(...)` | yes (child execution) | `FunctionFailed` wrapping child failure in `cause` |

### 5.8 Root Boundary

At root execution boundary:

1. an unhandled `Failure` terminates execution as `Failed(Failure)`,
2. no out-of-model exception is required for language semantics.

---

## 6. Inspection Helpers (Runtime API)

To make nested failures easy to consume, runtime should expose helpers:

```relang
failure.root()         // deepest single-cause leaf
failure.chain()        // [current, cause, cause.cause, ...]
failure.leafFailures() // all leaves (single leaf or flattened from causes tree)
```

These helpers avoid hand-written recursive traversal in user code.

---

## 6.1 Language vs Runtime Diagnostics

`Failure` is the language-level contract and should stay portable/stable.

Machine and platform details belong to runtime reporting:

```relang
type RuntimeFailureReport {
    failureId: String
    workerId: String?
    host: String?
    region: String?
    containerId: String?
    traceId: String?
    spanId: String?
    raw: Json?
}
```

Linking rule:

1. reports link to language failures by `failureId`,
2. runtime diagnostics are optional and must not change language matching behavior.

---

## 6.2 Scheduling and Versioning Notes

1. Awaitables have eager logical semantics; runtime scheduling may defer physical start.
2. Failure shape observed by user code must remain identical regardless of scheduling strategy.
3. Resume safety requires code-version compatibility policy at runtime (`codeVersion` and explicit migration for breaking changes).

---

## 7. Examples

### 7.1 Direct Action Failure

```relang
let r = await http.get(url)

match r {
    resp: HttpResponse -> handle(resp)
    f: Failure -> match f.kind {
        af: ActionFailed -> {
            log("HTTP action failed")
            log("attempts: ${af.attempts.length}")
        }
        at: ActionTimedOut -> log("Timed out after ${at.timeout}")
        _ -> log("Other failure")
    }
}
```

### 7.2 Nested Function Propagation

```relang
// child() -> ActionFailed (leaf)
// parent() await spawn child() -> FunctionFailed(cause=leaf)
// grandParent() await spawn parent() -> FunctionFailed(cause=FunctionFailed(...))

let r = await spawn grandParent()

match r {
    ok: Result -> ok
    f: Failure -> {
        let root = f.root()
        log("Top kind: ${f.kind}")
        log("Root kind: ${root.kind}")  // typically ActionFailed/ActionTimedOut
    }
}
```

### 7.3 `or` Aggregation with Nested Causes

```relang
let r = await (primary() or fallback() or cache())

match r {
    data: Data -> data
    f: Failure -> match f.kind {
        all: AllFailed -> {
            for leaf in f.leafFailures() {
                log("leaf failure: ${leaf.kind}")
            }
        }
        _ -> raise f
    }
}
```

---

## 8. Type Signatures

```relang
await action()                 : T | Failure
await spawn child()            : T | Failure
await (a and b)                : (A & B) | Failure
await (a or b)                 : A | B | Failure
```

---

## 9. Design Rationale

1. **Envelope + cause(s)** cleanly separates classification (`kind`) from causality.
2. **Single-cause vs multi-cause** removes ambiguity between wrapping and aggregation.
3. **Cross-execution wrapping** (`FunctionFailed`) makes distributed boundaries explicit.
4. **Stable structure** is compatible with serialized snapshots and observability tooling.
5. This aligns with durable workflow systems where parent-level failures wrap child/root errors while preserving causal traceability.

---

## 10. Summary

| Scenario | `kind` | `cause` | `causes` |
|----------|--------|---------|----------|
| Leaf action error | `ActionFailed` / `ActionTimedOut` | none | none |
| Parent observes failed child | `FunctionFailed` | single child failure | none |
| Spawn timeout | `FunctionTimedOut` | none | none |
| Spawn cancelled | `FunctionCancelled` | none | none |
| `or` all branches fail | `AllFailed` | none | list of branch failures |

---

*End of proposal*

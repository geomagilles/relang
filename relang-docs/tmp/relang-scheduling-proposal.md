# ReLang Scheduling Semantics

*Coordination Execution Order*

---

## 1. Overview

This proposal defines when and how awaitables are started, particularly within coordination operators (`and`, `or`).

**Core principle**: Awaitables are **eagerly started** when created, not when awaited.

---

## 2. Eager Execution Model

### 2.1 Start on Creation

```relang
let t1 = await http.get(url1)     // starts immediately
let t2 = await http.get(url2)     // starts immediately
let t3 = await http.get(url3)     // starts immediately

// All three are already in-flight
let results = (t1 and t2 and t3)
```

### 2.2 Not Lazy

```relang
let t = await http.get(url)       // starts NOW, not when awaited
// ... other code ...
let result = t      // just waits for completion
```

**Rationale**: Eager execution maximizes parallelism by default. Actions start as soon as they're defined.

---

## 3. Coordination Operator Semantics

### 3.1 `and` — Parallel Start, All Must Succeed

```relang
let combined = t1 and t2 and t3
```

**Execution**:
1. All operands are already started (eager)
2. Wait for all to complete
3. On first failure: cancel remaining, return failure
4. On all success: return product of results

**Timeline**:
```
t1: ──────────────────> success
t2: ──────────> success
t3: ────────────────────────> success
    │                        │
    start (all parallel)     await completes (all done)
```

### 3.2 `or` — Parallel Start, First Success Wins

```relang
let raced = t1 or t2 or t3
```

**Execution**:
1. All operands are already started (eager)
2. Wait for first success or all failures
3. On first success: cancel remaining, return that result
4. On all failure: return aggregated failure

**Timeline (first success)**:
```
t1: ──────────────────> (cancelled)
t2: ──────> success ─────────────────> (winner)
t3: ────────────────> (cancelled)
    │       │
    start   first success, others cancelled
```

**Timeline (all fail)**:
```
t1: ──────> failure
t2: ────────────> failure
t3: ──────────────────> failure
    │                   │
    start               all failed, return AggregateFailure
```

---

## 4. Hedged Execution

### 4.1 What Is Hedging?

Hedging starts backup requests after a delay, without waiting for failure.

### 4.2 Explicit Hedging

```relang
let hedged = hedge(http.get(url), delay: 100ms)
```

**Execution**:
1. Start first request
2. If no response after `delay`, start backup
3. Return first success, cancel the other

### 4.3 Why Not Default?

`or` does **not** hedge by default:

```relang
// This starts BOTH immediately, not sequentially
let result = (primaryService() or fallbackService())
```

**Rationale**:
- Hedging increases load on services
- Not appropriate for all use cases
- Explicit hedging is clearer

### 4.4 Sequential Fallback

For true fallback (try next only after failure):

```relang
fn withFallback(): *Response {
    match primaryService() {
        r: Response -> r
        f: Failure -> fallbackService()
    }
}
```

Or with explicit sequencing:

```relang
let result = firstOf([
    { primaryService() },
    { fallbackService() }
])
```

Where `firstOf` tries each in sequence until one succeeds.

---

## 5. Nested Coordination

### 5.1 Nesting Preserves Semantics

```relang
let complex = (t1 and t2) or (t3 and t4)
```

**Execution**:
1. All four awaitables start immediately (eager)
2. Inner `and` groups wait for both operands
3. Outer `or` returns first successful group
4. Losing group's remaining operations are cancelled

### 5.2 Example Timeline

```
t1: ──────────────────> success ──┐
t2: ──────────> success ──────────┼── (t1 and t2) succeeds
                                  │
t3: ────────────────────> (cancelled)
t4: ──────────────────────> (cancelled)
```

---

## 6. List Coordination

### 6.1 `and(list)` — Parallel All

```relang
let tasks = urls.map { url -> http.get(url) }
let results = and(tasks)!   // [Response]
```

**Execution**:
- All tasks started when `map` executes
- `and(tasks)` waits for all
- Fail-fast on first failure

### 6.2 `or(list)` — Parallel Race

```relang
let tasks = endpoints.map { ep -> http.get(ep) }
let result = or(tasks)!     // Response (first success)
```

**Execution**:
- All tasks started when `map` executes
- `or(tasks)` returns first success
- Others cancelled

---

## 7. Concurrency Limits

### 7.1 Problem

Eager execution can overload services:

```relang
let tasks = thousandUrls.map { url -> http.get(url) }
// 1000 concurrent requests!
```

### 7.2 Solution: Bounded Parallelism

```relang
let tasks = thousandUrls.map { url -> http.get(url) }
let results = and(tasks, maxConcurrency: 10)!
```

**Execution**:
- Start first 10 immediately
- As each completes, start next
- Maintain at most 10 in-flight

### 7.3 Batching Alternative

```relang
let batches = thousandUrls.chunked(10)
for batch in batches {
    let tasks = batch.map { url -> http.get(url) }
    let results = and(tasks)!
    process(results)
}
```

---

## 8. Ordering Guarantees

### 8.1 Start Order

Within a coordination expression, operands start left-to-right:

```relang
let result = (t1 and t2 and t3)
// t1 starts, then t2, then t3 (within same scheduler tick)
```

**Note**: This is observationally relevant only for side effects.

### 8.2 No Reordering

The runtime does **not** reorder starts for optimization:

```relang
let result = (slowSetup() and fastQuery())
// slowSetup starts first, even though fastQuery might finish faster
```

### 8.3 Result Order

`and` preserves positional correspondence:

```relang
let (a & b & c) = (t1 and t2 and t3)!
// a is result of t1, b is result of t2, c is result of t3
```

---

## 9. Cancellation Timing

### 9.1 When Cancellation Happens

For `and`:
- On first failure, remaining operands are cancelled immediately

For `or`:
- On first success, remaining operands are cancelled immediately

### 9.2 Cancellation Is Best-Effort

```relang
let result = (fast() or slow())
// fast() succeeds
// slow() receives cancel, but may have already completed
```

If `slow()` completes before receiving cancel, its result is discarded.

---

## 10. Determinism

### 10.1 Scheduling Is Deterministic

Given the same inputs, coordination executes identically:

- Same start order
- Same memoized results on replay
- Same cancellation timing (by logical clock)

### 10.2 Replay Semantics

On replay:
1. Already-completed actions return memoized results
2. Coordination logic re-executes with cached results
3. Cancellation decisions are the same

---

## 11. Summary

| Aspect | Behavior |
|--------|----------|
| **Start timing** | Eager (on creation, not await) |
| **`and` execution** | All parallel, fail-fast |
| **`or` execution** | All parallel, first-success |
| **Hedging** | Explicit only, not default |
| **Start order** | Left-to-right |
| **Result order** | Positional correspondence |
| **Cancellation** | Immediate on resolution |
| **Concurrency limits** | Explicit `maxConcurrency` parameter |
| **Determinism** | Fully deterministic on replay |

**Key principles:**
- Awaitables start immediately when created
- Coordination operators combine already-running awaitables
- No implicit hedging or sequential fallback
- Deterministic execution for replay safety

---

*End of proposal*

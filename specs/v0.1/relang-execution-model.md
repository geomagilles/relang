# ReLang Execution Model

*Snapshot-Based Durable Execution*

---

## 1. Overview

ReLang is a **durable execution language**. All code executes with durability guarantees: checkpoints, recovery, and reliable resume after failures.

**Core principles:**

- **Everything is durable** — All execution state can be persisted and resumed
- **Snapshot, not replay** — State is captured at checkpoints; resume restores state directly
- **One function type, two calling modes** — `f()` runs inline, `spawn f()` runs in parallel
- **Self-contained functions** — Functions access only parameters, enabling distribution
- **Explicit effects** — Async operations return `*T` awaitable types (see [ReLang v0.1 Canonique](relang-spec-v0.1-canonique.md))

---

## 2. The Snapshot Model

### 2.1 Checkpoints

Every `await` that may suspend creates a **checkpoint** — a snapshot of execution state.

```relang
fn example(): Int {
    let a = await step1()    // checkpoint 1
    let b = await step2(a)   // checkpoint 2
    let c = await step3(b)   // checkpoint 3
    a! + b! + c!
}
```

The `await` keyword resolves an asynchronous operation and persists the result when suspension occurs. If execution fails after checkpoint 2, it resumes with `a` and `b` already available — `step1()` and `step2()` are not called again.

At each checkpoint:
1. Current local variable values are captured
2. Execution position is recorded
3. State is persisted to durable storage

### 2.2 Resume Behavior

On resume (after failure, restart, or migration):

```
Before failure:
    checkpoint 1 ──► checkpoint 2 ──► [FAILURE]

After resume:
    [RESTORE checkpoint 2] ──► checkpoint 3 ──► complete
```

- Execution resumes **from the last checkpoint**
- No code is re-executed
- Local variables have their captured values
- Completed actions are not re-invoked

### 2.3 Comparison with Replay Systems

| Aspect | Snapshot Model (ReLang) | Replay Model (Temporal) |
|--------|-------------------------|-------------------------|
| **Resume** | Restore state, continue | Re-execute from start |
| **Past actions** | Not re-invoked | Memoized results returned |
| **Determinism** | Not strictly required | Strictly required |
| **Code changes** | State-compatible changes OK | Any change can break |
| **Performance** | O(1) resume | O(n) for n actions |

### 2.4 What Is Captured

Each checkpoint captures:

| Component | Captured | Notes |
|-----------|----------|-------|
| Local variables | Yes | Serialized values |
| Execution position | Yes | Which checkpoint |
| `self.id` | Yes | Execution identity |
| `now()` value | Yes | Wall-clock time when captured |
| Pending awaitables | Yes | References to in-flight actions |

All local variables must be serializable. This is enforced by the type system.

---

## 3. Functions

### 3.1 Declaration

```relang
fn name(parameters): ReturnType {
    body
}
```

Or with expression body:

```relang
fn name(parameters): ReturnType = expression
```

Return type annotations are optional — the compiler infers them.

For detailed syntax, see [ReLang v0.1 Canonique](relang-spec-v0.1-canonique.md).

### 3.2 Self-Contained (No Scope Capture)

Functions can only access their parameters — not variables from enclosing scope:

```relang
let config = loadConfig()

fn processOrder(orderId: String): OrderResult {
    let rate = config.rate  // ERROR: cannot access 'config'
}

// Correct: pass everything as parameters
fn processOrder(orderId: String, rate: Float): OrderResult {
    let discount = price * rate  // OK
}
```

**Why self-contained?**

- Functions may run on different servers (when spawned)
- All data must be serialized and passed explicitly
- Explicit data flow aids debugging and understanding

### 3.3 Calling Modes

**Inline call** — runs in current execution:

```relang
let total = calculateTotal(order.items)  // returns T directly
```

- Returns `T` directly
- Shares `self.id` with caller
- Runs in same execution context

**Spawn call** — creates parallel execution:

```relang
let task = spawn processOrder("order-123")  // returns *T
let result = await task!                     // await the result
```

- Returns `*T` (awaitable)
- Creates new execution context with own `self.id`
- May run on a different server

### 3.4 Comparison: Inline vs Spawn

| Aspect | `f()` (inline) | `spawn f()` (parallel) |
|--------|----------------|------------------------|
| **Returns** | `T` (direct value) | `*T` (awaitable) |
| **Execution** | Same context | New context |
| **`self.id`** | Inherits caller's | New unique id |
| **Server** | Same as caller | May be different |
| **Use case** | Helpers, sequential | Parallelism, distribution |

### 3.5 Execution Model Diagram

```
spawn call:
    Caller:     ──────●─────────────────────●──────►
                      │ spawn               │ await
                      ▼                     │
    Spawned:          ════════════════════►─┘ (parallel execution)

inline call:
    Caller:     ──────[fn runs here]───────────────►
                      (inline, same execution)
```

### 3.6 Parallel Processing

```relang
// Spawn multiple parallel executions
let t1 = spawn processOrder("order-1")
let t2 = spawn processOrder("order-2")
let t3 = spawn processOrder("order-3")

// All three run simultaneously
let (r1 & r2 & r3) = await (t1 and t2 and t3)
```

---

## 4. Execution Context (`self`)

### 4.1 Properties

Every execution has access to its context via `self`:

```relang
fn example(): Info {
    Info {
        id: self.id,
        createdAt: self.createdAt,
        parentId: self.parentId
    }
}
```

| Property | Type | Description |
|----------|------|-------------|
| `self.id` | `String` | Current execution id |
| `self.createdAt` | `Timestamp` | When execution started |
| `self.parentId` | `String?` | Parent execution id (`none` if root) |

### 4.2 Context Inheritance

```relang
fn helper(): String {
    self.id
}

fn main(): Unit {
    log(self.id)              // "exec-main-123"
    log(helper())             // "exec-main-123" (inherited, inline)

    let task = spawn helper()
    log(await task)          // "exec-helper-456" (new id, spawned)
}
```

- Inline calls inherit `self` from caller
- Spawned calls get a new `self` with new id
- Spawned calls have `self.parentId` referencing the parent's `self.id`

### 4.3 Explicit Execution ID (Idempotency)

```relang
@executionId("order-${orderId}")
fn processOrder(orderId: String): OrderResult {
    // If execution with this ID exists, return its result
}

spawn processOrder("123")  // uses id "order-123"
spawn processOrder("123")  // returns existing result (idempotent)
```

---

## 5. Time Handling

### 5.1 Wall-Clock Time

`now()` returns the **actual wall-clock time**:

```relang
fn example(): Duration {
    let start = now()         // e.g., 10:00:00.001
    await doWork()
    let end = now()           // e.g., 10:00:05.342
    end - start               // actual elapsed time
}
```

Time values are captured in checkpoints like any other variable. On resume, the captured values are restored exactly.

### 5.2 Resume and Time

On resume, captured time values are restored, but new `now()` calls return current time:

```relang
fn example(): (Timestamp, Timestamp) {
    let t1 = now()           // e.g., 10:00:00, captured in checkpoint
    await action()           // checkpoint at 10:00:05
    // [FAILURE AND RESUME AT 10:30:00]
    let t2 = now()           // 10:30:00 — actual current time
    (t1, t2)                 // (10:00:00, 10:30:00)
}
```

- `t1` was captured before the checkpoint → restored as 10:00:00
- `t2` is computed after resume → returns actual time 10:30:00

This means code can detect that time has passed during a failure/resume cycle, which is important for deadline handling and business logic.

### 5.3 Timer

```relang
timer(5s)              // wait 5 seconds
timer(deadline)        // wait until timestamp
```

---

## 6. State Compatibility

### 6.1 Safe Code Changes

Unlike replay systems, ReLang allows certain code changes without breaking existing executions:

**Safe changes:**
- Adding new code after the last checkpoint
- Changing code that hasn't executed yet
- Adding new variables after checkpoints
- Bug fixes in unexecuted code paths

```relang
// Original
fn process(): Result {
    let a = await step1()
    step2(a)  // bug here
}

// Fixed — safe if execution hasn't reached step2
fn process(): Result {
    let a = await step1()
    step2Fixed(a)  // fix applied
}
```

### 6.2 Breaking Changes

Changes that affect captured state can break resume:

**Breaking changes:**
- Removing or renaming captured variables
- Changing types of captured variables
- Reordering checkpoints

```relang
// Original
fn process(): Result {
    let order = await fetchOrder()
    let payment = await processPayment(order)
    // checkpoint has: order, payment
}

// Breaking — removes 'order' which is in checkpoint
fn process(): Result {
    let payment = await processPayment(await fetchOrder()!)!
    // checkpoint doesn't match
}
```

### 6.3 Versioned State Migration

For breaking changes, use version-aware state migration:

```relang
@stateVersion(2)
fn process(): Result {
    // Runtime checks state version and migrates if needed
    let order = await fetchOrder()!
    let payment = await processPayment(order)!
    OrderResult { order, payment }
}
```

---

## 7. Random Numbers

### 7.1 Seeded Randomness

Random values are seeded from execution context and captured in checkpoints:

```relang
fn example(): Int {
    let value = random()           // seeded from self.id + position
    await action()                 // checkpoint captures 'value'
    // After resume: value is restored, not regenerated
    value
}
```

### 7.2 Random Functions

```relang
random()              // Float 0.0..1.0
randomInt(0, 100)     // Int in range
randomChoice(list)    // Element from list
```

---

## 8. File Structure

### 8.1 File Contents

A ReLang file contains declarations only:
- **Functions (`fn`)**: reusable units of work
- **Types / sealed types / imports**

Top-level executable statements are not part of v0.1.

```relang
// orders.rl

fn processOrder(orderId: String): OrderResult {
    let order = await fetchOrder(orderId)!
    let total = calculateTotal(order.items)
    let payment = await processPayment(order, total)!
    OrderResult { orderId: orderId, paymentId: payment.id }
}

fn calculateTotal(items: [Item]): Int {
    items.map { it.price }.sum()
}
```

### 8.2 Starting an Execution

Executions are started by the runtime by targeting a function (module + function + args), for example:

```text
runtime.start(module="orders", function="processOrder", args=["order-123"])
```

- The entrypoint is an explicit function call, not top-level script code
- Checkpoint/resume semantics apply to that function execution

### 8.3 Importing Functions

```relang
import orders

let result = await spawn orders.processOrder("order-123")!
```

- All functions are importable
- Caller decides whether to spawn or call inline

---

## 9. Child Executions

### 9.1 Spawning Children

```relang
fn parent(): Result {
    // Each spawn creates a child execution
    let child1 = spawn childFn("arg1")
    let child2 = spawn childFn("arg2")

    let (r1 & r2) = await (child1 and child2)!
    combine(r1, r2)
}

fn childFn(arg: String): ChildResult {
    // self.parentId = parent's self.id
    log("Parent: ${self.parentId}")
    // ... work ...
}
```

### 9.2 Properties

- Child has its own `self.id`
- Child has its own checkpoint history
- Child can outlive parent (if not cancelled)
- Child's `self.parentId` references parent's `self.id`

---

## 10. Annotations

### 10.1 Execution ID

```relang
@executionId("order-${orderId}")
fn processOrder(orderId: String): Result {
    // idempotent by order ID
}
```

### 10.2 State Version

```relang
@stateVersion(2)
fn process(): Result {
    // enables state migration for breaking changes
}
```

---

## 11. Debugging and Testing

### 11.1 Checkpoint Inspection

```relang
// In debugger or admin console
inspect(executionId, checkpointNumber)
// Returns: { variables: {...}, position: 5, time: "..." }
```

### 11.2 State Replay (Debug Only)

```relang
// Debug tool
replayFrom(executionId, checkpoint: 3, overrides: { order: modifiedOrder })
```

### 11.3 Checkpoint Testing

```relang
test "resume produces same result" {
    let exec = startExecution(myFn, params)
    advanceTo(exec, checkpoint: 2)

    let state = captureState(exec)
    let result1 = completeExecution(exec)

    let resumed = resumeFrom(state)
    let result2 = completeExecution(resumed)

    assert result1 == result2
}
```

### 11.4 Failure Injection

```relang
test "handles failure at checkpoint 2" {
    let exec = startExecution(myFn, params)
    injectFailureAt(exec, checkpoint: 2)

    let result = runToCompletion(exec)
    assert result.checkpointCount > 2  // resumed and continued
}
```

---

## 12. Best Practices

### 12.1 Checkpoint Frequency

More checkpoints = more durability, more storage. Fewer = less overhead, more re-work on failure.

```relang
fn example(): Result {
    // Each await that blocks creates a checkpoint
    let a = await step1()     // checkpoint
    let b = await step2()     // checkpoint
    let c = await step3()     // checkpoint

    // Batch if steps are fast and failure is rare
    let (a & b & c) = await (step1() and step2() and step3())  // single checkpoint
}
```

### 12.2 State Size

Keep captured state small:

```relang
// Large state — avoid
fn example(): Result {
    let allData = await fetchAllData()!  // 10MB captured
    process(allData)
}

// Smaller state — preferred
fn example(): Result {
    let dataRef = await fetchDataReference()!  // small reference
    let data = await loadData(dataRef)!        // load when needed
    process(data)
}
```

---

## 13. Summary

| Aspect | Behavior |
|--------|----------|
| **Checkpoints** | Created at suspension points (`await` that blocks, long waits) |
| **Resume** | Restore state from checkpoint, continue |
| **Re-execution** | None — state is restored |
| **Time** | Wall-clock; captured values restored on resume |
| **Actions** | Completed actions not re-invoked |
| **Code changes** | Safe if state-compatible |
| **Variables** | Must be serializable |
| **Functions** | Self-contained, can be spawned |
| **`self`** | Execution identity and metadata |

**Core guarantee**: Execution can be interrupted at any checkpoint and reliably resumed with the exact same state.

---

## See Also

- [ReLang v0.1 Canonique](relang-spec-v0.1-canonique.md) — Normative language/runtime surface
- [ReLang Failure Model](relang-failures-proposal.md) — Causal failure envelope and propagation rules

---

*End of specification*

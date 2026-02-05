# ReLang Awaitable Lifecycle

*Introspection and State Queries*

---

## 1. Overview

Awaitables have a lifecycle from creation to resolution. ReLang provides methods to inspect this lifecycle without awaiting.

**Core principle**: Introspection is read-only and deterministic on replay.

---

## 2. Awaitable States

### 2.1 State Diagram

```
┌─────────────┐
│   Created   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Pending   │ ◄──── Running, waiting for result
└──────┬──────┘
       │
       ├─────────────────┐
       ▼                 ▼
┌─────────────┐   ┌─────────────┐
│  Succeeded  │   │   Failed    │
└─────────────┘   └─────────────┘
       │                 │
       └────────┬────────┘
                ▼
         ┌─────────────┐
         │  Resolved   │ (terminal)
         └─────────────┘
```

### 2.2 State Transitions

| From | To | Trigger |
|------|-----|---------|
| Created | Pending | Immediate (eager start) |
| Pending | Succeeded | Action completes successfully |
| Pending | Failed | Action fails or is cancelled |

---

## 3. Introspection Methods

### 3.1 State Queries

```relang
let t: *User = fetchUser(id)

t.isResolved()      // Bool — has completed (success or failure)
t.isSucceeded()     // Bool — completed successfully
t.isFailed()        // Bool — completed with failure
t.isCancelled()     // Bool — failed due to cancellation
```

### 3.2 Identity

```relang
t.id                // String — unique action identifier
t.createdAt         // Timestamp — when action was created
```

### 3.3 Result Access (After Resolution)

```relang
t.data()            // T? — success value (None if not succeeded)
t.failure()         // Failure? — failure (None if not failed)
t.resolvedAt()      // Timestamp? — when resolved (None if pending)
```

---

## 4. Semantics

### 4.1 Point-in-Time Snapshot

Introspection methods return the state at the moment of call:

```relang
let t = longOperation()

t.isResolved()      // false (still running)
// ... time passes ...
t.isResolved()      // maybe true now
```

### 4.2 Stable After Resolution

Once resolved, introspection results are stable:

```relang
let t = operation()
t           // wait for completion

t.isResolved()      // always true from here
t.isSucceeded()     // stable value
t.data()            // stable value
```

### 4.3 Determinism Guarantee

On replay, introspection returns consistent results:

```relang
fn example(): Bool {
    let t = action()
    t
    t.isSucceeded()     // same on original and replay
}
```

---

## 5. When to Use Introspection

### 5.1 Polling (Generally Avoid)

```relang
// ✗ Avoid polling — use await instead
while not t.isResolved() {
    self.sleep(100ms)
}
let result = t.data()!

// ✓ Better — just await
let result = t!
```

### 5.2 Selective Waiting

```relang
let t1 = fastOperation()
let t2 = slowOperation()

// Wait for fast one, check if slow is done
let r1 = t1!

if t2.isResolved() {
    let r2 = t2.data()!
    return combine(r1, r2)
} else {
    return r1   // proceed without t2
}
```

### 5.3 Logging and Monitoring

```relang
let t = criticalOperation()

// Log progress
log("Started: ${t.id} at ${t.createdAt}")

let result = t

if t.isSucceeded() {
    log("Completed: ${t.id} at ${t.resolvedAt()!}")
} else {
    log("Failed: ${t.id} - ${t.failure()!.error}")
}
```

### 5.4 Conditional Cancellation

```relang
let primary = primaryService()
let backup = backupService()

let r = primary

if primary.isSucceeded() {
    cancel(backup)      // don't need backup
    return r!
} else {
    return backup!  // fallback to backup
}
```

---

## 6. Composite Awaitable Introspection

### 6.1 Coordination Results

```relang
let combined = t1 and t2 and t3

combined.isResolved()   // true when coordination resolves
combined.isSucceeded()  // true if all succeeded (for 'and')
```

### 6.2 No Partial Results

Composite awaitables don't expose individual states:

```relang
let combined = t1 and t2

// ✗ NOT available
combined.t1.isResolved()

// ✓ Use original handles
t1.isResolved()
t2.isResolved()
```

### 6.3 Preserving Handles

```relang
// Keep individual handles for introspection
let t1 = operation1()
let t2 = operation2()
let t3 = operation3()

let combined = t1 and t2 and t3

// Can still check individuals
if t1.isResolved() and t2.isResolved() {
    log("First two done, waiting on third")
}

let result = combined!
```

---

## 7. Data Access Rules

### 7.1 Before Resolution

```relang
let t = operation()

t.data()            // None (not yet succeeded)
t.failure()         // None (not yet failed)
t.resolvedAt()      // None (not yet resolved)
```

### 7.2 After Success

```relang
let t = operation()
t           // succeeded

t.data()            // Some(value)
t.failure()         // None
t.resolvedAt()      // Some(timestamp)
```

### 7.3 After Failure

```relang
let t = operation()
t           // failed

t.data()            // None
t.failure()         // Some(Failure { ... })
t.resolvedAt()      // Some(timestamp)
```

---

## 8. Replay Considerations

### 8.1 Introspection Is Memoized

```relang
fn example(): String {
    let t = action()
    t

    // These return same values on replay
    let succeeded = t.isSucceeded()
    let time = t.resolvedAt()

    "${succeeded} at ${time}"
}
```

### 8.2 Pre-Resolution Queries

Queries before resolution may vary on replay:

```relang
fn risky(): Bool {
    let t = action()

    // ✗ Risky — may differ on replay
    let wasResolved = t.isResolved()    // timing-dependent!

    t
    wasResolved
}
```

**Guideline**: Only rely on post-resolution introspection for determinism.

---

## 9. Interface Summary

### 9.1 Awaitable Interface

```relang
interface Awaitable<T> {
    // Identity
    id: String
    createdAt: Timestamp

    // State queries
    isResolved(): Bool
    isSucceeded(): Bool
    isFailed(): Bool
    isCancelled(): Bool

    // Result access (after resolution)
    data(): T?
    failure(): Failure?
    resolvedAt(): Timestamp?

    // Resolution
    await(): T | Failure
}
```

### 9.2 Primitive vs Composite

| Method | Primitive `*T` | Composite `*(A & B)` |
|--------|----------------|----------------------|
| `id` | Unique ID | Not available |
| `isResolved()` | ✓ | ✓ |
| `isSucceeded()` | ✓ | ✓ |
| `data()` | `T?` | `(A & B)?` |

---

## 10. Summary

| Method | Returns | When Valid |
|--------|---------|------------|
| `id` | `String` | Always |
| `createdAt` | `Timestamp` | Always |
| `isResolved()` | `Bool` | Always |
| `isSucceeded()` | `Bool` | Always (false if pending) |
| `isFailed()` | `Bool` | Always (false if pending) |
| `isCancelled()` | `Bool` | Always (false if pending/succeeded) |
| `data()` | `T?` | After success |
| `failure()` | `Failure?` | After failure |
| `resolvedAt()` | `Timestamp?` | After resolution |

**Key principles:**
- Introspection is read-only
- Results stable after resolution
- Deterministic on replay (post-resolution)
- Use await for waiting, introspection for inspection

---

*End of proposal*

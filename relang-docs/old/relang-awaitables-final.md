# Relang — Awaitable Types

*Language Design Specification*

---

## 1. Overview

Relang introduces **awaitable types**, denoted by the `*` prefix.

An awaitable represents an **in-flight effect** whose resolution may succeed or fail. Awaitables are **not data types**.

Resolution is explicit via `.await()`.

---

## 2. Design Commitments (Normative)

1. `*T` denotes an awaitable whose successful resolution yields a value of type `T`.
2. Awaitables compose through **coordination operators**:
   - `and` — all must succeed (fail-fast)
   - `or` — first success wins (fail-last)
3. Coordination operators (`and`, `or`) are **effect-level only**.
4. Data construction operators are:
   - `&` — product (tuple) types and values
   - `|` — sum (union) types and values
5. `await()` **preserves data shape**: it unwraps `*` and does not rewrite `&` / `|`.

---

## 3. Failure Model (Normative)

Relang uses a coordination-driven error model:

| Coordination form | Success condition | `await()` failure payload |
|------------------|-------------------|---------------------------|
| Single awaitable | one success | `Failure` |
| `and` | all succeed | `Failure` (first temporal failure) |
| `or` | any succeeds | `[Failure]` (all failures) |

This rule applies uniformly.

---

## 4. Data Types

### 4.1 Product Types (`&`)

A **product type** combines multiple values.

```relang
A & B
A & B & C
```

Properties:

- Associative and flattened: `(A & B) & C` ≡ `A & B & C`
- Order is significant
- Canonical representation of tuples

#### Product values

```relang
let v: (A & B) = (a & b)
```

#### Destructuring

```relang
let (a & b) = v
```

### 4.2 Sum Types (`|`)

A **sum type** represents one of several alternatives.

```relang
A | B | C
```

---

## 5. Awaitable Types

| Awaitable type | Meaning | Has `id` | `await()` result |
|---------------|--------|----------|------------------|
| `*S` | single async operation | ✓ | `S \| Failure` |
| `*(A & B)` | coordinated (all) | ✗ | `(A & B) \| Failure` |
| `*(A \| B)` | coordinated (any) | ✗ | `A \| B \| [Failure]` |

Notes:

- `S` is typically a struct type returned by an activity.
- Composite awaitables do not have identity (`id`).

### Identity and Awaitable Provenance

The presence of an awaitable type *T does not imply that the awaitable has identity.
Identity (id) is a property of primitive asynchronous operations, not of the value type T they eventually produce.

Awaitables created directly by an activity or external call have stable identity and expose id.
Awaitables produced by coordination (and, or) are composite awaitables: even if their type is *T, they do not have identity and must not expose id.

This distinction ensures that identity reflects execution provenance rather than data shape, and prevents coordination constructs from being mistaken for single operations.

---

## 6. Single Awaitable — `*S`

### Semantics

A `*S` represents a single asynchronous operation with identity.

### Interface

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

---

## 7. Coordination: `and` (All Must Succeed)

### Typing Rules

```relang
*A and *B         : *(A & B)
*A and *B and *C  : *(A & B & C)
```

### Await Rules

```relang
await(*(A & B))        : (A & B) | Failure
await(*(A & B & C))    : (A & B & C) | Failure
```

### Semantics

- All awaitables must succeed.
- The first failure resolves the composite as failed (fail-fast).
- Remaining awaitables may be cancelled.

### Example

```relang
let t1: *User   = db.query("SELECT * FROM users WHERE id = 1")
let t2: *Orders = db.query("SELECT * FROM orders WHERE user_id = 1")

let joined: *(User & Orders) = t1 and t2
let r = joined.await()

match r {
  (user & orders) => print("${user.name} has ${orders.count} orders")
  err: Failure      => print("Failed: ${err}")
}
```

---

## 8. Coordination: `or` (First Success Wins)

### Typing Rules

```relang
*A or *B         : *(A | B)
*A or *B or *C   : *(A | B | C)

### Await Rules

```relang
await(*(A | B))        : A | B | [Failure]
await(*(A | B | C))    : A | B | C | [Failure]

- The first success resolves the composite as succeeded (fail-last).
- Failures are accumulated.
- Remaining awaitables may be cancelled after first success.

### Example

```relang
let primary:  *Data = http.get("https://primary.api.com/data")
let fallback: *Data = http.get("https://fallback.api.com/data")

let raced: *Data = primary or fallback
let r = raced.await()

match r {
  d: Data        => print("Got data")
  errs: [Failure]  => print("All failed: ${errs}")
}
```

---

## 9. Nested Composition

Awaitables compose freely; types track the resulting data shape.

```relang
(t1 and t2) or t3   : *((A & B) | C)
(t1 or t2) and t3   : *((A | B) & C)
```

Example:

```relang
let combined: *((User & Orders) | CachedData) = (t1 and t2) or t3
let r = combined.await()

match r {
  (user & orders)     => ...
  cached: CachedData  => ...
  errs: [Failure]   => ...
}
```

---

## 10. Awaitables from Lists

For homogeneous lists, use functions.

```relang
and([*S]) : *[S]
or([*S])  : *S
```

Await results:

- `and([*S]).await() : [S] | Failure`
- `or([*S]).await()  : S | [Failure]`

Notes:

- List coordination yields lists, not products.
- Heterogeneous lists are inferred as `[*(A | B | ...)]` and lose positional type precision.

---

## 11. Identity Semantics (Normative)

- Only awaitables originating from a single async operation have `id`.
- Composite awaitables are coordination constructs and do not have `id`.
- Identity is stable within a workflow and across retries.

---

## 12. Execution Defaults (Normative)

- Awaitables are eagerly started.
- `and` cancels remaining awaitables on first failure.
- `or` cancels remaining awaitables on first success.
- Failure lists preserve temporal order.

---

## 13. Summary

- `and` / `or` coordinate effects.
- `&` / `|` construct data shapes.
- Coordination directly yields awaitables of those data shapes:
  - `*A and *B : *(A & B)`
  - `*A or  *B : *(A | B)`
- `await()` unwraps `*` and preserves `&` / `|` without rewriting.
- Commas are optional sugar in patterns/destructuring only.

---

*End of section*

## Failure

### Overview

`Failure` represents the **inability to produce a successful value from an awaitable**.

It is **not** a domain error value.  
It is the dedicated failure channel of `await`.

All `await` expressions return either a success value or a `Failure`.

```relang
e : *T
await e : T | Failure
```

`Failure` is semantically equivalent to `Failure<*>`:  
a failure payload exists, but its concrete type is **not statically known**.

---

### Failure Payload

A `Failure` carries a structured **error payload** describing the cause of the failure.

```relang
struct Failure {
  error: ErrorData
  originId: String?
  occurredAt: Timestamp?
}
```

- `error` is **data**, not an exception and not a base class.
- `ErrorData` is a value from a shared, serializable error space (structs, unions, lists, primitives).
- The language does **not** impose a hierarchy on error data.

---

### Action-Specific Error Data

Each action family defines its own set of error structs.

Example: HTTP actions

```relang
struct Timeout {
  duration: Duration
}

struct DnsFailure {
  host: String
}

struct HttpStatusError {
  status: Int
  body: Bytes?
}
```

An HTTP awaitable returns:

```relang
await http.get(url) : HttpResponse | Failure
```

When a failure occurs, `failure.error` is guaranteed (by the action contract) to be one of the HTTP error structs above, or a generic failure such as cancellation.

---

### Handling Failures

Failures are handled by pattern matching on the failure payload.

```relang
let r = await http.get("https://google.com")

match r {
  resp: HttpResponse => ...
  f: Failure => match f.error {
    t: Timeout          => ...
    d: DnsFailure       => ...
    s: HttpStatusError  => ...
    _                   => ...
  }
}
```

This model keeps error data **non-hierarchical** and fully pattern-matchable.

---

### Failure Aggregation (`or`)

Coordination with `or` produces a single `Failure` on total failure.

```relang
await (a or b) : T | Failure
```

If all branches fail, the returned `Failure` contains an aggregated payload.

```relang
struct AggregateFailure {
  failures: list<Failure>
}
```

The aggregate is carried as:

```relang
Failure {
  error: AggregateFailure { failures = [...] }
}
```

This preserves a **single failure channel** while retaining full detail.

---

### Failure Propagation (`!`)

The postfix operator `!` unwraps a successful value or propagates a `Failure`.

```relang
(await e)! : T
```

Semantics:

```relang
x! ≡ match x {
  v: T       => v
  f: Failure => raise f
}
```

Only `Failure` may be raised by `!`.  
Domain error data (`Timeout`, `DnsFailure`, etc.) is **never raised directly**.

---

### Identity and Failure

If a failure originates from a primitive awaitable, `originId` refers to that awaitable’s `id`.

Failures produced by coordination (`and`, `or`) may have no single origin and may leave `originId` unset.

---

### Design Rationale

- `Failure` separates **execution failure** from **domain data**.
- Error payloads remain plain data, without inheritance or hierarchy.
- Signatures stay stable (`T | Failure`) under composition.
- Aggregation and propagation remain explicit and deterministic.

## Action-Family Failure Contracts

### Overview

Relang keeps the failure channel uniform:

```relang
await e : T | Failure
```

However, the **payload** carried by `Failure.error` is **action-family specific** by *contract*.

This preserves simple signatures while retaining strongly structured error data.

---

### Sealed Error Families

Each action family defines a **sealed error family** for its failure payloads.

Example: HTTP actions

```relang
sealed HttpError

struct Timeout : HttpError { duration: Duration }
struct DnsFailure : HttpError { host: String }
struct HttpStatusError : HttpError { status: Int, body: Bytes? }
```

---

### Normative Contract

For each action family `F`, the specification defines a sealed error family `FError` (e.g., `HttpError`).

**Contract rule (normative):**

> If an awaited action from family `F` fails, then the returned `Failure.error` is guaranteed to be an instance of `FError` (including any generic failures explicitly defined as part of that family).

Example:

```relang
await http.get(url) : HttpResponse | Failure
```

If the result is a `Failure`, then:

```relang
failure.error is HttpError
```

---

### Static Refinement Rule

When the compiler can determine that an awaited expression originates from a specific action family, it may **refine** the type of `Failure.error` within the failure branch of a pattern match.

```relang
let r = await http.get("https://google.com")

match r {
  resp: HttpResponse => ...
  f: Failure => match f.error {     // refined as HttpError here
    t: Timeout         => ...
    d: DnsFailure      => ...
    s: HttpStatusError => ...
    _                  => ...
  }
}
```

This refinement is **contextual**: it is based on the origin of the awaited action, not on the nominal type `Failure`.

---

### Generic Failures

Some failures (e.g., cancellation) may be applicable to all action families.

To preserve sealed-family exhaustiveness, generic failures should be represented as variants within each family’s sealed error set (e.g., `Cancelled : HttpError`, `Cancelled : GrpcError`) rather than as a separate global error family.

---

### Rationale

- Keeps the core invariant: `await e : T | Failure`
- Avoids generic parameterization of `Failure`
- Preserves non-hierarchical, matchable error payloads
- Enables exhaustive (or near-exhaustive) handling per action family
- Supports tooling and documentation: “HTTP failures are `HttpError`”


## Cancellation

### Philosophy

Cancellation in Relang is **coordination control**, not business logic.

A cancellation request expresses *loss of interest* in a result and provides a best-effort mechanism to reduce wasted work. It is not a rollback mechanism and does not imply that remote side effects are undone.

Relang adopts three principles:

1. **Cancellation is a request, not a guarantee.**
2. **Cancellation is observed only at resolution time** (via `await` or terminal state).
3. **Coordination defines default cancellation propagation** (`and` / `or` cancel losers).

---

### API Surface

Relang provides a minimal cancellation surface:

```relang
cancel(t)        // record a cancellation request (non-blocking)
shield(t)        // block propagated cancellation into t (non-blocking wrapper)
```

Optional derived predicates (if exposed):

```relang
t.isOngoing(): Bool
t.isCompleted(): Bool
t.isFailed(): Bool
t.isCanceled(): Bool
```

---

### Non-Blocking Semantics (Normative)

`cancel(t)` is **non-blocking**. It records a cancellation request and returns immediately.

It does not wait for:

- the underlying action to stop
- an acknowledgement from a remote system
- any confirmation that cancellation “won”

This is required for determinism and correctness in distributed execution.

---

### Observability Rule (Normative)

The effect of cancellation is **observed only when the awaitable resolves**.

Therefore, the following is valid and expected:

```relang
cancel(t)
t.isCanceled()    // may be false
```

`isCanceled()` must be a **terminal outcome predicate**, not a “request was issued” predicate.

---

### Outcome Model

Awaiting an awaitable yields either a value or a failure:

```relang
await t : T | Failure
```

Cancellation is represented as a failure payload by action-family contract:

- For HTTP actions: `Cancelled : HttpError`
- For gRPC actions: `Cancelled : GrpcError`
- For DB actions: `Cancelled : DbError`

Example:

```relang
match await t {
  v: T => ...
  f: Failure => match f.error {
    c: Cancelled => ...
    _ => ...
  }
}
```

---

### Idempotency

`cancel(t)` is **idempotent**. Repeating it has no additional effect.

---

### Timing and Best-Effort Guarantees

A cancellation request may take effect in several ways:

- **Cancelled before start**: the task never begins execution and resolves as cancelled.
- **Cancelled in-flight**: the runtime requests interruption; the action may stop if it supports cancellation.
- **Too late**: the task has already resolved successfully or failed; cancellation is a no-op.

Relang does not claim “hard cancellation” unless the action family explicitly guarantees it.

---

### Cancellation Propagation and `shield`

#### Motivation

In structured coordination, cancellation often propagates from a composite awaitable to its unresolved operands (e.g., losers in `and`/`or`). In some cases, an operand must continue running despite parent cancellation.

Relang provides `shield(t)` as an explicit **cancellation propagation barrier**.

#### Task-Level Semantics (Normative)

`shield(t)` returns an awaitable with the **same success type** as `t`:

```relang
t : *T
shield(t) : *T
```

`shield(t)` does not create a new operation; it is a wrapper over the same underlying execution.

Cancellation behavior:

- `cancel(shield(t))` does **not** propagate into `t`
- `cancel(t)` may still cancel `t` (if the action supports cancellation)

This makes shielding precise: it blocks **incoming propagated cancellation**, not direct cancellation of the underlying task handle.

#### Coordination Interaction (Normative)

Coordination constructs issue cancellation requests to unresolved operands **as provided**.

Therefore, for:

```relang
let c = t1 and shield(t2)
```

If `t1` fails while `t2` is unresolved:

- `and` requests `cancel(shield(t2))`
- the request is absorbed by `shield`
- the underlying `t2` continues running

`shield` does **not** change `and` fail-fast semantics: `await (t1 and shield(t2))` still resolves as soon as failure is determined.

---

### Coordination Defaults

#### `and` (Fail-Fast, Cancel Remaining by Default)

For:

```relang
t = t1 and t2 and t3
```

Default behavior:

- If any operand fails, the composite fails immediately (fail-fast).
- The runtime issues cancellation requests for remaining unresolved operands.

Rationale: once one required operand fails, remaining work typically cannot change the composite outcome.

If an operand must not receive propagated cancellation, it must be explicitly shielded:

```relang
t = t1 and shield(t2)
```

#### `or` (First Success Wins, Cancel Remaining by Default)

For:

```relang
t = t1 or t2 or t3
```

Default behavior:

- The first success resolves the composite.
- The runtime issues cancellation requests for remaining unresolved operands.

Shielding prevents propagation into a specific operand:

```relang
t = t1 or shield(t2)
```

---

### Propagation Scope

Cancellation requests propagate downwards:

- Cancelling a composite awaitable cancels its unresolved operands (subject to `shield`).
- Cancelling a workflow scope cancels unresolved awaitables created within that scope (subject to `shield`).

Propagation is best-effort and deterministic.

---

### Timeouts

Timeouts should be defined as **policies that trigger cancellation**.

On timeout:

1. The runtime issues cancellation requests for affected awaitables.
2. The awaited result resolves with a timeout failure payload (e.g., `Timeout { duration: ... }`).

This makes timeouts deterministic and replayable.

---

### Rationale

- Avoids lying about distributed reality (“cancelled” cannot be confirmed synchronously).
- Keeps signatures stable (`await t : T | Failure`).
- Preserves a single failure channel while allowing action-family-specific cancellation payloads.
- Enables predictable resource control with `and` / `or` defaults.
- Provides an explicit, composable escape hatch (`shield`) when cancellation propagation is undesirable.


## Timeouts

### Purpose

Timeouts bound how long Relang is willing to wait for an awaitable to resolve.
They are **orchestration policies**, not guarantees that an underlying action stops.

Timeouts are deterministic and replayable.

---

### API

```relang
timeout(t, d) : *T
```

Where:
- `t : *T`
- `d : Duration`

`timeout` returns a new awaitable with the same success type as `t`.

---

### Semantics (Normative)

For `u = timeout(t, d)`:

1. If `t` resolves before duration `d`, `u` resolves with the same result.
2. If duration `d` elapses before `t` resolves:
   - the runtime issues `cancel(t)` (best-effort, deterministic request)
   - `u` resolves with `Failure`

The failure payload is a **timeout error** belonging to the action family of `t`.

Example (HTTP):

```relang
struct Timeout : HttpError { duration: Duration }
```

---

### Observation

```relang
match await timeout(http.get(url), 2s) {
  r: HttpResponse => ...
  f: Failure => match f.error {
    t: Timeout => ...
    c: Cancelled => ...
    _ => ...
  }
}
```

If the timeout wrapper is the cause of failure, the wrapper **must return `Timeout`**, not `Cancelled`, even though cancellation may be used internally.

---

### Composition

Timeouts compose transparently with coordination:

```relang
await timeout(t1 and t2, 1s)
```

If the timeout elapses:
- unresolved operands are cancelled (subject to `shield`)
- the composite resolves with `Failure(Timeout)`

---

## Retries

### Purpose

Retries allow re-executing **action awaitables** according to a deterministic policy.

Retries apply only to awaitables originating from **action invocations**, not to coordination constructs.

---

### API

```relang
retry(t, policy) : *T
```

Typing rule:

```relang
retry : (*T, RetryPolicy) -> *T
```

Where:
- `t : *T` originates from an action
- `RetryPolicy` is an explicit data structure

---

### Retry Policy

A retry policy is a pure data value, for example:

```relang
struct RetryPolicy {
  maxAttempts: Int
  initialDelay: Duration
  maxDelay: Duration?
  backoff: Backoff
  retryOn: RetryPredicate
}
```

`retryOn` is evaluated by pattern matching on **failure payloads**, not by exception class or hierarchy.

---

### Semantics (Normative)

For `let r: *T = retry(t: *T, policy)`:

1. The runtime executes attempts sequentially.
2. Each attempt is a fresh action invocation.
3. If an attempt succeeds, `r` resolves with the success value.
4. If an attempt fails:
   - the failure payload is evaluated against `policy.retryOn`
   - if retryable and attempts remain, a new attempt is scheduled
5. If attempts are exhausted, `r` resolves with `Failure`

The failure payload on exhaustion is **family-specific**, e.g.:

```relang
struct RetryExhausted : HttpError {
  attempts: Int
}
```

---

### Cancellation Interaction

- `cancel(r)`:
  - cancels the in-flight attempt (best-effort)
  - prevents further retries
  - causes `r` to resolve as cancelled

- `shield(r)`:
  - blocks propagated cancellation into the retry wrapper
  - does not block explicit `cancel(r)`

---

### Timeout and Retry Composition

Wrapper order is semantically significant.

**Per-attempt timeout:**

```relang
retry(timeout(http.get(url), 2s), policy)
```

Each attempt has a 2s timeout.

**Overall deadline:**

```relang
timeout(retry(http.get(url), policy), 10s)
```

The entire retry sequence must complete within 10s.

---

### Determinism Requirements

- Retry scheduling (delays, backoff) must be deterministic.
- Any jitter must be derived from deterministic seeds (e.g., workflow id, task id, attempt index).
- Retry behavior must be replay-safe.

---

### Restrictions (Normative)

The following are invalid:

```relang
retry(t1 and t2, policy)   // invalid
retry(t1 or t2, policy)    // invalid
```

Retries apply only to **action awaitables**, not to coordination constructs.

---

### Rationale

- Keeps retry behavior explicit and local
- Avoids hidden ambient policies
- Preserves clean interaction with `and`, `or`, `cancel`, and `shield`
- Maintains a single failure channel (`Failure`)



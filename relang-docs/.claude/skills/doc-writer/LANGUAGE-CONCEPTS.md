# ReLang Language Concepts

Documentation-ready explanations of core ReLang concepts. Reference `tmp/relang-awaitables-final.md` for authoritative specification.

## Awaitable Types (`*T`)

An awaitable represents an in-flight effect whose resolution may succeed or fail. Awaitables are not data types—they represent ongoing operations.

**Key points for documentation**:
- `*T` denotes an awaitable that yields type `T` on success
- Resolution is explicit via `.await()`
- Single awaitables have identity (`id`); composites do not
- Awaitables are eagerly started

**Interface** (for reference docs):
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

## Coordination Operators

### `and` — All Must Succeed (Fail-Fast)

Combines awaitables where all must succeed for the composite to succeed.

```relang
*A and *B         : *(A & B)
*A and *B and *C  : *(A & B & C)
```

**Behavior**:
- First failure resolves the composite as failed
- Remaining awaitables are cancelled
- Success yields a product type with all results

**Await result**: `(A & B) | Failure`

### `or` — First Success Wins (Fail-Last)

Combines awaitables where the first success resolves the composite.

```relang
*A or *B         : *(A | B)
*A or *B or *C   : *(A | B | C)
```

**Behavior**:
- First success resolves the composite
- Remaining awaitables are cancelled
- All must fail for composite to fail
- Failures are accumulated

**Await result**: `A | B | [Failure]`

## Data Types

### Product Types (`&`)

Combines multiple values into a tuple.

```relang
A & B           // Product of A and B
let (a & b) = v // Destructuring
```

**Properties**: Associative, flattened, order-significant.

### Sum Types (`|`)

Represents one of several alternatives.

```relang
A | B | C       // One of A, B, or C
```

## Failure Model

**Core invariant**: `await e : T | Failure`

### Failure Structure

```relang
struct Failure {
  error: ErrorData
  originId: String?
  occurredAt: Timestamp?
}
```

- `error` is data, not an exception
- Action families define sealed error sets (e.g., `HttpError`)
- Failures are handled via pattern matching

### Action-Family Errors

Each action family defines its error types:

```relang
sealed HttpError
struct Timeout : HttpError { duration: Duration }
struct DnsFailure : HttpError { host: String }
struct HttpStatusError : HttpError { status: Int, body: Bytes? }
```

### Failure Propagation (`!`)

The `!` operator unwraps success or propagates failure:

```relang
(await e)! : T   // Unwraps T or raises Failure
```

## Cancellation

### Philosophy

Cancellation is coordination control, not business logic:
- Request-based, not guaranteed
- Observed only at resolution time
- Best-effort; does not imply rollback

### API

```relang
cancel(t)   // Request cancellation (non-blocking)
shield(t)   // Block propagated cancellation
```

### Coordination Defaults

- `and`: Cancels remaining on first failure
- `or`: Cancels remaining on first success
- `shield(t)` prevents propagated cancellation into `t`

## Timeouts

Bound how long to wait for resolution.

```relang
timeout(t, d) : *T
```

**Semantics**:
- If `t` resolves before `d`, same result
- If `d` elapses first: issues `cancel(t)`, resolves with `Failure(Timeout)`

## Retries

Re-execute action awaitables according to policy.

```relang
retry(t, policy) : *T
```

**Restrictions**: Only applies to action awaitables, not coordination constructs.

**Composition**:
- Per-attempt timeout: `retry(timeout(action, 2s), policy)`
- Overall deadline: `timeout(retry(action, policy), 10s)`

## Execution Semantics

- Awaitables are eagerly started
- `and` is fail-fast (cancels on first failure)
- `or` is fail-last (cancels on first success)
- Failure lists preserve temporal order
- All scheduling is deterministic and replayable

## Common Patterns

### Coordinated Queries

```relang
let t1: *User = db.query("SELECT * FROM users WHERE id = 1")
let t2: *Orders = db.query("SELECT * FROM orders WHERE user_id = 1")

let joined: *(User & Orders) = t1 and t2
match joined.await() {
  (user & orders) => print("${user.name} has ${orders.count} orders")
  err: Failure => print("Failed: ${err}")
}
```

### Racing with Fallback

```relang
let primary: *Data = http.get("https://primary.api.com/data")
let fallback: *Data = http.get("https://fallback.api.com/data")

let raced: *Data = primary or fallback
match raced.await() {
  d: Data => print("Got data")
  errs: [Failure] => print("All failed: ${errs}")
}
```

### Nested Composition

```relang
let combined: *((User & Orders) | CachedData) = (t1 and t2) or t3
match combined.await() {
  (user & orders) => ...
  cached: CachedData => ...
  errs: [Failure] => ...
}
```

### Shielded Cleanup

```relang
let main = doWork()
let cleanup = shield(cleanupResources())
let result = main and cleanup  // cleanup continues even if main fails
```

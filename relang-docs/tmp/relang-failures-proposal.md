# ReLang Failure Model

*Error Structure for Durable Execution*

---

## 1. Overview

ReLang distinguishes between two levels of failure:

| Level | What failed | Example |
|-------|-------------|---------|
| **Action** | An external call (HTTP, DB, gRPC) | Network timeout, DNS failure |
| **Function** | A spawned function | Child function failed, timed out, cancelled |

Both levels share a common structure but have different semantics.

---

## 2. Core Types

### 2.1 Await Result

Every `await` returns either a success value or a failure:

```relang
await expr : T | Failure
```

### 2.2 Failure Type

```relang
type Failure {
    kind: FailureKind
    sourceId: String
    failedAt: Timestamp
}
```

| Field | Description |
|-------|-------------|
| `kind` | The specific failure type (see below) |
| `sourceId` | ID of the awaitable that failed |
| `failedAt` | When the failure occurred |

---

## 3. Failure Kinds

```relang
sealed FailureKind

// Action-level failures
struct ActionFailed : FailureKind { ... }
struct ActionTimedOut : FailureKind { ... }

// Function-level failures
struct FunctionFailed : FailureKind { ... }
struct FunctionTimedOut : FailureKind { ... }
struct FunctionCancelled : FailureKind { ... }

// Coordination failures
struct AllFailed : FailureKind { ... }
```

---

## 4. Action Failures

An **action** is an external call: HTTP request, database query, gRPC call, etc.

### 4.1 ActionFailed

Returned when an action fails after exhausting all retry attempts.

```relang
struct ActionFailed : FailureKind {
    actionType: String              // "http", "db", "grpc", etc.
    error: ActionError              // Family-specific error
    attempts: [Attempt]             // History of all attempts
}
```

### 4.2 ActionTimedOut

Returned when an action exceeds its timeout.

```relang
struct ActionTimedOut : FailureKind {
    actionType: String
    timeout: Duration
    attempts: [Attempt]             // Attempts made before timeout
}
```

### 4.3 Attempt

Each retry attempt is recorded:

```relang
type Attempt {
    index: Int                      // 0-based attempt number
    startedAt: Timestamp
    endedAt: Timestamp
    workerId: String                // Server/worker that executed this attempt
    error: ActionError?             // Error if this attempt failed
}
```

This enables:
- Debugging which servers had issues
- Understanding retry behavior
- Analyzing failure patterns

---

## 5. Action Error Families

Each action type defines a **sealed error family**.

### 5.1 HTTP Errors

```relang
sealed HttpError : ActionError

struct HttpTimeout : HttpError {
    duration: Duration
}

struct DnsFailure : HttpError {
    host: String
}

struct ConnectionRefused : HttpError {
    host: String
    port: Int
}

struct HttpStatus : HttpError {
    status: Int
    body: Bytes?
}

struct TlsError : HttpError {
    message: String
}
```

### 5.2 Database Errors

```relang
sealed DbError : ActionError

struct DbTimeout : DbError {
    duration: Duration
}

struct ConnectionFailed : DbError {
    host: String
}

struct QueryError : DbError {
    code: String
    message: String
}

struct ConstraintViolation : DbError {
    constraint: String
    message: String
}
```

### 5.3 gRPC Errors

```relang
sealed GrpcError : ActionError

struct GrpcTimeout : GrpcError {
    duration: Duration
}

struct GrpcStatus : GrpcError {
    code: Int                       // gRPC status code
    message: String
}

struct GrpcUnavailable : GrpcError {
    endpoint: String
}
```

---

## 6. Function Failures

A **function** is a spawned ReLang function that runs as an independent execution.

### 6.1 FunctionFailed

Returned when a spawned function fails due to an internal failure.

```relang
struct FunctionFailed : FailureKind {
    functionName: String
    cause: Failure                  // The underlying failure
}
```

The `cause` field preserves the **error chain** — you can trace back to the original action that failed.

### 6.2 FunctionTimedOut

Returned when a spawned function exceeds its timeout.

```relang
struct FunctionTimedOut : FailureKind {
    functionName: String
    timeout: Duration
}
```

### 6.3 FunctionCancelled

Returned when a spawned function was cancelled.

```relang
struct FunctionCancelled : FailureKind {
    functionName: String
    cancelledAt: Timestamp
}
```

---

## 7. Coordination Failures

### 7.1 AllFailed (`or` aggregation)

When all branches of an `or` coordination fail:

```relang
struct AllFailed : FailureKind {
    failures: [Failure]             // All individual failures
}
```

For `and` coordination, the first failure is returned directly (fail-fast).

---

## 8. Examples

### 8.1 Handling Action Failure

```relang
let result = await http.get("https://api.example.com/data")

match result {
    data: Response -> process(data)

    f: Failure -> match f.kind {
        af: ActionFailed -> {
            log("Action failed after ${af.attempts.size} attempts")
            match af.error {
                t: HttpTimeout -> log("Timed out after ${t.duration}")
                s: HttpStatus -> log("HTTP ${s.status}")
                _ -> log("Other error")
            }
        }
        at: ActionTimedOut -> log("Overall timeout after ${at.timeout}")
    }
}
```

### 8.2 Handling Function Failure

```relang
let result = await spawn processOrder(orderId)

match result {
    order: OrderResult -> ship(order)

    f: Failure -> match f.kind {
        ff: FunctionFailed -> {
            log("Function ${ff.functionName} failed")
            // Trace the error chain
            match ff.cause.kind {
                af: ActionFailed -> log("Root cause: ${af.actionType} failed")
                _ -> log("Other cause")
            }
        }
        ft: FunctionTimedOut -> log("Function timed out after ${ft.timeout}")
        fc: FunctionCancelled -> log("Function was cancelled")
    }
}
```

### 8.3 Inspecting Attempts

```relang
match result {
    f: Failure -> match f.kind {
        af: ActionFailed -> {
            for attempt in af.attempts {
                log("Attempt ${attempt.index} on ${attempt.workerId}")
                log("  Duration: ${attempt.endedAt - attempt.startedAt}")
                if attempt.error != none {
                    log("  Error: ${attempt.error}")
                }
            }
        }
    }
}
```

### 8.4 Handling `or` Aggregation

```relang
let result = await (primary or fallback)

match result {
    data: Data -> process(data)

    f: Failure -> match f.kind {
        all: AllFailed -> {
            log("All ${all.failures.size} branches failed:")
            for failure in all.failures {
                log("  - ${failure.sourceId}: ${failure.kind}")
            }
        }
    }
}
```

---

## 9. Propagation with `!`

The `!` operator unwraps success or propagates failure:

```relang
let data = await http.get(url)!   // propagates Failure if failed
```

When propagated:
- The `Failure` travels up the call stack
- If inside a spawned function, it becomes `FunctionFailed.cause`
- The error chain is preserved

---

## 10. Type Signatures

### 10.1 Actions

```relang
// Action calls return their success type or Failure
http.get(url)     : *HttpResponse
await http.get(url) : HttpResponse | Failure
```

### 10.2 Spawned Functions

```relang
// Spawn returns an awaitable
spawn processOrder(id) : *OrderResult
await spawn processOrder(id) : OrderResult | Failure
```

### 10.3 Coordination

```relang
// and: all must succeed (fail-fast)
await (t1 and t2) : (A & B) | Failure

// or: any success wins (fail-last, aggregates failures)
await (t1 or t2) : A | B | Failure
```

---

## 11. Design Rationale

### 11.1 Why separate Action vs Function failures?

- **Different semantics**: Actions are external calls; functions are internal orchestration
- **Different retry behavior**: Actions have automatic retries; functions may need different strategies
- **Different debugging needs**: Action failures need attempt history; function failures need error chains

### 11.2 Why track attempts?

- **Debugging**: See which workers/servers had issues
- **Observability**: Monitor retry patterns
- **Root cause analysis**: Understand if failures are localized to specific infrastructure

### 11.3 Why error chains?

When a spawned function fails because an HTTP call failed:
- You want to know the function failed
- You also want to know *why* — the original HTTP error
- Error chains preserve this context through `FunctionFailed.cause`

### 11.4 Why sealed error families?

- **Exhaustive matching**: Compiler can check you handle all cases
- **Documentation**: Clear contract of what can fail
- **Evolution**: New error types can be added to the family

---

## 12. Summary

| Failure Kind | When | Key Fields |
|--------------|------|------------|
| `ActionFailed` | External call failed after retries | `actionType`, `error`, `attempts` |
| `ActionTimedOut` | External call exceeded timeout | `actionType`, `timeout`, `attempts` |
| `FunctionFailed` | Spawned function failed | `functionName`, `cause` |
| `FunctionTimedOut` | Spawned function exceeded timeout | `functionName`, `timeout` |
| `FunctionCancelled` | Spawned function was cancelled | `functionName` |
| `AllFailed` | All `or` branches failed | `failures` (list) |

---

*End of proposal*

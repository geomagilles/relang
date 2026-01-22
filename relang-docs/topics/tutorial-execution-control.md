# Controlling Execution

This tutorial teaches you how to control awaitable execution with timeouts, retries, and cancellation.
You'll learn to build resilient programs that handle real-world conditions.

## What You'll Learn

- Setting timeouts on operations
- Configuring retry policies
- Cancelling operations manually
- Using `shield` to protect from cancellation
- Composing policies for complex scenarios

## Prerequisites

Complete [Handling Failures](tutorial-failures.md) first.

## 1. Timeouts

Timeouts bound how long you're willing to wait:

```relang
let response = timeout(http.get(url), 5s)
```

The `timeout` function:
- Returns a new awaitable with the same success type
- If the operation completes in time, returns its result
- If time expires, cancels the operation and returns `Timeout` failure

### Handling Timeout Results

```relang
match timeout(http.get(url), 5s).await() {
    response: HttpResponse => {
        print("Got response: ${response.status}")
    }
    f: Failure => match f.error {
        t: Timeout => {
            print("Request timed out after ${t.duration}")
        }
        _ => print("Other failure: ${f.error}")
    }
}
```

### Timeouts with Coordination

Timeouts compose with coordination:

```relang
// Both queries must complete within 10 seconds total
let result = timeout(query1 and query2, 10s)

// Each race participant has 5 seconds
let winner = timeout(primary or fallback, 5s)
```

## 2. Retries

Retries re-execute failed operations according to a policy:

```relang
let policy = RetryPolicy {
    maxAttempts: 3,
    initialDelay: 100ms,
    maxDelay: 2s,
    backoff: Exponential,
    retryOn: isRetryable
}

let response = retry(http.get(url), policy)
```

### Retry Policy Options

| Field | Description |
|-------|-------------|
| `maxAttempts` | Maximum number of tries |
| `initialDelay` | Delay before first retry |
| `maxDelay` | Cap on backoff delay |
| `backoff` | `Constant`, `Linear`, or `Exponential` |
| `retryOn` | Predicate determining which failures to retry |

### Selective Retries

Only retry certain errors:

```relang
fn isRetryable(f: Failure): Bool {
    match f.error {
        t: Timeout => true           // Retry timeouts
        s: HttpStatusError => {
            s.status >= 500          // Retry server errors
        }
        _ => false                   // Don't retry others
    }
}

let policy = RetryPolicy {
    maxAttempts: 3,
    retryOn: isRetryable
}
```

### Retry Exhaustion

When retries are exhausted:

```relang
match retry(http.get(url), policy).await() {
    response: HttpResponse => print("Success!")
    f: Failure => match f.error {
        r: RetryExhausted => {
            print("Failed after ${r.attempts} attempts")
        }
        _ => print("Non-retryable failure: ${f.error}")
    }
}
```

## 3. Combining Timeouts and Retries

Order matters when combining policies:

### Per-Attempt Timeout

```relang
// Each attempt has 2 seconds
let operation = retry(
    timeout(http.get(url), 2s),
    policy
)
```

If an attempt times out, it counts as a failure and triggers retry.

### Overall Deadline

```relang
// All retries must complete within 10 seconds
let operation = timeout(
    retry(http.get(url), policy),
    10s
)
```

If the deadline expires, the entire retry sequence is cancelled.

### Both

```relang
// 2s per attempt, 10s overall
let operation = timeout(
    retry(
        timeout(http.get(url), 2s),
        policy
    ),
    10s
)
```

## 4. Cancellation

Cancel operations you no longer need:

```relang
let request = http.get(url)

// Later, if we don't need it anymore
cancel(request)
```

### Cancellation Semantics

- `cancel(t)` is **non-blocking** — it records a request and returns immediately
- Cancellation is **best-effort** — the operation may have already completed
- The effect is observed at **resolution time**

```relang
let t = http.get(url)
cancel(t)

// The operation might still succeed if it completed before cancellation
match t.await() {
    response: HttpResponse => print("Completed before cancel")
    f: Failure => match f.error {
        c: Cancelled => print("Was cancelled")
        _ => print("Other failure")
    }
}
```

### Automatic Cancellation

Coordination operators cancel remaining operations automatically:

```relang
// `and`: remaining cancelled on first failure
let all = t1 and t2 and t3
// If t1 fails, t2 and t3 receive cancellation requests

// `or`: remaining cancelled on first success
let any = t1 or t2 or t3
// If t2 succeeds first, t1 and t3 receive cancellation requests
```

## 5. Shielding from Cancellation

Sometimes an operation must complete regardless of parent cancellation:

```relang
let payment = processPayment(order)
let notification = sendNotification(user)

// Payment is shielded; notification can be cancelled
let checkout = shield(payment) and notification
```

### How Shield Works

- `shield(t)` wraps the awaitable
- Propagated cancellation is absorbed by the shield
- Direct `cancel(t)` still works

```relang
let shielded = shield(importantOperation())

cancel(shielded)   // Absorbed — operation continues
cancel(importantOperation())  // Direct cancel works (if you have the handle)
```

### Shield in Coordination

```relang
let critical = shield(saveToDatabase())
let optional = sendEmail()

let result = critical and optional
```

If `optional` fails:
- `and` tries to cancel `critical`
- `shield` absorbs the cancellation
- Database save continues to completion

## 6. Complete Example

Robust API call with all policies:

```relang
fn fetchDataReliably(url: String): Data | Failure {
    // Retry policy: 3 attempts, exponential backoff
    let retryPolicy = RetryPolicy {
        maxAttempts: 3,
        initialDelay: 100ms,
        maxDelay: 2s,
        backoff: Exponential,
        retryOn: fn(f) => match f.error {
            t: Timeout => true
            s: HttpStatusError => s.status >= 500
            _ => false
        }
    }

    // Build the resilient operation:
    // - 2s timeout per attempt
    // - Up to 3 retries with backoff
    // - 10s overall deadline
    let operation = timeout(
        retry(
            timeout(http.get(url), 2s),
            retryPolicy
        ),
        10s
    )

    return operation.await()
}

fn main() {
    match fetchDataReliably("https://api.example.com/data") {
        data: Data => {
            print("Got data: ${data}")
        }
        f: Failure => match f.error {
            t: Timeout => print("Overall deadline exceeded")
            r: RetryExhausted => print("All ${r.attempts} attempts failed")
            _ => print("Unexpected failure: ${f.error}")
        }
    }
}
```

## 7. Policy Composition Summary

| Pattern | Code | Behavior |
|---------|------|----------|
| Timeout only | `timeout(t, d)` | Fail if not done in `d` |
| Retry only | `retry(t, policy)` | Retry failures per policy |
| Per-attempt timeout | `retry(timeout(t, d), policy)` | Each attempt limited to `d` |
| Overall deadline | `timeout(retry(t, policy), d)` | All retries within `d` |
| Protected operation | `shield(t)` | Ignore propagated cancellation |

## Key Takeaways

1. **Timeouts bound wait time** — not execution time
2. **Retries are explicit** — configure what to retry and how
3. **Order of composition matters** — inner policies apply first
4. **Cancellation is cooperative** — request, not guarantee
5. **Shield protects critical operations** — from propagated cancellation only

## Next Steps

You've completed the core tutorials! Continue exploring:

- [Failure reference](reference-language-syntax.md) for complete error type documentation
- [Resumability explanation](explanation-resumability.md) for how durable execution works

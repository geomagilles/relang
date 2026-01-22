# Handling Failures

This tutorial teaches you how to handle failures in ReLang.
You'll learn about the failure model, error payloads, and propagation patterns.

## What You'll Learn

- Understanding `Failure` and error payloads
- Pattern matching on specific error types
- Propagating failures with `!`
- Handling aggregated failures from `or`

## Prerequisites

Complete [Coordinating Tasks](tutorial-coordination.md) first.

## 1. The Failure Model

Every `await()` returns either success or failure:

```relang
await(t) : T | Failure
```

`Failure` is **not** an exception. It's a value that carries structured error data:

```relang
struct Failure {
    error: ErrorData      // The specific error
    originId: String?     // Which operation failed
    occurredAt: Timestamp?
}
```

## 2. Action-Specific Error Types

Each action family defines its own error types. For HTTP:

```relang
sealed HttpError

struct Timeout : HttpError { duration: Duration }
struct DnsFailure : HttpError { host: String }
struct HttpStatusError : HttpError { status: Int, body: Bytes? }
struct Cancelled : HttpError {}
```

When an HTTP request fails, `failure.error` is guaranteed to be one of these types.

## 3. Basic Failure Handling

Handle failures with pattern matching:

```relang
let response = http.get("https://api.example.com/data")

match response.await() {
    data: Data => {
        print("Success: ${data}")
    }
    f: Failure => {
        print("Failed: ${f.error}")
    }
}
```

## 4. Matching Specific Errors

For precise error handling, match on the error payload:

```relang
let response = http.get("https://api.example.com/data")

match response.await() {
    data: Data => process(data)

    f: Failure => match f.error {
        t: Timeout => {
            print("Request timed out after ${t.duration}")
        }
        d: DnsFailure => {
            print("Could not resolve host: ${d.host}")
        }
        s: HttpStatusError => {
            print("HTTP ${s.status}")
            if s.status == 404 {
                print("Resource not found")
            }
        }
        c: Cancelled => {
            print("Request was cancelled")
        }
        _ => {
            print("Unknown error")
        }
    }
}
```

## 5. Failure Propagation with `!`

The `!` operator unwraps success or propagates failure:

```relang
let data = response.await()!   // Propagates Failure if failed
```

This is equivalent to:

```relang
let data = match response.await() {
    v: T => v
    f: Failure => raise f
}
```

Use `!` for concise code when the caller should handle failures:

```relang
fn fetchUserData(id: Int): User | Failure {
    let user = db.query("SELECT * FROM users WHERE id = ${id}").await()!
    let profile = db.query("SELECT * FROM profiles WHERE user_id = ${id}").await()!

    return User { ...user, profile: profile }
}
```

If any query fails, the failure propagates to the caller.

## 6. Failures from `and` Coordination

With `and`, the **first** failure determines the result:

```relang
let t1: *A = willSucceed()
let t2: *B = willFail()    // This fails first
let t3: *C = willFail()

match (t1 and t2 and t3).await() {
    (a & b & c) => print("All succeeded")
    f: Failure => {
        // f.error is from t2 (first to fail)
        print("Failed: ${f.error}")
    }
}
```

## 7. Failures from `or` Coordination

With `or`, failures are **aggregated** only if all fail:

```relang
let t1: *Data = source1()   // Fails
let t2: *Data = source2()   // Fails
let t3: *Data = source3()   // Fails

match (t1 or t2 or t3).await() {
    data: Data => print("Got data")
    f: Failure => {
        // f.error is AggregateFailure containing all failures
        match f.error {
            agg: AggregateFailure => {
                print("All ${agg.failures.length} sources failed:")
                for failure in agg.failures {
                    print("  - ${failure.error}")
                }
            }
        }
    }
}
```

## 8. Error Recovery Patterns

### Try-Else Pattern

```relang
fn fetchWithFallback(): Data | Failure {
    match primary().await() {
        data: Data => return data
        f: Failure => {
            print("Primary failed, trying fallback")
            return fallback().await()
        }
    }
}
```

### Default Value Pattern

```relang
fn fetchOrDefault(): Data {
    match source().await() {
        data: Data => return data
        f: Failure => return Data.default()
    }
}
```

### Logging and Re-propagating

```relang
fn fetchWithLogging(): Data | Failure {
    match source().await() {
        data: Data => return data
        f: Failure => {
            log.error("Fetch failed: ${f.error}")
            raise f  // Re-propagate
        }
    }
}
```

## 9. Complete Example

Robust user fetch with detailed error handling:

```relang
fn fetchUser(id: Int): User | Failure {
    let userRequest = http.get("https://api.example.com/users/${id}")

    match userRequest.await() {
        response: HttpResponse => {
            if response.status == 200 {
                return parseUser(response.body)
            } else if response.status == 404 {
                return Failure { error: UserNotFound { id: id } }
            } else {
                return Failure { error: HttpStatusError { status: response.status } }
            }
        }

        f: Failure => match f.error {
            t: Timeout => {
                log.warn("User fetch timed out, trying cache")
                return fetchUserFromCache(id)
            }
            d: DnsFailure => {
                log.error("DNS resolution failed for API")
                raise f
            }
            _ => raise f
        }
    }
}

fn main() {
    match fetchUser(123) {
        user: User => print("Hello, ${user.name}!")
        f: Failure => match f.error {
            n: UserNotFound => print("User ${n.id} does not exist")
            _ => print("Could not fetch user: ${f.error}")
        }
    }
}
```

## Key Takeaways

1. **`Failure` is data, not an exception** — structured, matchable error payloads
2. **Errors are action-specific** — HTTP errors differ from DB errors
3. **Use `!` for propagation** — concise unwrap-or-propagate
4. **`and` fails fast** — first failure wins
5. **`or` aggregates failures** — only if all fail

## Next Steps

Learn to [control execution](tutorial-execution-control.md) with timeouts, retries, and cancellation.

# Working with Awaitables

This tutorial introduces awaitables, ReLang's core abstraction for durable execution.
You'll learn how to create awaitables, resolve them, and understand their lifecycle.

## What You'll Learn

- What awaitables represent
- How to create and resolve awaitables
- Inspecting awaitable state
- The difference between awaitables and regular values

## Prerequisites

Complete [Getting Started](tutorial-getting-started.md) first.

## 1. Understanding Awaitables

An **awaitable** represents an in-flight effect—an operation that may succeed or fail.
The `*` prefix denotes an awaitable type:

```relang
*T    // An awaitable that, when resolved, yields a value of type T
```

Awaitables are **not** values. They are handles to operations whose results aren't yet available.

## 2. Creating Awaitables

Awaitables are created by calling **actions**—operations that interact with external systems:

```relang
// HTTP request returns an awaitable
let response: *HttpResponse = http.get("https://api.example.com/users/1")

// Database query returns an awaitable
let user: *User = db.query("SELECT * FROM users WHERE id = 1")
```

At this point, `response` and `user` are in-flight. The operations have started, but we don't have results yet.

## 3. Resolving Awaitables with await

To get the result, call `.await()`:

```relang
let response: *HttpResponse = http.get("https://api.example.com/users/1")
let result = response.await()
```

The `await()` method:
- Blocks until the operation completes
- Returns either the success value or a `Failure`

The result type is always `T | Failure`:

```relang
response.await() : HttpResponse | Failure
```

## 4. Handling Results with Pattern Matching

Since `await()` can return either success or failure, use pattern matching:

```relang
let response: *HttpResponse = http.get("https://api.example.com/users/1")

match response.await() {
    r: HttpResponse => print("Got response: ${r.status}")
    f: Failure => print("Request failed: ${f.error}")
}
```

This pattern is fundamental to ReLang: **every await explicitly handles both outcomes**.

## 5. Inspecting Awaitable State

Awaitables expose their lifecycle state:

```relang
let t: *Data = fetchData()

t.isResolved()   // true if complete (success or failure)
t.isSucceeded()  // true if resolved successfully
t.isFailed()     // true if resolved with failure
```

You can also access metadata:

```relang
t.id           // Unique identifier for this operation
t.createdAt    // When the awaitable was created
t.resolvedAt() // When it resolved (if resolved)
```

## 6. Awaitables vs Regular Values

A key insight: awaitables and their resolved values are different things.

```relang
let t: *User = db.query("...")   // t is an awaitable
let user: User = t.await()!      // user is the actual value
```

| Aspect | Awaitable (`*T`) | Value (`T`) |
|--------|------------------|-------------|
| Represents | In-flight operation | Concrete data |
| Can fail | Yes (until resolved) | No (already resolved) |
| Has identity | Yes (`id`) | No |
| Checkpointed | Operation handle | Actual data |

## 7. Complete Example

Here's a complete program that fetches user data:

```relang
fn main() {
    // Start the operation
    let userRequest: *User = http.get("https://api.example.com/users/1")

    print("Request started with id: ${userRequest.id}")
    print("Waiting for response...")

    // Resolve and handle the result
    match userRequest.await() {
        user: User => {
            print("Found user: ${user.name}")
            print("Email: ${user.email}")
        }
        failure: Failure => {
            print("Failed to fetch user")
            print("Error: ${failure.error}")
        }
    }
}
```

## Key Takeaways

1. **Awaitables represent in-flight operations** — they're handles, not values
2. **Resolution is explicit** — call `.await()` to get the result
3. **Always handle both outcomes** — success (`T`) or failure (`Failure`)
4. **Awaitables have identity** — each operation has a unique `id`

## Next Steps

Now that you understand single awaitables, learn how to [coordinate multiple operations](tutorial-coordination.md) running in parallel.

# Coordinating Tasks

This tutorial teaches you how to coordinate multiple awaitables running in parallel.
You'll learn to use `and` and `or` operators to express common concurrency patterns.

## What You'll Learn

- Running multiple operations in parallel
- Using `and` when all must succeed
- Using `or` when any success is enough
- Understanding result types from coordination
- Nesting coordination for complex patterns

## Prerequisites

Complete [Working with Awaitables](tutorial-awaitables.md) first.

## 1. The Problem: Multiple Operations

Often you need results from several operations:

```relang
let user: *User = db.query("SELECT * FROM users WHERE id = 1")
let orders: *Orders = db.query("SELECT * FROM orders WHERE user_id = 1")
```

Both queries start immediately (awaitables are eager). But how do you wait for both?

## 2. Coordination with `and`

Use `and` when **all operations must succeed**:

```relang
let user: *User = db.query("SELECT * FROM users WHERE id = 1")
let orders: *Orders = db.query("SELECT * FROM orders WHERE user_id = 1")

let combined: *(User & Orders) = user and orders
```

The result type `*(User & Orders)` is an awaitable of a **product type**—both values together.

### Resolving an `and` Expression

```relang
match combined.await() {
    (user & orders) => {
        print("${user.name} has ${orders.count} orders")
    }
    f: Failure => {
        print("Failed: ${f.error}")
    }
}
```

### Fail-Fast Behavior

`and` is **fail-fast**: if any operation fails, the composite fails immediately.

```relang
let t1: *A = willSucceed()
let t2: *B = willFail()
let t3: *C = willSucceed()

let all = t1 and t2 and t3   // Fails as soon as t2 fails
```

When `t2` fails:
- The composite resolves with `Failure` (from `t2`)
- `t1` and `t3` receive cancellation requests

## 3. Coordination with `or`

Use `or` when **any success is enough**:

```relang
let primary: *Data = http.get("https://primary.api.com/data")
let fallback: *Data = http.get("https://fallback.api.com/data")

let raced: *(Data | Data) = primary or fallback
```

The result type `*(Data | Data)` simplifies to `*Data` when types match.

### Resolving an `or` Expression

```relang
match raced.await() {
    data: Data => {
        print("Got data from one of the sources")
    }
    failures: [Failure] => {
        print("All sources failed")
        for f in failures {
            print("  - ${f.error}")
        }
    }
}
```

### Fail-Last Behavior

`or` is **fail-last**: it only fails if **all** operations fail.

```relang
let t1: *A = willFail()
let t2: *B = willSucceed()
let t3: *C = willFail()

let any = t1 or t2 or t3   // Succeeds when t2 succeeds
```

When `t2` succeeds:
- The composite resolves with `t2`'s value
- `t1` and `t3` receive cancellation requests

## 4. Product and Sum Types

Coordination operators produce data types:

| Coordination | Result Type | Data Shape |
|--------------|-------------|------------|
| `*A and *B` | `*(A & B)` | Product (both values) |
| `*A or *B` | `*(A \| B)` | Sum (one value) |

### Product Types (`&`)

Products combine multiple values:

```relang
let result: (User & Orders) = ...

// Destructure to access components
let (user & orders) = result
print(user.name)
print(orders.count)
```

### Sum Types (`|`)

Sums represent alternatives:

```relang
let result: (CachedData | FreshData) = ...

// Pattern match to determine which
match result {
    cached: CachedData => print("From cache")
    fresh: FreshData => print("Fresh fetch")
}
```

## 5. Chaining Multiple Operations

Coordination operators chain naturally:

```relang
// All three must succeed
let all: *(A & B & C) = t1 and t2 and t3

// Any of three
let any: *(A | B | C) = t1 or t2 or t3
```

The types flatten automatically:
- `(A & B) & C` becomes `A & B & C`
- `(A | B) | C` becomes `A | B | C`

## 6. Nested Coordination

Combine `and` and `or` for complex patterns:

```relang
// Try (user AND orders) together, OR fall back to cache
let result: *((User & Orders) | CachedData) = (user and orders) or cache

match result.await() {
    (user & orders) => print("Got fresh data")
    cached: CachedData => print("Using cached data")
    failures: [Failure] => print("Everything failed")
}
```

## 7. Coordinating Lists

For homogeneous lists of awaitables:

```relang
let requests: [*Response] = urls.map(url => http.get(url))

// Wait for all
let all: *[Response] = and(requests)

// Wait for first success
let first: *Response = or(requests)
```

## 8. Complete Example

Fetch user profile with parallel queries and fallback:

```relang
fn fetchUserProfile(userId: Int) {
    // Primary: fetch user and recent orders in parallel
    let user: *User = db.query("SELECT * FROM users WHERE id = ${userId}")
    let orders: *[Order] = db.query("SELECT * FROM orders WHERE user_id = ${userId} LIMIT 5")

    // Fallback: cached profile
    let cached: *CachedProfile = cache.get("profile:${userId}")

    // Try fresh data first, fall back to cache
    let profile = (user and orders) or cached

    match profile.await() {
        (user & orders) => {
            print("User: ${user.name}")
            print("Recent orders: ${orders.length}")
        }
        cached: CachedProfile => {
            print("Using cached profile for ${cached.name}")
        }
        failures: [Failure] => {
            print("Could not load profile")
        }
    }
}
```

## Key Takeaways

1. **`and` requires all to succeed** — fail-fast, returns product type
2. **`or` requires any to succeed** — fail-last, returns sum type
3. **Coordination is composable** — nest `and`/`or` for complex patterns
4. **Types track data shape** — `&` for products, `|` for sums
5. **Remaining operations get cancelled** — after composite resolves

## Next Steps

Learn how to [handle failures](tutorial-failures.md) effectively when operations don't succeed.

# Coordinating Tasks

This tutorial shows v0.1 coordination with `and` and `or`.

## What You Will Learn

- `and` for all-success coordination
- `or` for first-success coordination
- Result typing with products (`&`) and unions (`|`)

## 1. `and`: All Must Succeed

```relang
fn fetchProfile(id: String): *(User & [Order]) {
  getUser(id) and getOrders(id)
}
```

Type rule:

- `*A and *B : *(A & B)`

Runtime behavior:

- Fail-fast on first failure
- Best-effort cancellation of remaining branches

## 2. `or`: Any Success Is Enough

```relang
fn fetchWithFallback(id: String): *User {
  primaryUserSource(id) or cachedUserSource(id)
}
```

Type rule:

- `*A or *B : *(A | B)`

Runtime behavior:

- First success wins
- If all branches fail, result is `Failure(kind = AllFailed(...))`

## 3. Handle Coordinated Results

```relang
fn run(id: String): User | Failure {
  let result = await (primaryUserSource(id) or cachedUserSource(id))
  match result {
    u: User -> u
    f: Failure -> f
  }
}
```

## 4. Timeout Pattern in v0.1 Core

No dedicated `timeout(...)` keyword exists in core v0.1.

Use coordination with timers:

```relang
fn withTimeout(id: String): User | Failure {
  let result = await (primaryUserSource(id) or timer(5s))
  match result {
    u: User -> u
    f: Failure -> f
  }
}
```

## Next Step

Continue with [Handling Failures](tutorial-failures.md).

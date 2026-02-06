# Controlling Execution (v0.1)

This tutorial covers execution control patterns that are valid in core v0.1.

## What You Will Learn

- How to express deadlines with `timer(...)`
- How to coordinate work with `and` / `or`
- What is runtime policy vs language syntax

## 1. Deadline Pattern

Core v0.1 has no dedicated `timeout(...)` syntax.

Use `or` with a timer:

```relang
fn fetchWithDeadline(id: String): User | Failure {
  let result = await (http.get("https://api.example.com/users/" + id) or timer(5s))
  match result {
    u: User -> u
    f: Failure -> f
  }
}
```

## 2. Parallel Coordination Pattern

```relang
fn aggregate(id: String): (User & [Order]) | Failure {
  let result = await (getUser(id) and getOrders(id))
  match result {
    pair: (User & [Order]) -> pair
    f: Failure -> f
  }
}
```

## 3. Signal Wait Pattern

```relang
fn waitApproval(): Approval | Failure {
  let result = await receive<Approval>()
  match result {
    a: Approval -> a
    f: Failure -> f
  }
}
```

## 4. Retry Is Runtime Policy in v0.1

In v0.1:

- no `retry` language keyword
- retry behavior is configured by runtime/action family policy
- attempts are reported through failure metadata (`ActionFailed` / `ActionTimedOut`)

## 5. Determinism and Time

- `now()` reads current UTC wall-clock time
- values captured before snapshot are restored on resume
- new `now()` calls after resume read current time

## Next Step

Use [How to Suspend and Resume](howto-suspend-resume.md) for operational guidance.

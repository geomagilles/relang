# Working with Awaitables

This tutorial covers the v0.1 awaitable model.

## What You Will Learn

- How awaitables are created
- How to resolve them with `await e`
- How `await e!` propagates failures

## 1. Create Awaitables

Effects return awaitables (`*T`):

```relang
fn getUser(id: String): *User {
  http.get("https://api.example.com/users/" + id)
}

fn getOrders(id: String): *[Order] {
  http.get("https://api.example.com/users/" + id + "/orders")
}
```

## 2. Resolve Awaitables Explicitly

```relang
fn load(id: String): (User & [Order]) | Failure {
  let userResult = await getUser(id)
  match userResult {
    u: User -> {
      let ordersResult = await getOrders(id)
      match ordersResult {
        o: [Order] -> (u & o)
        f: Failure -> f
      }
    }
    f: Failure -> f
  }
}
```

## 3. Propagate with `!`

`await e!` is sugar for `(await e)!`.

```relang
fn loadUser(id: String): User {
  await getUser(id)!
}
```

Semantics:

- success: unwraps and returns `T`
- failure: propagates `Failure`

## 4. Identity and Lifecycle

Per spec:

- Primitive awaitables have stable identity (`id`, `createdAt`)
- `and` / `or` compositions do not create a new identity object
- Runtime snapshots include awaitable table and resolution state

## Next Step

Continue with [Coordinating Tasks](tutorial-coordination.md).

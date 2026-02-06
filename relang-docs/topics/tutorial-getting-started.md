# Getting Started with ReLang v0.1

This tutorial gets you started with the v0.1 language model defined in `specs/v0.1`.

## What You Will Learn

- Core syntax (`fn`, `type`, `let`, `match`)
- Explicit await (`await e`)
- Basic failure handling (`T | Failure`)

## 1. Write a Minimal Program

Create `hello.re`:

```relang
type Greeting { message: String }

fn buildGreeting(name: String): Greeting {
  Greeting { message: "Hello, " + name }
}

fn main(): Greeting {
  buildGreeting("ReLang")
}
```

Key points:

- Functions are top-level declarations
- Last expression can be the return value
- User types are immutable records (`type`)

## 2. Add an Awaitable Boundary

```relang
type User { id: String, name: String }

fn fetchUser(id: String): *User {
  http.get("https://api.example.com/users/" + id)
}

fn main(id: String): User | Failure {
  let result = await fetchUser(id)
  match result {
    u: User -> u
    f: Failure -> f
  }
}
```

Key points:

- Effects return `*T`
- `await` produces `T | Failure`
- `match` handles both success and failure

## 3. Read the v0.1 Contracts

Use these as canonical references:

- `/Users/gilles/dev/relang/specs/v0.1/relang-spec-v0.1-canonique.md`
- `/Users/gilles/dev/relang/specs/v0.1/relang-failures-proposal.md`
- `/Users/gilles/dev/relang/specs/v0.1/relang-functions.md`

## Next Step

Continue with [Working with Awaitables](tutorial-awaitables.md).

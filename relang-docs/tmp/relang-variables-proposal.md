# ReLang Variables and Bindings

*Variable Declaration, Scope, and Value Semantics*

---

## 1. Overview

ReLang uses `let` to declare variables and `=` to reassign them.

```relang
let x = 10            // Declare new variable
x = 20                // Reassign (same type required)
let x = "hello"       // Shadow (new variable, can change type)
x.field = value       // Mutate field (local variables only)
```

**Value semantics**: Assignment copies values, not references.

```relang
let x = 10
let y = x             // y is a copy of x
x = 20                // x is now 20, y is still 10
```

---

## 2. Core Model: The Function as Isolation Boundary

The key mental model: **variables are mutable inside their scope and immutable outside.**

| Perspective | Behavior |
|-------------|----------|
| **Inside** the function | Mutable — reassignment, field mutation |
| **Outside** (from caller) | Immutable — value semantics protects the caller |

```relang
fn process(user: User): User {
    let user = user           // Shadow parameter with mutable local copy
    user.name = "Bob"         // OK — user is now a local variable
    user
}

let myUser = User { name: "Alice" }
process(myUser)
// myUser is GUARANTEED unchanged — the function cannot touch it
```

**Why this model?**

1. **Encapsulation** — each function has its own "sandbox"
2. **Local reasoning** — to understand mutations, look only inside the function
3. **No hidden side effects** — the only effects are through return values
4. **Aligned with distributed execution** — each invocation is isolated, state is self-contained

This is essentially Erlang/Elixir's model, but with the ergonomics of local mutation.

**The true invariants** that make ReLang safe for durable execution:

1. **Value semantics**: `y = x` copies, never aliases
2. **Closed functions**: Functions access only their parameters
3. **Immutable parameters**: Function inputs are read-only
4. **Static typing**: Types are checked at compile time
5. **Serializable state**: All values can be checkpointed

---

## 3. Declaration and Assignment

### 3.1 Declaration with `let`

```relang
let name = "Alice"
let age = 30
let active = true
```

`let` serves humans, not the compiler:

1. **Visual marker** — easy to scan for new variables
2. **Intent signal** — "I'm creating something new here"
3. **Error detection** — typos are caught (`userName` vs `userNmae`)

Type annotation when needed:

```relang
let items: [String] = []          // Empty collection needs type
let value: Int | String = 42      // Widening
let parsed: User = Json.parse(data)!  // Generic result
```

### 3.2 Reassignment with `=`

Reassignment updates an existing variable with a value of the **same type**:

```relang
let x = 10
x = 20            // OK — still Int
x = "hello"       // Error: expected Int, got String
```

Reassignment in blocks affects the outer variable:

```relang
let result = "default"
if condition {
    result = "special"    // Reassigns outer result
}
print(result)             // "special" if condition was true
```

### 3.3 Shadowing with `let`

Shadowing creates a **new variable** with the same name, allowing type changes:

```relang
let x = 10          // x: Int
let x = "hello"     // x: String (new variable, shadows previous)
```

| Syntax | Effect | Type |
|--------|--------|------|
| `x = value` | Updates existing variable | Must match |
| `let x = value` | Creates new variable (shadows) | Any type |

**Use cases:**

```relang
// Progressive transformation
let input = readLine()           // String
let input = input.trim()         // String
let input = parse(input)         // Config (type change)

// Type narrowing
let value = getValue()           // Int | String
let value = validate(value)!     // Int
```

Shadowing in blocks creates a local variable:

```relang
let x = 10
if condition {
    let x = 99        // Local x, shadows outer
    print(x)          // 99
}
print(x)              // 10 (outer x unchanged)
```

### 3.4 Field Mutation

Local variables can have their fields mutated:

```relang
let user = User { name: "Alice", email: "a@x.com" }
user.name = "Bob"           // OK — mutates local variable's field
```

Function parameters **cannot** have their fields mutated:

```relang
fn updateUser(user: User): User {
    user.name = "Bob"        // Error: cannot mutate parameter field
    user
}
```

**Why this distinction?**

Allowing `user.name = "Bob"` on a parameter *looks* like it affects the caller, but with value semantics it doesn't. This creates confusion. By making parameters immutable:
- No syntax exists that *looks* like it could affect the caller
- Functions clearly receive inputs and produce outputs
- Data flow is explicit and predictable

**The idiomatic pattern** for modifying a parameter:

```relang
fn updateUser(user: User): User {
    let user = user       // Shadow parameter with mutable local copy
    user.name = "Bob"     // OK — now a local variable
    user
}
```

### 3.5 Summary Table

| Context | Reassign | Field mutation |
|---------|----------|----------------|
| Local variable | ✅ `x = value` | ✅ `x.field = value` |
| Parameter | ❌ Use `let x = x` | ❌ Use `let x = x` |

---

## 4. Scope

### 4.1 Block Scope

Variables declared in a block are scoped to that block:

```relang
if condition {
    let y = 30        // y only visible here
}
print(y)              // Error: y not in scope
```

Blocks can see and reassign outer variables:

```relang
let x = 10
if condition {
    x = 20            // Found in outer scope → reassigns
}
print(x)              // 20
```

### 4.2 Function Scope (Closed)

Functions are **closed** — they cannot see variables from enclosing scopes:

```relang
let x = 10

fn process(input: String): String {
    x = 99            // Error: x not declared (fn can't see outer x)
    input
}

print(x)              // 10 (unchanged)
```

| Context | Can see outer scope? | `x = value` | `let x = value` |
|---------|---------------------|-------------|-----------------|
| `if` / `while` / `for` | Yes | Reassigns outer | Creates local |
| `match` arm | Yes | Reassigns outer | Creates local |
| `fn` body | **No** | Error if not local | Creates local |

### 4.3 Lambdas

Lambdas are **syntactic sugar** for inline code — they execute immediately within the current scope, just like `if`/`for` blocks.

```relang
let x = 10
let result = items.map { it * x }   // Lambda sees x = 10
x = 20                               // Runs AFTER map completes
```

There's no "capture" because there's no closure object. The lambda simply sees variables from the enclosing scope.

### 4.4 Loop Variables

Loop variables are scoped to the loop body:

```relang
for item in items {
    let processed = transform(item)
}
print(item)           // Error: item not in scope
```

---

## 5. Type Inference

The compiler infers types from initializers:

```relang
let x = 42              // Int
let y = 3.14            // Float
let s = "hello"         // String
let list = [1, 2, 3]    // [Int]
let pair = (1 & "a")    // Int & String
```

Type annotation required when:

```relang
let empty: [Int] = []              // Empty collection
let value: Int | String = 42       // Widening
let zero: Float = 0                // Ambiguous literal
let parsed: User = Json.parse(d)!  // Generic result
```

---

## 6. Destructuring

### 6.1 Product Destructuring (Partial)

Binds the first N elements, ignores the rest:

```relang
let (x & y) = getPoint()
let (name & age) = getUser()      // ignores extra fields
```

### 6.2 List Destructuring (Complete)

Must match the structure exactly:

```relang
let [first, second, third] = items    // exact match
let [first, _, third] = items         // skip middle
let [head, ..tail] = items            // head + rest
let [begin.., last] = items           // all but last + last
```

### 6.3 In Other Contexts

**Match expressions:**
```relang
match result {
    (a & b) -> ...              // binds a and b
    [first, second] -> ...      // binds first and second
    _ -> ...                    // wildcard
}
```

**For loops:**
```relang
for (index & value) in items.enumerate() { ... }
for (name & age) in users { ... }
```

---

## 7. Workflow Integration

### 7.1 Checkpoints

Checkpoints capture the current value of all local variables:

```relang
fn processOrder(orderId: String) {
    let order = fetchOrder(orderId)!
    // checkpoint — order is saved

    let payment = processPayment(order)!
    // checkpoint — order AND payment are saved

    completeOrder(order, payment)
}
```

On resume from a checkpoint, variables are restored to their captured values.

### 7.2 Serialization Requirement

All workflow-scoped variables must be serializable:

- Primitives (Int, Float, Bool, String, etc.)
- User-defined types (composed of serializable fields)
- Lists and products of serializable types
- Awaitables (by reference/id)

---

## 8. Reference

### 8.1 Syntax Summary

| Syntax | Meaning |
|--------|---------|
| `let x = value` | Declare new variable |
| `let x: T = value` | Declare with type annotation |
| `x = value` | Reassign (same type required) |
| `x.field = value` | Mutate field (local variables only) |
| `let x = value` | Shadow existing variable (any type) |
| `let (a & b) = pair` | Destructure product (partial) |
| `let [h, ..t] = list` | Destructure list (complete) |

### 8.2 Reserved Words

```
and, as, await, break, continue, else, false, fn, for,
if, in, let, match, none, not, or, raise, return, sealed, self,
shield, spawn, timeout, true, type, unit, while
```

### 8.3 Naming Conventions

```relang
let userName = "Alice"      // camelCase for variables
let MAX_RETRIES = 3         // SCREAMING_SNAKE_CASE for constants (convention)
```

---

## Appendix: Comparison with Other Languages

### vs Rust

```rust
// Rust
let x = 10;
x = 20;           // Error: x is immutable
let mut x = 10;
x = 20;           // OK
let x = "hello";  // OK: shadowing
```

```relang
// ReLang — all variables are reassignable, no `mut` needed
let x = 10
x = 20            // OK
let x = "hello"   // OK: shadowing
```

### vs TypeScript

```typescript
// TypeScript
let x = 10;
x = 20;           // OK
x = "hello";      // Error: type mismatch
let x = "hello";  // Error: cannot redeclare
```

```relang
// ReLang — shadowing allowed
let x = 10
x = 20            // OK
x = "hello"       // Error: type mismatch
let x = "hello"   // OK: shadowing (new variable)
```

### vs Python

```python
# Python — no declaration, dynamic typing
x = 10
x = "hello"       # OK (dynamic typing)
```

```relang
// ReLang — declaration required, static typing
let x = 10
x = "hello"       // Error: type mismatch
let x = "hello"   // OK: explicit shadowing
```

---

*End of proposal*

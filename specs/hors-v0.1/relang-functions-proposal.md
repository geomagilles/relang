# ReLang Functions

*Named Functions and Return Semantics*

---

## 1. Overview

Functions are named, reusable blocks of code.

**Key characteristics:**
- **Self-contained**: Functions can only access their parameters — no scope capture
- **Portable**: Can be exported, imported, and executed remotely (including via `spawn`)
- **Implicit return**: The last expression in a function body is the return value
- **Inferred types**: Return type annotations are optional — the compiler infers them

---

## 2. Syntax

### 2.1 Declaration Forms

**Block body** — for multi-statement functions:

```relang
fn name(parameters): ReturnType {
    body
}
```

**Expression body** — for single-expression functions:

```relang
fn name(parameters): ReturnType = expression
```

The return type annotation is optional in both forms. If omitted, the compiler infers it from the body.

### 2.2 Examples

```relang
// Block body with explicit return type
fn double(x: Int): Int {
    x * 2
}

// Expression body (preferred for one-liners)
fn double(x: Int): Int = x * 2

// Inferred return type
fn double(x: Int) = x * 2

// Multiple parameters
fn add(a: Int, b: Int): Int = a + b

// No parameters
fn version(): String = "1.0.0"

// No meaningful return value (Unit)
fn log(msg: String) {
    print(msg)
}

// Generic function
fn identity<T>(x: T): T = x

// Generic with constraint
fn max<T: Comparable>(a: T, b: T): T = if a > b { a } else { b }
```

### 2.3 Parameters

**All parameters require type annotations:**

```relang
fn greet(name: String, age: Int): String {
    "${name} is ${age} years old"
}
```

**Default values:**

```relang
fn greet(name: String, greeting: String = "Hello"): String {
    "${greeting}, ${name}!"
}

greet("Alice")           // "Hello, Alice!"
greet("Alice", "Hi")     // "Hi, Alice!"
```

**Named arguments:**

```relang
fn createUser(name: String, age: Int, active: Bool): User {
    User { name, age, active }
}

createUser(name: "Alice", age: 30, active: true)
createUser(age: 30, name: "Alice", active: true)  // order doesn't matter
```

**Mixing positional and named** — positional must come first:

```relang
createUser("Alice", age: 30, active: true)  // OK
createUser(name: "Alice", 30, true)          // Error
```

**Parameters are immutable** — to modify, shadow with `let`:

```relang
fn process(count: Int): Int {
    count = count + 1     // Error: cannot reassign parameter
    count * 2
}

fn process(count: Int): Int {
    let count = count + 1 // OK: shadows parameter
    count * 2
}
```

### 2.4 Return Types

Every function has a return type. The annotation is optional — the compiler infers it from the body.

**Explicit return type:**

```relang
fn double(x: Int): Int = x * 2
```

**Inferred return type:**

```relang
fn double(x: Int) = x * 2  // Compiler infers: Int
```

**When to use explicit types:**
- Public API functions (documentation)
- Complex functions where intent should be clear
- When the inferred type might be surprising

**Unit type** — for functions with no meaningful return value:

- `Unit` — the type (capitalized)
- `unit` — the single value (lowercase, like `true`, `false`, `none`)

```relang
fn log(msg: String): Unit {
    print(msg)
}

// Equivalent — compiler infers Unit
fn log(msg: String) {
    print(msg)
}
```

In practice, you rarely write `unit` explicitly:

```relang
// Only needed when branches must match types
fn process(flag: Bool): Unit {
    if flag { doWork() } else { unit }
}
```

**Optional and union return types:**

```relang
fn find(list: [Int], value: Int): Int? {
    list.indexOf(value)
}

fn parse(s: String): Int | Failure {
    Int.parse(s) ?? Failure("invalid number")
}
```

---

## 3. Semantics

### 3.1 Implicit Return

The last expression in a function body is the return value:

```relang
fn process(x: Int): Int {
    let y = x * 2
    let z = y + 1
    z  // returned
}

fn max(a: Int, b: Int): Int {
    if a > b { a } else { b }  // if-expression returned
}

fn describe(n: Int): String {
    match n {
        0 -> "zero"
        1 -> "one"
        _ -> "many"
    }  // match-expression returned
}
```

### 3.2 Early Return

Use `return` for early exit:

```relang
fn divide(a: Int, b: Int): Int | Failure {
    if b == 0 {
        return Failure("division by zero")
    }
    a / b
}

fn findFirst(list: [Int], target: Int): Int? {
    for x in list {
        if x == target {
            return x
        }
    }
    none
}
```

`return` is only needed for early exit. Prefer implicit return otherwise.

### 3.3 Recursion

```relang
fn factorial(n: Int): Int {
    if n <= 1 { 1 } else { n * factorial(n - 1) }
}
```

### 3.4 Hoisting

Functions can be called before their definition (mutual recursion):

```relang
fn isEven(n: Int): Bool {
    if n == 0 { true } else { isOdd(n - 1) }
}

fn isOdd(n: Int): Bool {
    if n == 0 { false } else { isEven(n - 1) }
}
```

---

## 4. Restrictions

### 4.1 Self-Contained (No Scope Capture)

Functions can only access their parameters — not variables from enclosing scope:

```relang
let rate = 0.1

fn applyRate(price: Int): Int {
    price * (1 - rate)   // Error: cannot access 'rate'
}

// Correct: pass as parameter
fn applyRate(price: Int, rate: Float): Int {
    price * (1 - rate)
}
```

### 4.2 Top-Level Only

Functions must be defined at file level. Nested functions are not allowed:

```relang
fn outer(x: Int): Int {
    fn inner(y: Int): Int {  // Error: nested function
        y * 2
    }
    inner(x)
}
```

### 4.3 No Function Types

Functions cannot be stored in variables, passed as parameters, or returned:

```relang
fn apply(x: Int, f: ???): Int {  // No function type exists
    f(x)
}
```

Use lambdas with built-in operations instead (see next section).

---

## 5. Functions vs Lambdas

| Aspect | Function (`fn`) | Lambda (`{ }`) |
|--------|-----------------|----------------|
| Syntax | `fn name(x: Int): Int { x * 2 }` | `{ x -> x * 2 }` |
| Named | Yes | No |
| Recursive | Yes | No |
| Hoisted | Yes | No |
| Scope capture | **No** — self-contained | **Yes** — sees enclosing scope |
| Storable/passable | No | No (inline only) |
| Use case | Reusable logic | Inline with built-ins |

**Lambdas** are syntactic sugar for built-in collection operations:

```relang
[1, 2, 3].map { it * 2 }       // [2, 4, 6]
[1, 2, 3].filter { it > 1 }    // [2, 3]
users.find { it.active }
```

**Key distinction:**
- Functions are **self-contained** — can be spawned to run on different servers
- Lambdas can **capture scope** — but are inline-only, for use with built-ins

**Execution modes:**
- `f()` — synchronous call, returns `T`
- `spawn f()` — asynchronous call, returns `*T`

---

## 6. Quick Reference

| Form | Example |
|------|---------|
| Block body | `fn double(x: Int): Int { x * 2 }` |
| Expression body | `fn double(x: Int): Int = x * 2` |
| Inferred type | `fn double(x: Int) = x * 2` |
| No return (Unit) | `fn log(msg: String) { print(msg) }` |
| Default params | `fn greet(name: String, msg: String = "Hi") { ... }` |
| Generic | `fn identity<T>(x: T): T = x` |
| Constrained generic | `fn max<T: Comparable>(a: T, b: T): T = ...` |
| Early return | `if bad { return error }; result` |

---

## 7. Design Rationale

### 7.1 Why Self-Contained (No Scope Capture)?

ReLang is designed for **distributed execution**:
- A spawned function may execute on a different server
- If functions captured scope variables, those values wouldn't exist remotely
- By requiring explicit parameters, all functions are portable

Benefits:
- **Explicit data flow** — you see exactly what each function depends on
- **Easier debugging** — no hidden dependencies
- **Predictable behavior** — same code works the same everywhere

This follows the model of Go and Elixir, where module-level functions don't capture.

### 7.2 Why No Function Types?

- **Simplicity**: Avoids complex type system features (variance, higher-kinded types)
- **Distributed safety**: Closures can't be serialized safely with captures
- **Lambdas suffice**: Built-in operations (`map`, `filter`) cover common cases

### 7.3 Why Implicit Return?

- **Consistency**: Same rule for functions and lambdas
- **Conciseness**: Less boilerplate for expression-oriented code
- **Precedent**: Rust, Ruby, Scala, Kotlin all use this approach

### 7.4 Why Expression Body `=`?

- Mirrors mathematical notation: `f(x) = x²`
- Clear visual distinction from block body
- Eliminates braces for one-liners

### 7.5 Why Inferred Return Types?

Return type annotations are optional — the compiler infers the type from the body.

The function always has a static return type; inference just means the programmer doesn't have to write it. This follows Kotlin's model.

Explicit types are recommended for public APIs because they:
- Document intent
- Enable better error messages
- Prevent accidental API changes when the body changes

### 7.6 Why Immutable Parameters?

- **No confusion about side effects** — calling `process(x)` cannot modify `x`
- **Clear data flow** — functions receive inputs (read-only), return outputs
- **Value semantics** — even if mutation were allowed, it wouldn't affect the caller

### 7.7 Why Unit Instead of None or Void?

`Unit` is the type for functions with no meaningful return value.

**Why not `None`?**
- `None` means "absence of a value" (used in optionals: `Int?` = `Int | None`)
- `Unit` means "a value that carries no information"
- Conflating them causes type confusion

**Why not `void`?**
- In C/Java, `void` is not a real type — you can't have `List<void>`
- `Unit` is a proper type, enabling uniform generic handling

**Why `unit` literal instead of `()`?**
- `unit` is more readable and consistent with other literals (`true`, `false`, `none`)
- Follows the pattern: type is capitalized (`Unit`), value is lowercase (`unit`)

---

*End of specification*

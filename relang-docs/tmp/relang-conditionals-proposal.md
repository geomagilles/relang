# ReLang Conditionals

*Control Flow and Pattern Matching*

---

## 1. Overview

ReLang provides three conditional constructs:

| Construct | Use case |
|-----------|----------|
| `if` | Binary conditions |
| `match` | Pattern matching on values |
| `?.` / `??` | Optional handling |

All conditionals are **expressions** — they return values.

---

## 2. If Expression

### 2.1 Basic Form

```relang
if condition {
    thenBranch
} else {
    elseBranch
}
```

The condition must be `Bool`. Both branches must have compatible types.

### 2.2 As Expression

`if` returns a value:

```relang
let max = if a > b { a } else { b }

let status = if isActive { "active" } else { "inactive" }
```

### 2.3 As Statement

When the result is not used, branches can have type `Unit`:

```relang
if isDebug {
    print("Debug mode")
}
// else branch is implicit: else { }
```

### 2.4 No Else-If

ReLang does not have `else if`. For multi-way conditions, use `match`:

```relang
// ✗ Not supported
if score >= 90 {
    "A"
} else if score >= 80 {
    "B"
}

// ✓ Use match instead
let grade = match {
    score >= 90 -> "A"
    score >= 80 -> "B"
    score >= 70 -> "C"
    _ -> "F"
}
```

This keeps `if` simple (binary only) and encourages `match` for complex branching.

### 2.5 No Implicit Truthiness

Only `Bool` is allowed as a condition:

```relang
if count { ... }        // ✗ Compile error: Int is not Bool
if name { ... }         // ✗ Compile error: String is not Bool
if user != none { ... } // ✓ OK: comparison returns Bool
```

---

## 3. Match Expression

### 3.1 Two Forms of Match

ReLang's `match` has two forms:

| Form | Syntax | Purpose |
|------|--------|---------|
| With subject | `match value { patterns }` | Pattern matching on a value |
| Subjectless | `match { conditions }` | Conditional dispatch |

#### Match with Subject — Pattern Matching

```relang
match value {
    pattern1 -> result1
    pattern2 -> result2
    _ -> defaultResult
}
```

Matches `value` against patterns in order. The first matching pattern executes.

#### Subjectless Match — Conditional Dispatch

```relang
match {
    condition1 -> result1
    condition2 -> result2
    _ -> defaultResult
}
```

Evaluates boolean conditions in order. The first `true` condition executes. This replaces `else if` chains.

```relang
let grade = match {
    score >= 90 -> "A"
    score >= 80 -> "B"
    score >= 70 -> "C"
    _ -> "F"
}

let category = match {
    age < 13 -> "child"
    age < 20 -> "teenager"
    age < 65 -> "adult"
    _ -> "senior"
}
```

The `_` arm is required since the compiler cannot prove boolean expressions are exhaustive.

### 3.2 Patterns (Subject Form)

**Supported patterns:**

| Pattern | Example | Meaning |
|---------|---------|---------|
| Literal | `1 -> ...` | Match exact value |
| Type test | `is Int -> ...` | Match type, narrow original variable |
| Type binding | `n: Int -> ...` | Match type, bind to new name |
| Wildcard | `_ -> ...` | Match anything, discard |
| None | `none -> ...` | Match absence |

### 3.3 Matching Literals

```relang
let name = match day {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    7 -> "Sunday"
    _ -> "Invalid"
}
```

### 3.4 Matching Types

ReLang provides two patterns for type matching:

#### Type Test (`is Type`) — Flow Narrowing

When matching a variable, use `is Type` to narrow the original variable:

```relang
let description = match value {
    is Int -> "Integer: $value"
    is String -> "String: $value"
    is Bool -> "Boolean: $value"
}
```

The original `value` is narrowed to the matched type within each branch.

#### Type Binding (`name: Type`) — New Name

When matching an expression, or when you want a different name, bind explicitly:

```relang
let description = match getData() {
    d: Int -> "Integer: $d"
    s: String -> "String: $s"
    b: Bool -> "Boolean: $b"
}
```

#### When to Use Each

| Pattern | Use When |
|---------|----------|
| `is Type` | Matching a variable — reuse its name |
| `name: Type` | Matching an expression, or want a new name |

```relang
// Variable — use is Type
let value: Int | String = getData()
match value {
    is Int -> compute(value)      // value is narrowed to Int
    is String -> parse(value)     // value is narrowed to String
}

// Expression — use name: Type
match fetchUser() {
    u: User -> greet(u)
    e: Error -> report(e)
}
```

#### Combining Type Matching with Conditions

When you need to match a type AND check a condition, use nested `if` or subjectless `match`:

```relang
// Type match + condition with nested if
match getData() {
    i: Int -> if i > 0 { "positive" } else { "non-positive" }
    s: String -> "string"
}

// Type match + multiple conditions with nested match
match getData() {
    i: Int -> match {
        i > 100 -> "large"
        i > 0 -> "small positive"
        _ -> "non-positive"
    }
    s: String -> "string"
}
```

This keeps concerns separated: pattern matching handles type discrimination, `if`/`match` handles value conditions.

### 3.5 Matching Optional Values

```relang
match name {
    is String -> print("Name: $name")
    none -> print("No name provided")
}
```

### 3.6 Matching Sealed Types

```relang
sealed Shape
type Circle : Shape { radius: Int }
type Square : Shape { side: Int }
type Rectangle : Shape { width: Int, height: Int }

let area = match shape {
    is Circle -> 3.14 * shape.radius * shape.radius
    is Square -> shape.side * shape.side
    is Rectangle -> shape.width * shape.height
}
```

The compiler verifies exhaustiveness — all variants must be handled.

### 3.7 Matching Sum Types

```relang
let result: Int | String = getValue()

match result {
    is Int -> print("Got number: $result")
    is String -> print("Got text: $result")
}
```

### 3.8 Wildcard Pattern

`_` matches anything and discards the value:

```relang
match value {
    0 -> "Zero"
    1 -> "One"
    _ -> "Other"
}
```

### 3.9 Exhaustiveness

The compiler requires all cases to be handled:

```relang
match shape {
    c: Circle -> c.radius
    // ✗ Compile error: missing cases for Square, Rectangle
}
```

Use `_` for catch-all when appropriate:

```relang
match shape {
    c: Circle -> c.radius
    _ -> 0  // Handles Square, Rectangle
}
```

---

## 4. Optional Operators

### 4.1 Safe Navigation (`?.`)

Access members of optional values safely:

```relang
let user: User? = getUser()
let name: String? = user?.profile?.name
```

If any step is `none`, the entire expression is `none`.

Equivalent to:
```relang
let name: String? = match user {
    is User -> match user.profile {
        is Profile -> user.profile.name
        none -> none
    }
    none -> none
}
```

### 4.2 Null Coalescing (`??`)

Provide a default value:

```relang
let user: User? = getUser()
let name: String = user?.name ?? "Anonymous"
```

The right side is only evaluated if the left side is `none`.

### 4.3 Force Unwrap (`!`)

Assert that a value is not `none`:

```relang
let maybeName: String? = getName()
let name: String = maybeName!  // raises Failure if none
```

Raises `Failure` if the value is `none`.

### 4.4 Combining Operators

```relang
let user: User? = getUser()
let company: Company? = getCompany()

// Safe navigation + default
let city: String = user?.address?.city ?? "Unknown"

// Safe navigation + force unwrap
let email: String = user?.email!  // raises if result is none

// Chained safe navigation
let zip: String? = company?.headquarters?.address?.zipCode
```

---

## 5. Conditional Expressions Summary

### 5.1 When to Use What

| Scenario | Construct |
|----------|-----------|
| Binary choice | `if` / `else` |
| Multi-way conditions | `match { conditions }` (subjectless) |
| Multiple specific values | `match` with literals |
| Type discrimination (variable) | `match` with `is Type` |
| Type discrimination (expression) | `match` with `name: Type` |
| Type + condition | `match` with nested `if` or `match {}` |
| Optional handling (simple) | `?.`, `??`, `!` |
| Optional handling (complex) | `match` with `none` |
| Sealed type dispatch | `match` (exhaustive) |

### 5.2 Expression Types

All conditionals return values:

```relang
// if returns the type of its branches
let x: Int = if cond { 1 } else { 2 }

// match returns the common type of all branches
let y: String = match n {
    1 -> "one"
    _ -> "other"
}

// ?. returns optional
let z: String? = user?.name

// ?? returns non-optional
let w: String = user?.name ?? "default"
```

---

## 6. Smart Casts

The compiler tracks type checks and narrows accordingly:

```relang
let name: String? = getName()

if name != none {
    // name is String here, not String?
    print(name.length())
}

match name {
    is String -> {
        // name is narrowed to String
        print(name.length())
    }
    none -> print("No name")
}
```

The `is Type` pattern leverages the same flow-sensitive narrowing as `if (x is T)`.

---

## 7. No Ternary Operator

ReLang does not have a C-style ternary operator (`?:`). Use `if`/`else`:

```relang
// Not supported: condition ? then : else

// Use if/else instead:
let result = if condition { thenValue } else { elseValue }
```

The `if`/`else` expression is equally concise and more readable.

---

## 8. Summary

| Syntax | Purpose |
|--------|---------|
| `if c { a } else { b }` | Binary condition |
| `if c { a }` | Conditional execution (Unit) |
| `match v { p -> r }` | Pattern matching on value |
| `match { c -> r }` | Conditional dispatch (multi-way if) |
| `is Type` | Type pattern with flow narrowing |
| `n: Type` | Type pattern with binding |
| `x?.y` | Safe navigation |
| `x ?? d` | Default value |
| `x!` | Force unwrap |

**Key principles:**
- All conditionals are expressions
- `if` is binary only (no `else if` — use subjectless `match`)
- Only `Bool` for `if` conditions (no implicit truthiness)
- `match value {}` for pattern matching, `match {}` for conditional dispatch
- `match` must be exhaustive (subjectless requires `_`)
- `is Type` narrows the original variable
- `name: Type` binds to a new name (for expressions)
- No guards — use nested `if` or `match {}` for conditions after type matching
- Smart casts after type checks

---

*End of specification*

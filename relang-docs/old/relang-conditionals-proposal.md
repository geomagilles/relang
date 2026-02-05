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
let grade = match score {
    s if s >= 90 => "A"
    s if s >= 80 => "B"
    s if s >= 70 => "C"
    _ => "F"
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

### 3.1 Basic Form

```relang
match value {
    pattern1 => result1
    pattern2 => result2
    _ => defaultResult
}
```

**Supported patterns:**

| Pattern | Example | Meaning |
|---------|---------|---------|
| Literal | `1 => ...` | Match exact value |
| Binding | `n => ...` | Bind to name |
| Type | `i: Int => ...` | Match type, bind name |
| Guard | `n if n > 0 => ...` | Condition on binding |
| Wildcard | `_ => ...` | Match anything, discard |
| None | `none => ...` | Match absence |

### 3.2 Matching Literals

```relang
let name = match day {
    1 => "Monday"
    2 => "Tuesday"
    3 => "Wednesday"
    4 => "Thursday"
    5 => "Friday"
    6 => "Saturday"
    7 => "Sunday"
    _ => "Invalid"
}
```

### 3.3 Matching Types

```relang
let description = match value {
    i: Int => "Integer: ${i}"
    s: String => "String: ${s}"
    b: Bool => "Boolean: ${b}"
}
```

### 3.4 Matching Optional Values

```relang
match name {
    s: String => print("Name: ${s}")
    none => print("No name provided")
}
```

### 3.5 Matching Sealed Types

```relang
sealed Shape
type Circle : Shape { radius: Int }
type Square : Shape { side: Int }
type Rectangle : Shape { width: Int, height: Int }

let area = match shape {
    c: Circle => 3.14 * c.radius * c.radius
    s: Square => s.side * s.side
    r: Rectangle => r.width * r.height
}
```

The compiler verifies exhaustiveness — all variants must be handled.

### 3.6 Matching Sum Types

```relang
let result: Int | String = getValue()

match result {
    i: Int => print("Got number: ${i}")
    s: String => print("Got text: ${s}")
}
```

### 3.7 Matching with Guards

Add conditions with `if`:

```relang
// Single type — type annotation optional
match value {
    n if n > 42 => "large"
    n if n > 12 => "medium"
    n if n > 3 => "small"
    _ => "tiny"
}

// Sum type — type annotation required
match result {
    i: Int if i > 0 => "positive int"
    i: Int => "non-positive int"
    s: String => "string"
}
```

**Exhaustiveness rule:** When using guards on a type, the last pattern for that type must be unguarded:

```relang
// ✓ OK — unguarded Int catches the rest
match result {
    i: Int if i > 0 => "positive"
    i: Int => "other"          // Required fallback
    s: String => "string"
}

// ✗ Compile error — no fallback for Int
match result {
    i: Int if i > 0 => "positive"
    i: Int if i < 0 => "negative"
    // Error: missing unguarded Int pattern
    s: String => "string"
}
```

The compiler cannot prove guards are exhaustive, so an unguarded fallback is required.

### 3.8 Wildcard Pattern

`_` matches anything and discards the value:

```relang
match value {
    0 => "Zero"
    1 => "One"
    _ => "Other"
}
```

### 3.9 Exhaustiveness

The compiler requires all cases to be handled:

```relang
match shape {
    c: Circle => c.radius
    // ✗ Compile error: missing cases for Square, Rectangle
}
```

Use `_` for catch-all when appropriate:

```relang
match shape {
    c: Circle => c.radius
    _ => 0  // Handles Square, Rectangle
}
```

---

## 4. Optional Operators

### 4.1 Safe Navigation (`?.`)

Access members of optional values safely:

```relang
let name: String? = user?.profile?.name
```

If any step is `none`, the entire expression is `none`.

Equivalent to:
```relang
let name: String? = match user {
    u: User => match u.profile {
        p: Profile => p.name
        none => none
    }
    none => none
}
```

### 4.2 Null Coalescing (`??`)

Provide a default value:

```relang
let name: String = user?.name ?? "Anonymous"
```

The right side is only evaluated if the left side is `none`.

### 4.3 Force Unwrap (`!`)

Assert that a value is not `none`:

```relang
let name: String = user?.name!
```

Raises `Failure` if the value is `none`.

### 4.4 Combining Operators

```relang
// Safe navigation + default
let city: String = user?.address?.city ?? "Unknown"

// Safe navigation + force unwrap
let email: String = user?.email!  // raises if user or email is none

// Chained safe navigation
let zip: String? = company?.headquarters?.address?.zipCode
```

---

## 5. Conditional Expressions Summary

### 5.1 When to Use What

| Scenario | Construct |
|----------|-----------|
| Binary choice | `if` / `else` |
| Multi-way conditions | `match` with guards |
| Multiple specific values | `match` with literals |
| Type discrimination | `match` with type patterns |
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
    1 => "one"
    _ => "other"
}

// ?. returns optional
let z: String? = user?.name

// ?? returns non-optional
let w: String = user?.name ?? "default"
```

---

## 6. Smart Casts

The compiler tracks null checks:

```relang
let name: String? = getName()

if name != none {
    // name is String here, not String?
    print(name.length())
}

match name {
    s: String => {
        // s is String
        print(s.length())
    }
    none => print("No name")
}
```

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
| `match v { p => r }` | Pattern matching |
| `x?.y` | Safe navigation |
| `x ?? d` | Default value |
| `x!` | Force unwrap |

**Key principles:**
- All conditionals are expressions
- `if` is binary only (no `else if` — use `match`)
- Only `Bool` for `if` conditions (no implicit truthiness)
- `match` must be exhaustive
- Smart casts after null checks

---

*End of specification*

# ReLang Inlined Lambdas

*Inline Anonymous Functions*

---

## 1. Overview

Lambdas are **syntactic sugar** for inline anonymous functions. They are used exclusively with built-in higher-order operations like `map`, `filter`, and `fold`.

**Key characteristics:**
- Inline only — cannot be stored in variables
- No lambda types — purely syntactic
- Transparent scope — can access variables from enclosing scope
- Used with built-in operations only

---

## 2. Basic Syntax

Lambdas are enclosed in braces `{ }` with an arrow `->` separating parameters from body.

### 2.1 Single Parameter

```relang
{ x -> x * 2 }
```

- `{ }` — braces delimit the lambda
- `x` — parameter name
- `->` — separates parameters from body
- `x * 2` — body (last expression is return value)

### 2.2 Multiple Parameters

```relang
{ a, b -> a + b }
```

### 2.3 No Parameters

When there are no parameters, omit the arrow:

```relang
{ 42 }
```

### 2.4 Multi-line Body

The last expression is the return value:

```relang
{ x ->
    let doubled = x * 2
    let result = doubled + 1
    result
}
```

---

## 3. Type Inference

Lambda parameter types are inferred from the context (the built-in operation):

```relang
[1, 2, 3].map { x -> x * 2 }        // x inferred as Int
["a", "b"].map { s -> s.length() }  // s inferred as String
```

Explicit type annotations when needed for clarity:

```relang
{ x: Int -> x * 2 }
{ x: Int, y: Int -> x + y }
```

---

## 4. Trailing Lambda Syntax

When a built-in operation takes a lambda, it can be placed outside parentheses.

### 4.1 Lambda Outside Parentheses

```relang
// Lambda inside parentheses (always valid)
[1, 2, 3].map({ x -> x * 2 })

// Trailing lambda: outside parentheses
[1, 2, 3].map { x -> x * 2 }
```

### 4.2 With Other Parameters

```relang
[1, 2, 3].reduce(0) { acc, x -> acc + x }
[1, 2, 3].fold("") { acc, x -> acc + x.toString() }
```

### 4.3 Lambda as Only Parameter

When the lambda is the only parameter, parentheses can be omitted:

```relang
items.forEach { x -> print(x) }
numbers.filter { x -> x > 0 }
```

### 4.4 Readability

Trailing lambdas read like built-in control structures:

```relang
items.forEach { item ->
    process(item)
    log(item)
}

users.filter { user -> user.isActive }
     .map { user -> user.name }
     .sorted()
```

---

## 5. Implicit Parameter: `it`

When a lambda has exactly one parameter, you can omit the declaration and use `it`:

```relang
// Explicit parameter
[1, 2, 3].map { x -> x * 2 }

// Implicit 'it'
[1, 2, 3].map { it * 2 }
[1, 2, 3].filter { it > 1 }
users.find { it.isActive }
```

Use `it` for short, simple lambdas. Use explicit names when:
- The lambda body is complex
- The name adds clarity
- There are nested lambdas (avoid `it` ambiguity)

```relang
// 'it' is clear
numbers.filter { it > 0 }

// Explicit name is clearer
users.filter { user -> user.age > 18 and user.isVerified }
```

---

## 6. Scope Access

Lambdas have **transparent scope** — they can access variables from their enclosing context, just like `if`/`for` blocks.

### 6.1 Accessing Outer Variables

```relang
let factor = 10
[1, 2, 3].map { x -> x * factor }  // uses 'factor' → [10, 20, 30]
```

### 6.2 Accessing Multiple Variables

```relang
let offset = 10
let scale = 2
[1, 2, 3].map { x -> x * scale + offset }  // uses both → [12, 14, 16]
```

### 6.3 Inline Execution (No Capture)

Lambdas are **syntactic sugar** — they execute immediately as part of the operation they're passed to. There is no "capture" because:

- Lambdas cannot be stored in variables
- Lambdas cannot be passed to user-defined functions
- Lambdas cannot outlive their enclosing scope

The lambda simply sees the current value of variables when it executes:

```relang
let threshold = 100
let filtered = items.filter { it.price > threshold }  // uses threshold = 100
threshold = 200                                        // runs AFTER filter completes
```

This is simpler than traditional closures and avoids serialization complexity.

---

## 7. Built-in Operations

Lambdas work with built-in higher-order operations on collections:

### 7.1 Transformation

```relang
// map: transform each element
[1, 2, 3].map { it * 2 }                    // [2, 4, 6]

// flatMap: transform and flatten
[[1, 2], [3, 4]].flatMap { it }             // [1, 2, 3, 4]
```

### 7.2 Filtering

```relang
// filter: keep matching elements
[1, 2, 3, 4].filter { it > 2 }              // [3, 4]

// partition: split by predicate
[1, 2, 3, 4].partition { it % 2 == 0 }      // ([2, 4], [1, 3])
```

### 7.3 Aggregation

```relang
// reduce: combine elements
[1, 2, 3].reduce(0) { acc, x -> acc + x }   // 6

// fold: like reduce with different accumulator type
[1, 2, 3].fold("") { acc, x -> acc + x.toString() }  // "123"
```

### 7.4 Search

```relang
// find: first matching element
[1, 2, 3].find { it > 1 }                   // 2 (or none)

// any: check if any matches
[1, 2, 3].any { it > 2 }                    // true

// all: check if all match
[1, 2, 3].all { it > 0 }                    // true
```

### 7.5 Ordering

```relang
// sortedBy: sort by key
users.sortedBy { it.name }

// groupBy: group by key
users.groupBy { it.department }
```

### 7.6 Iteration

```relang
// forEach: execute for each element
items.forEach { print(it) }
```

---

## 8. Restrictions

### 8.1 Cannot Store in Variables

Lambdas have no type — they cannot be assigned to variables:

```relang
// ✗ Error: lambdas cannot be stored
let double = { x -> x * 2 }

// ✓ Use inline
[1, 2, 3].map { x -> x * 2 }
```

### 8.2 Cannot Pass as Parameters

Functions cannot accept lambdas as parameters (no lambda type exists):

```relang
// ✗ Error: no lambda type
fn apply(x: Int, f: ???): Int {
    f(x)
}

// ✓ Use built-in operations
[x].map { it * 2 }.first()
```

### 8.3 No Return as Value

Functions cannot return lambdas:

```relang
// ✗ Error: no lambda type
fn multiplier(factor: Int): ??? {
    { x -> x * factor }
}
```

### 8.4 No Recursion

Lambdas cannot call themselves (they have no name):

```relang
// ✗ Error: cannot reference self
[1, 2, 3].map { n -> if n <= 1 { 1 } else { n * ??? } }

// ✓ Use a named function
fn factorial(n: Int): Int {
    if n <= 1 { 1 } else { n * factorial(n - 1) }
}
```

### 8.5 No Await Inside Lambdas

Lambdas cannot contain `await`:

```relang
// ✗ Error: await not allowed in lambda
urls.map { url -> fetch(url) }

// ✓ Use explicit coordination
let fetches = urls.map { url -> fetch(url) }  // [*Response]
let responses = (await and(fetches))!          // await all in parallel
```

---

## 9. Comparison with Named Functions

| Aspect | Lambda | `fn` |
|--------|--------|------|
| Syntax | `{ x -> x * 2 }` | `fn name(...) { }` |
| Named | No | Yes |
| Storable | No | — |
| Has type | No | — |
| Scope access | **Yes** (like blocks) | **No** (closed) |
| Recursion | No | Yes |
| Await | No | Yes |
| Use case | Inline with built-ins | Reusable logic |

**Key distinction:** Lambdas can access variables from enclosing scope (like `if`/`for` blocks). Named functions (`fn`) are **closed** — they can only access their parameters. This makes named functions portable across distributed execution contexts.

**Execution modes for `fn`:**
- `f()` — inline call, returns `T`
- `spawn f()` — parallel call, returns `*T` (awaitable)

---

## 10. Summary

```relang
// Single parameter
[1, 2, 3].map { x -> x * 2 }

// Implicit 'it'
[1, 2, 3].filter { it > 1 }

// Multiple parameters
[1, 2, 3].reduce(0) { acc, x -> acc + x }

// Scope access
let threshold = 10
items.filter { it.value > threshold }

// Chaining
users.filter { it.isActive }
     .map { it.name }
     .sorted()
```

**Key principles:**
- Lambdas are syntactic sugar — inline only
- No lambda types — cannot store or pass
- Transparent scope — access variables from enclosing scope (like blocks)
- Used exclusively with built-in operations (`map`, `filter`, `fold`, etc.)

---

*End of proposal*

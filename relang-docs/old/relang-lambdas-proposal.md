# ReLang Lambda Functions

*Anonymous Functions and Closures*

---

## 1. Overview

Lambda functions (also called anonymous functions or closures) are unnamed functions defined inline. They are commonly used with collection operations, callbacks, and higher-order functions.

---

## 2. Basic Syntax

Lambdas are enclosed in braces `{ }` with an optional arrow `->` separating parameters from the body.

### 2.1 Single Parameter

```relang
{ x -> x * 2 }
```

- `{ }` — braces delimit the lambda
- `x` — parameter name
- `->` — separates parameters from body
- `x * 2` — body (last expression is the return value)

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

## 3. Type Annotations

### 3.1 Parameter Types

Types are usually inferred from context:

```relang
[1, 2, 3].map { x -> x * 2 }  // x inferred as Int
```

Explicit types when needed:

```relang
{ x: Int -> x * 2 }
{ x: Int, y: Int -> x + y }
```

---

## 4. Trailing Lambda Syntax

When the last parameter of a function is a lambda, it can be placed outside the parentheses.

### 4.1 Lambda as Last Parameter

```relang
// Lambda inside parentheses (always valid)
[1, 2, 3].map({ x -> x * 2 })

// Trailing lambda: outside parentheses
[1, 2, 3].map { x -> x * 2 }
```

### 4.2 With Other Parameters

```relang
// Other params in parens, lambda outside
[1, 2, 3].reduce(0) { acc, x -> acc + x }
[1, 2, 3].fold("") { acc, x -> acc + x.toString() }
```

### 4.3 Lambda as Only Parameter

When the lambda is the only parameter, parentheses can be omitted entirely:

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

When a lambda has exactly one parameter, you can omit the parameter declaration and use `it`:

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

## 6. Usage Examples

### 6.1 With Collections

```relang
// Map
[1, 2, 3].map { it * 2 }                        // [2, 4, 6]

// Filter
[1, 2, 3, 4].filter { it > 2 }                  // [3, 4]

// Reduce
[1, 2, 3].reduce(0) { acc, x -> acc + x }       // 6

// Sort by key
users.sortedBy { it.name }

// Find
[1, 2, 3].find { it > 1 }                       // Some(2)

// Partition
[1, 2, 3, 4].partition { it % 2 == 0 }          // ([2, 4], [1, 3])
```

### 6.2 With Higher-Order Functions

```relang
fn apply(x: Int, f: (Int) -> Int): Int {
    f(x)
}

let result = apply(5) { it * it }  // 25
```

### 6.3 Stored in Variables

```relang
let double: (Int) -> Int = { it * 2 }
let result = double(5)  // 10
```

### 6.4 As Return Values

```relang
fn multiplier(factor: Int): (Int) -> Int {
    { x -> x * factor }
}

let triple = multiplier(3)
triple(4)  // 12
```

---

## 7. Closures

Lambdas capture variables from their enclosing scope.

### 7.1 Capturing Values

```relang
let factor = 10
let scale = { x -> x * factor }  // captures 'factor'
scale(5)  // 50
```

### 7.2 Capture Semantics

Captured variables are **copied by value** at the time the lambda is created. Since all values in ReLang are immutable, there's no distinction between capturing by value or by reference.

### 7.3 Capturing Multiple Values

```relang
let offset = 10
let scale = 2
let transform = { x -> x * scale + offset }

transform(5)  // 20
```

---

## 8. Function Types

### 8.1 Type Syntax

Function types use arrow syntax:

```relang
(Int) -> Int                    // one param, returns Int
(Int, Int) -> Int               // two params
() -> String                    // no params
(String) -> Unit                // returns nothing meaningful
([Int]) -> Int                  // list param
(Int) -> (Int) -> Int           // returns a function
```

### 8.2 In Type Annotations

```relang
let f: (Int) -> Int = { it * 2 }

fn process(items: [Int], transform: (Int) -> Int): [Int] {
    items.map(transform)
}
```

### 8.3 Optional Function Parameters

```relang
fn fetch(url: String, onError: ((Failure) -> Unit)?): Json {
    // ...
}

// Called without callback
fetch("https://api.example.com", none)

// Called with callback
fetch("https://api.example.com") { err -> log(err) }
```

---

## 9. Restrictions

### 9.1 No Recursion

Lambdas cannot call themselves directly (they have no name):

```relang
// This doesn't work
let factorial = { n -> if n <= 1 { 1 } else { n * factorial(n - 1) } }  // Error
```

Use a named function for recursion:

```relang
fn factorial(n: Int): Int {
    if n <= 1 { 1 } else { n * factorial(n - 1) }
}
```

### 9.2 No Await Inside Lambdas

Lambdas passed to collection operations cannot contain `await`:

```relang
// This doesn't work
urls.map { url -> fetch(url).await() }  // Error: await not allowed here

// Use explicit coordination
let fetches = urls.map { url -> fetch(url) }  // [*Response]
let responses = (fetches and).await()          // await all in parallel
```

This restriction ensures that collection operations remain synchronous and deterministic.

---

## 10. Comparison with Named Functions

| Aspect | Lambda | Named Function |
|--------|--------|----------------|
| Syntax | `{ x -> x * 2 }` | `fn double(x: Int): Int { x * 2 }` |
| Name | Anonymous | Named |
| Recursion | No | Yes |
| Hoisting | No | Yes (can call before definition) |
| Closures | Yes | No (only accesses parameters) |
| Return | Last expression | Last expression |
| Use case | Inline, short-lived | Reusable, complex logic |

---

## 11. Summary

| Form | Example |
|------|---------|
| Single param | `{ x -> x * 2 }` |
| Multiple params | `{ a, b -> a + b }` |
| With types | `{ x: Int, y: Int -> x + y }` |
| No params | `{ 42 }` |
| Implicit `it` | `{ it * 2 }` |
| Multi-line | `{ x -> let y = x * 2; y + 1 }` |
| Trailing lambda | `list.map { x -> x * 2 }` |
| Only param | `items.forEach { print(it) }` |

---

## 12. Design Notes

### 12.1 Why Braces?

The `{ params -> body }` syntax (from Kotlin):
- Clear visual boundary for lambda scope
- Natural for multi-line bodies
- Trailing lambdas read like control structures
- No ambiguity about where lambda ends

### 12.2 Why Implicit Return?

The last expression is the return value in both lambdas and functions:
- **Unified rule**: Same behavior everywhere — no special cases
- **Concise syntax**: Less boilerplate
- **Precedent**: Rust, Ruby, Scala all use this approach

See also: [Functions](relang-functions-proposal.md) for the same rule applied to named functions.

### 12.3 Why Copy Capture?

All captures are by value because:
- ReLang values are immutable — no observable difference
- Simplifies reasoning about closure behavior
- Safe for checkpointing — no hidden mutable state

### 12.4 Why No Await in Lambdas?

Collection operations like `map` and `filter` are synchronous transformations. Allowing `await` inside would:
- Make execution order unpredictable
- Complicate checkpointing
- Mix coordination concerns with data transformation

For async operations on collections, use explicit coordination operators.

---

*End of specification*

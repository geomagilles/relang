# ReLang Functions

*Named Functions and Return Semantics*

---

## 1. Overview

Functions are named, reusable blocks of code. In ReLang, the last expression in a function body is implicitly the return value.

---

## 2. Basic Syntax

### 2.1 Function Declaration

```relang
fn name(parameters): ReturnType {
    body
}
```

### 2.2 Simple Function

```relang
fn double(x: Int): Int {
    x * 2
}
```

- `fn` — keyword
- `double` — function name
- `(x: Int)` — parameters with types
- `: Int` — return type
- `x * 2` — body (last expression is the return value)

### 2.3 Multiple Parameters

```relang
fn add(a: Int, b: Int): Int {
    a + b
}
```

### 2.4 No Parameters

```relang
fn getVersion(): String {
    "1.0.0"
}
```

### 2.5 No Return Value

Functions that return nothing use `Unit`:

```relang
fn log(message: String): Unit {
    print(message)
}
```

`Unit` can be omitted:

```relang
fn log(message: String) {
    print(message)
}
```

---

## 3. Implicit Return

The last expression in a function body is the return value.

### 3.1 Single Expression

```relang
fn double(x: Int): Int {
    x * 2
}
```

### 3.2 Multiple Statements

```relang
fn process(x: Int): Int {
    let y = x * 2
    let z = y + 1
    z
}
```

The last line `z` is returned.

### 3.3 Conditional Return

```relang
fn max(a: Int, b: Int): Int {
    if a > b { a } else { b }
}
```

The `if` expression evaluates to either `a` or `b`, which becomes the return value.

### 3.4 Match Return

```relang
fn describe(n: Int): String {
    match n {
        0 => "zero"
        1 => "one"
        _ => "many"
    }
}
```

---

## 4. Early Return

Use `return` for early exit:

```relang
fn findFirst(list: [Int], predicate: (Int) -> Bool): Int? {
    for x in list {
        if predicate(x) {
            return Some(x)
        }
    }
    None
}
```

### 4.1 Guard Clauses

```relang
fn divide(a: Int, b: Int): Int | Failure {
    if b == 0 {
        return Failure("division by zero")
    }
    a / b
}
```

### 4.2 Return is Optional

`return` is only needed for early exit. The last expression is returned automatically:

```relang
// These are equivalent
fn double(x: Int): Int {
    return x * 2
}

fn double(x: Int): Int {
    x * 2
}
```

Prefer the implicit form unless early return is needed.

---

## 5. Expression Body (Single Expression)

When a function body is a single expression, replace `{ }` with `=`:

```relang
// Block body
fn double(x: Int): Int {
    x * 2
}

// Expression body (equivalent, preferred for one-liners)
fn double(x: Int): Int = x * 2
```

More examples:

```relang
fn add(a: Int, b: Int): Int = a + b
fn isPositive(n: Int): Bool = n > 0
fn greet(name: String): String = "Hello, ${name}!"
fn max(a: Int, b: Int): Int = if a > b { a } else { b }
```

**Rule**: If the body is one expression, use `=`. If it has multiple statements, use `{ }`.

---

## 6. Parameters

### 6.1 Required Parameters

All parameters must be provided:

```relang
fn greet(name: String, greeting: String): String {
    "${greeting}, ${name}!"
}

greet("Alice", "Hello")  // "Hello, Alice!"
```

### 6.2 Default Parameters

Parameters can have default values:

```relang
fn greet(name: String, greeting: String = "Hello"): String {
    "${greeting}, ${name}!"
}

greet("Alice")           // "Hello, Alice!"
greet("Alice", "Hi")     // "Hi, Alice!"
```

### 6.3 Named Arguments

Call with named arguments for clarity:

```relang
fn createUser(name: String, age: Int, active: Bool): User {
    User { name: name, age: age, active: active }
}

createUser(name: "Alice", age: 30, active: true)
createUser(age: 30, name: "Alice", active: true)  // order doesn't matter
```

### 6.4 Mixing Positional and Named

Positional arguments must come first:

```relang
createUser("Alice", age: 30, active: true)  // OK
createUser(name: "Alice", 30, true)          // Error
```

---

## 7. Return Types

### 7.1 Explicit Return Type

Always specify return types for public functions:

```relang
fn double(x: Int): Int {
    x * 2
}
```

### 7.2 Unit Return

Functions that don't return a meaningful value return `Unit`:

```relang
fn log(msg: String): Unit {
    print(msg)
}

// Unit can be omitted
fn log(msg: String) {
    print(msg)
}
```

### 7.3 Optional Return

```relang
fn find(list: [Int], value: Int): Int? {
    list.indexOf(value)
}
```

### 7.4 Union Return (for errors)

```relang
fn parse(s: String): Int | Failure {
    Int.parse(s) ?? Failure("invalid number")
}
```

---

## 8. Generic Functions

### 8.1 Type Parameters

```relang
fn identity<T>(x: T): T {
    x
}

identity(42)        // Int
identity("hello")   // String
```

### 8.2 Multiple Type Parameters

```relang
fn pair<A, B>(a: A, b: B): A & B {
    a & b
}

pair(1, "one")  // Int & String
```

### 8.3 Type Constraints

```relang
fn max<T: Comparable>(a: T, b: T): T {
    if a > b { a } else { b }
}
```

---

## 9. Higher-Order Functions

### 9.1 Function Parameters

```relang
fn apply(x: Int, f: (Int) -> Int): Int {
    f(x)
}

apply(5, { it * 2 })  // 10
```

### 9.2 Returning Functions

```relang
fn multiplier(factor: Int): (Int) -> Int {
    { x -> x * factor }
}

let triple = multiplier(3)
triple(4)  // 12
```

### 9.3 With Trailing Lambda

```relang
fn repeat(times: Int, action: () -> Unit) {
    for i in 0..<times {
        action()
    }
}

repeat(3) {
    print("hello")
}
```

---

## 10. Function Scope

### 10.1 Local Functions

Functions can be defined inside other functions:

```relang
fn outer(x: Int): Int {
    fn inner(y: Int): Int {
        y * 2
    }
    inner(x) + 1
}
```

### 10.2 Hoisting

Functions are hoisted — they can be called before their definition:

```relang
fn isEven(n: Int): Bool {
    if n == 0 { true } else { isOdd(n - 1) }
}

fn isOdd(n: Int): Bool {
    if n == 0 { false } else { isEven(n - 1) }
}
```

---

## 11. Recursion

### 11.1 Basic Recursion

```relang
fn factorial(n: Int): Int {
    if n <= 1 { 1 } else { n * factorial(n - 1) }
}
```

### 11.2 Tail Recursion

The compiler optimizes tail-recursive functions:

```relang
fn factorial(n: Int): Int {
    fn loop(n: Int, acc: Int): Int {
        if n <= 1 { acc } else { loop(n - 1, n * acc) }
    }
    loop(n, 1)
}
```

---

## 12. Comparison: Functions vs Lambdas

| Aspect | Function | Lambda |
|--------|----------|--------|
| Syntax | `fn name(x: Int): Int { x * 2 }` | `{ x -> x * 2 }` |
| Name | Named | Anonymous |
| Recursion | Yes | No |
| Hoisting | Yes | No |
| Closures | No (only parameters) | Yes (captures scope) |
| Return | Implicit (last expr) | Implicit (last expr) |
| Early return | `return` keyword | `return` keyword |

**Unified rule**: In both functions and lambdas, the last expression is the return value.

---

## 13. Summary

| Form | Example |
|------|---------|
| Basic function | `fn double(x: Int): Int { x * 2 }` |
| Expression body | `fn double(x: Int): Int = x * 2` |
| Multiple params | `fn add(a: Int, b: Int): Int { a + b }` |
| No params | `fn version(): String { "1.0" }` |
| No return | `fn log(msg: String) { print(msg) }` |
| Default params | `fn greet(name: String, msg: String = "Hi") { ... }` |
| Generic | `fn identity<T>(x: T): T { x }` |
| Higher-order | `fn apply(x: Int, f: (Int) -> Int): Int { f(x) }` |
| Early return | `if bad { return error }; result` |

---

## 14. Design Notes

### 14.1 Why Implicit Return?

- **Consistency**: Same rule for functions and lambdas
- **Conciseness**: Less boilerplate for expression-oriented code
- **Precedent**: Rust, Ruby, Scala all use this approach
- **Safety**: Return type annotation catches mismatches

### 14.2 Why Expression Body `=`?

- Mirrors mathematical notation: `f(x) = x²`
- Clear visual distinction from block body
- Eliminates braces for one-liners

### 14.3 Why Required Return Types?

Explicit return types:
- Document intent
- Enable better error messages
- Prevent accidental type changes
- Support IDE tooling

---

*End of specification*

# Syntax Patterns from Modern Languages

Established patterns from production languages for reference when designing ReLang syntax.

## Variable Declarations

### Pattern: `let` for Immutability

```rust
let x = 42          // Rust
let x = 42          // Swift, Kotlin
const x = 42        // JavaScript (block-scoped)
```

ReLang follows this pattern:
```relang
let x = 42
let x: Int = 42     // With explicit type
```

### Pattern: `var` or `mut` for Mutability

```swift
var x = 42          // Swift
var x = 42          // Kotlin
let mut x = 42      // Rust
```

## Function Declarations

### Pattern: Named Return Types

```rust
fn add(a: i32, b: i32) -> i32      // Rust
func add(a: Int, b: Int) -> Int    // Swift
fun add(a: Int, b: Int): Int       // Kotlin
```

ReLang uses:
```relang
fn add(a: Int, b: Int): Int
```

### Pattern: Expression Bodies

```kotlin
fun add(a: Int, b: Int) = a + b    // Kotlin
fn add(a: i32, b: i32) -> i32 { a + b }  // Rust (last expr)
```

## Type Annotations

### Pattern: Postfix Type Annotations

```typescript
let x: number = 42           // TypeScript
var x: Int = 42              // Swift
val x: Int = 42              // Kotlin
let x: i32 = 42              // Rust
```

Consistent: type follows identifier after colon.

### Pattern: Nullable/Optional Types

```kotlin
String?              // Kotlin (nullable)
String?              // Swift (optional)
Option<String>       // Rust
string | null        // TypeScript
```

ReLang uses union types:
```relang
String | None
```

## Pattern Matching

### Pattern: `match` Expression

```rust
match x {
    1 => "one",
    2 => "two",
    _ => "other",
}
```

```kotlin
when (x) {
    1 -> "one"
    2 -> "two"
    else -> "other"
}
```

ReLang:
```relang
match x {
  1 => "one"
  2 => "two"
  _ => "other"
}
```

### Pattern: Destructuring

```rust
let (a, b) = tuple;
let Point { x, y } = point;
```

```javascript
const [a, b] = array;
const { x, y } = object;
```

ReLang products:
```relang
let (a & b) = product
```

### Pattern: Type Discrimination

```typescript
if (result.kind === "success") {
  result.value  // TypeScript narrowing
}
```

```rust
match result {
    Ok(value) => ...
    Err(e) => ...
}
```

ReLang:
```relang
match result {
  v: Success => v.value
  e: Failure => e.error
}
```

## Error Handling

### Pattern: Result Types

```rust
Result<T, E>         // Rust
Result<T>            // Kotlin (success or failure)
Either<E, T>         // Functional languages
```

ReLang's approach:
```relang
T | Failure          // Union with single failure channel
```

### Pattern: Propagation Operator

```rust
let x = operation()?;   // Rust: propagate Err
```

```kotlin
val x = operation()!!   // Kotlin: throw on null
```

ReLang:
```relang
let x = (await operation())!   // Propagate Failure
```

### Pattern: Try-Catch Alternative

```rust
// Rust: No exceptions, use Result + match
match operation() {
    Ok(v) => ...
    Err(e) => ...
}
```

```go
result, err := operation()
if err != nil { return err }
```

ReLang:
```relang
match await operation() {
  v: T => ...
  f: Failure => ...
}
```

## Async/Await Patterns

### Pattern: Async Function Declaration

```javascript
async function fetch() { ... }
```

```rust
async fn fetch() -> T { ... }
```

```kotlin
suspend fun fetch(): T { ... }
```

ReLang marks return type, not function:
```relang
fn fetch(): *T { ... }   // Returns awaitable
```

### Pattern: Await Expression

```javascript
const result = await fetch();
```

```rust
let result = fetch().await;   // Postfix
```

ReLang:
```relang
let result = fetch().await()   // Method call
```

### Pattern: Concurrent Execution

```javascript
const [a, b] = await Promise.all([p1, p2]);
```

```kotlin
val (a, b) = awaitAll(d1, d2)
```

ReLang:
```relang
let (a & b) = (t1 and t2).await()
```

### Pattern: Racing

```javascript
const first = await Promise.race([p1, p2]);
```

ReLang:
```relang
let first = (t1 or t2).await()
```

## Control Flow

### Pattern: Expression-Oriented

```rust
let x = if cond { a } else { b };
let y = match v { ... };
```

```kotlin
val x = if (cond) a else b
val y = when (v) { ... }
```

Languages where `if`/`match` are expressions, not statements.

### Pattern: Early Return

```rust
fn process() -> Result<T, E> {
    let x = operation()?;  // Early return on Err
    Ok(x)
}
```

```go
if err != nil {
    return err
}
```

ReLang's `!` serves this purpose.

## Generics

### Pattern: Angle Brackets

```java
List<String>         // Java
Vec<String>          // Rust
Array<String>        // Swift, Kotlin
```

### Pattern: Constraints

```rust
fn process<T: Display>(x: T)
```

```kotlin
fun <T : Comparable<T>> sort(list: List<T>)
```

```swift
func process<T: Equatable>(_ x: T)
```

## Operator Precedence

### Common Precedence (high to low)

1. Postfix: `.`, `()`, `[]`, `?`, `!`
2. Unary: `-`, `!`, `*`, `&`
3. Multiplicative: `*`, `/`, `%`
4. Additive: `+`, `-`
5. Comparison: `<`, `>`, `<=`, `>=`
6. Equality: `==`, `!=`
7. Logical AND: `&&`
8. Logical OR: `||`
9. Assignment: `=`

### ReLang Coordination Operators

Consider precedence for `and`/`or`:
- Lower than comparison (so `t1 and t2` binds loosely)
- `and` higher than `or` (like `&&` vs `||`)

```relang
t1 and t2 or t3   // Parses as: (t1 and t2) or t3
```

## Sigil Usage

### Type-Level Sigils

```rust
&T       // Reference
*T       // Raw pointer
Box<T>   // Owned pointer
```

```typescript
T[]      // Array
T?       // Optional (in some contexts)
```

ReLang:
```relang
*T       // Awaitable
A & B    // Product
A | B    // Sum
```

### Expression-Level Sigils

```rust
&x       // Borrow
*x       // Dereference
x?       // Propagate error
```

```kotlin
x!!      // Assert non-null
x?.y     // Safe navigation
```

ReLang:
```relang
x!       // Propagate failure
```

## Module Patterns

### Pattern: Explicit Imports

```rust
use std::collections::HashMap;
```

```kotlin
import kotlin.collections.HashMap
```

```typescript
import { HashMap } from './collections';
```

### Pattern: Visibility Modifiers

```rust
pub fn public() {}
fn private() {}
pub(crate) fn crate_visible() {}
```

```kotlin
public fun public() {}
private fun private() {}
internal fun moduleVisible() {}
```

## Closure Syntax

### Pattern: Lightweight Closures

```rust
|x| x + 1
|x, y| x + y
```

```kotlin
{ x -> x + 1 }
{ x, y -> x + y }
```

```javascript
x => x + 1
(x, y) => x + y
```

```swift
{ x in x + 1 }
```

## String Interpolation

### Pattern: Template Strings

```kotlin
"Hello, $name"
"Result: ${1 + 2}"
```

```javascript
`Hello, ${name}`
```

```swift
"Hello, \(name)"
```

ReLang:
```relang
"Hello, ${name}"
```

# ReLang Equality Semantics

*How values are compared*

---

## 1. Overview

ReLang uses **structural equality** — values are compared by their content, not by identity or memory location.

**Core principle**: Two values are equal if they have the same type and the same content.

---

## 2. Equality Operators

| Operator | Meaning |
|----------|---------|
| `==` | Equal (structural) |
| `!=` | Not equal |

There is no reference equality operator. All equality is structural.

---

## 3. Primitive Equality

### 3.1 Same-Type Comparison

Primitives of the same type compare by value:

```relang
// Int
42 == 42            // true
42 == 43            // false

// Float
3.14 == 3.14        // true
3.14 == 3.15        // false

// Bool
true == true        // true
true == false       // false

// String
"hello" == "hello"  // true
"hello" == "Hello"  // false (case-sensitive)
"" == ""            // true

// Duration
5s == 5s            // true
5s == 5000ms        // true (same duration)
1h == 60m           // true

// Timestamp
t1 == t1            // true
t1 == t2            // depends on time values

// Bytes
b"\x00\x01" == b"\x00\x01"  // true
```

### 3.2 Cross-Type Comparison

**Different primitive types are never equal:**

```relang
42 == 42.0          // false (Int vs Float)
42 == "42"          // false (Int vs String)
1 == true           // false (Int vs Bool)
"true" == true      // false (String vs Bool)
```

**Rationale**: Implicit coercion leads to subtle bugs. If you want to compare across types, convert explicitly:

```relang
42 as Float == 42.0         // true (both Float)
42.toString() == "42"       // true (both String)
```

### 3.3 Float Special Cases

```relang
// NaN is not equal to anything, including itself
Float.nan == Float.nan      // false

// Use isNan() to check
Float.nan.isNan()           // true

// Positive and negative zero
0.0 == -0.0                 // true

// Infinity
Float.infinity == Float.infinity        // true
Float.negInfinity == Float.negInfinity  // true
Float.infinity == Float.negInfinity     // false
```

---

## 4. None and Unit Equality

```relang
// None
none == none        // true

// Unit
unit == unit        // true
() == ()            // true
unit == ()          // true (same value)
```

**None vs other types:**

```relang
none == 0           // false
none == ""          // false
none == false       // false
```

---

## 5. List Equality

Lists are equal if they have the same length and all elements are equal (in order):

```relang
[1, 2, 3] == [1, 2, 3]      // true
[1, 2, 3] == [1, 2]         // false (different length)
[1, 2, 3] == [1, 3, 2]      // false (different order)
[1, 2, 3] == [1, 2, 4]      // false (different element)
[] == []                     // true

// Nested lists
[[1, 2], [3]] == [[1, 2], [3]]  // true
```

**Element types must match:**

```relang
[1, 2] == ["1", "2"]        // false (Int vs String elements)
```

---

## 6. Json Equality

Json values are equal if they have the same structure and content:

```relang
// Primitives
(42 as Json) == (42 as Json)            // true
("hello" as Json) == ("hello" as Json)  // true

// Arrays
([1, 2] as Json) == ([1, 2] as Json)    // true

// Objects — key-value pairs must match
{"a": 1, "b": 2} == {"a": 1, "b": 2}    // true
{"a": 1, "b": 2} == {"b": 2, "a": 1}    // true (order doesn't matter for objects)
{"a": 1} == {"a": 1, "b": 2}            // false (different keys)

// Nested
{"user": {"name": "Alice"}} == {"user": {"name": "Alice"}}  // true
```

**Json vs typed values:**

```relang
(42 as Json) == 42          // false (Json vs Int)
```

To compare, convert to same type:

```relang
(42 as Json).int() == Some(42)      // true
data as! Int == 42                   // true (if data contains 42)
```

---

## 7. User-Defined Type Equality

User-defined types use structural equality — all fields must be equal:

```relang
type Point { x: Int, y: Int }

let p1 = Point { x: 1, y: 2 }
let p2 = Point { x: 1, y: 2 }
let p3 = Point { x: 1, y: 3 }

p1 == p2    // true (same field values)
p1 == p3    // false (y differs)
```

### 7.1 Nested Types

Equality is recursive:

```relang
type Rectangle { topLeft: Point, bottomRight: Point }

let r1 = Rectangle {
    topLeft: Point { x: 0, y: 0 },
    bottomRight: Point { x: 10, y: 10 }
}
let r2 = Rectangle {
    topLeft: Point { x: 0, y: 0 },
    bottomRight: Point { x: 10, y: 10 }
}

r1 == r2    // true (all nested fields equal)
```

### 7.2 Different Types Are Never Equal

Even with identical fields, different types are not equal:

```relang
type Point2D { x: Int, y: Int }
type Coordinate { x: Int, y: Int }

let p = Point2D { x: 1, y: 2 }
let c = Coordinate { x: 1, y: 2 }

p == c      // ✗ Compile error: cannot compare Point2D and Coordinate
```

### 7.3 Optional Fields

```relang
type User { name: String, email: String? }

let u1 = User { name: "Alice", email: "alice@example.com" }
let u2 = User { name: "Alice", email: "alice@example.com" }
let u3 = User { name: "Alice", email: none }
let u4 = User { name: "Alice", email: none }

u1 == u2    // true
u3 == u4    // true
u1 == u3    // false (email differs)
```

---

## 8. Sealed Type Equality

Sealed types compare by variant and content:

```relang
sealed Shape

type Circle : Shape { radius: Int }
type Square : Shape { side: Int }

let c1 = Circle { radius: 5 }
let c2 = Circle { radius: 5 }
let c3 = Circle { radius: 10 }
let s1 = Square { side: 5 }

c1 == c2    // true (same variant, same content)
c1 == c3    // false (same variant, different content)
c1 == s1    // false (different variants)
```

**Comparing as parent type:**

```relang
let shape1: Shape = Circle { radius: 5 }
let shape2: Shape = Circle { radius: 5 }
let shape3: Shape = Square { side: 5 }

shape1 == shape2    // true
shape1 == shape3    // false
```

---

## 9. Product Type Equality

Product types (`A & B`) compare component-wise:

```relang
let p1: Int & String = (42 & "hello")
let p2: Int & String = (42 & "hello")
let p3: Int & String = (42 & "world")

p1 == p2    // true
p1 == p3    // false
```

---

## 10. Sum Type Equality

Sum types (`A | B`) compare by variant and value:

```relang
let v1: Int | String = 42
let v2: Int | String = 42
let v3: Int | String = "42"

v1 == v2    // true (same variant, same value)
v1 == v3    // false (different variants)
```

---

## 11. Awaitable Equality

Awaitables cannot be compared for equality:

```relang
let t1: *User = db.getUser("123")
let t2: *User = db.getUser("123")

t1 == t2    // ✗ Compile error: cannot compare awaitables
```

**Rationale**: Awaitables represent in-flight operations, not values. Compare the results after awaiting:

```relang
let u1 = t1!
let u2 = t2!
u1 == u2    // ✓ OK: comparing User values
```

---

## 12. Failure Equality

Failures compare by error content:

```relang
let f1 = Failure { error: Timeout { duration: 5s } }
let f2 = Failure { error: Timeout { duration: 5s } }
let f3 = Failure { error: Timeout { duration: 10s } }

f1 == f2    // true
f1 == f3    // false
```

---

## 13. Equality in Pattern Matching

Equality is used implicitly in pattern matching with literal values:

```relang
match x {
    0 -> "zero"         // x == 0
    1 -> "one"          // x == 1
    _ -> "other"
}

match name {
    "Alice" -> "Hi Alice"
    "Bob" -> "Hi Bob"
    _ -> "Hello stranger"
}
```

---

## 14. Equality Summary

| Type | Equality basis |
|------|----------------|
| Primitives (same type) | Value equality |
| Primitives (different types) | Always false |
| `None` | Equal to `none` only |
| `Unit` | Always equal |
| `[T]` | Length + element equality (ordered) |
| `Json` | Structure + content (objects unordered) |
| User-defined types | All fields equal |
| Sealed types | Same variant + content |
| Product (`A & B`) | Component-wise |
| Sum (`A \| B`) | Same variant + value |
| Awaitables | Not comparable |
| `Failure` | Error content equality |

---

## 15. Design Rationale

### 15.1 Why No Reference Equality?

ReLang values are immutable data. There's no meaningful concept of "same object" vs "equal object" when values can't change. Structural equality is simpler and more predictable.

### 15.2 Why No Cross-Type Equality?

Implicit type coercion (like JavaScript's `==`) causes bugs:

```javascript
// JavaScript surprises
0 == ""         // true
0 == "0"        // true
"" == "0"       // false
```

ReLang avoids this by requiring same types. Convert explicitly if needed.

### 15.3 Why Can't Awaitables Be Compared?

Awaitables represent in-flight operations, not data. Two awaitables from the same call may:
- Resolve to different values (time-dependent)
- Have different identities
- Be at different stages of execution

Compare the resolved values instead.

---

*End of proposal*

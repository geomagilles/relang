# ReLang Operators

*Complete Operator Reference*

---

## 1. Overview

ReLang operators are divided into categories:

| Category | Purpose |
|----------|---------|
| Arithmetic | Numeric computation |
| Comparison | Value comparison |
| Logical | Boolean logic |
| Coordination | Awaitable composition |
| Data | Type construction |
| Access | Member and index access |
| Type | Type conversion and testing |
| Null-safety | Optional handling |
| Range | Sequence generation |

---

## 2. Arithmetic Operators

### 2.1 Binary Operators

| Operator | Description | Types | Example |
|----------|-------------|-------|---------|
| `+` | Addition | `Int`, `Float` | `3 + 2` → `5` |
| `-` | Subtraction | `Int`, `Float` | `5 - 2` → `3` |
| `*` | Multiplication | `Int`, `Float` | `3 * 4` → `12` |
| `/` | Division | `Int` (truncated), `Float` | `7 / 2` → `3` |
| `%` | Remainder | `Int` | `7 % 3` → `1` |

### 2.2 Unary Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `-` | Negation | `-5` |

### 2.3 String Concatenation

| Operator | Description | Example |
|----------|-------------|---------|
| `+` | Concatenation | `"a" + "b"` → `"ab"` |

### 2.4 Duration Arithmetic

| Operator | Description | Example |
|----------|-------------|---------|
| `+` | Add durations | `1h + 30m` → `90m` |
| `-` | Subtract durations | `1h - 15m` → `45m` |
| `*` | Scale duration | `5s * 3` → `15s` |
| `/` | Divide duration | `1m / 2` → `30s` |

### 2.5 Timestamp Arithmetic

| Operator | Description | Result |
|----------|-------------|--------|
| `t + d` | Add duration to timestamp | `Timestamp` |
| `t - d` | Subtract duration from timestamp | `Timestamp` |
| `t1 - t2` | Difference between timestamps | `Duration` |

---

## 3. Comparison Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `==` | Equal | `a == b` |
| `!=` | Not equal | `a != b` |
| `<` | Less than | `a < b` |
| `<=` | Less than or equal | `a <= b` |
| `>` | Greater than | `a > b` |
| `>=` | Greater than or equal | `a >= b` |

**Comparison rules:**
- Same-type comparison only (no implicit coercion)
- Structural equality for composite types
- See [Equality Semantics](relang-equality-proposal.md) for details

---

## 4. Logical Operators

### 4.1 Boolean Operators

| Operator | Description | Short-circuit |
|----------|-------------|---------------|
| `and` | Logical AND | Yes — `b` not evaluated if `a` is false |
| `or` | Logical OR | Yes — `b` not evaluated if `a` is true |
| `not` | Logical NOT | N/A |

```relang
let result = isValid and isActive
let fallback = primary or secondary
let inverted = not enabled
```

### 4.2 Type-Based Disambiguation

`and`/`or` behavior depends on operand types:

| Operands | Interpretation | Result |
|----------|----------------|--------|
| `Bool and Bool` | Logical AND | `Bool` |
| `Bool or Bool` | Logical OR | `Bool` |
| `*A and *B` | Coordination (all) | `*(A & B)` |
| `*A or *B` | Coordination (race) | `*(A \| B)` |

Mixed operand types are a compile error.

---

## 5. Coordination Operators

For awaitable types only.

| Operator | Description | Result Type |
|----------|-------------|-------------|
| `and` | All must succeed (fail-fast) | `*(A & B)` |
| `or` | First success wins (fail-last) | `*(A \| B)` |

```relang
let both: *(User & Orders) = getUser() and getOrders()
let any: *(Primary \| Fallback) = tryPrimary() or tryFallback()
```

See [ReLang v0.1 Canonique](relang-spec-v0.1-canonique.md) for full `await` semantics.

---

## 6. Data Operators

### 6.1 Product Type (`&`)

Constructs tuples/products:

```relang
let pair: Int & String = (42 & "hello")
let triple: Int & String & Bool = (1 & "a" & true)
```

### 6.2 Sum Type (`|`)

Declares alternatives (in types):

```relang
let value: Int | String = 42
let result: Success | Failure = getResult()
```

**Note**: `|` in type position declares alternatives. Values inhabit one variant.

---

## 7. Access Operators

### 7.1 Member Access (`.`)

```relang
user.name
user.address.city
list.length()
```

### 7.2 Index Access (`[]`)

```relang
list[0]             // first element
list[-1]            // last element
map["key"]          // key lookup
```

### 7.3 Safe Index Access (`[]?`)

Returns optional:

```relang
list[0]?            // first element or none
list[100]?          // none if out of bounds
```

### 7.4 Method Call (`()`)

```relang
str.toUpperCase()
list.map { x -> x * 2 }
await user
```

---

## 8. Type Operators

### 8.1 Type Conversion (`as`)

| Syntax | Result | On failure |
|--------|--------|------------|
| `x as T` (safe) | `T` | N/A |
| `x as T` (unsafe) | `T \| Failure` | Returns Failure |
| `x as! T` | `T` | Raises Failure |
| `x as? T` | `T?` | Returns none |

```relang
let f: Float = 42 as Float          // safe widening
let u: User | Failure = data as User // unsafe conversion
let u: User = data as! User          // force (raises on failure)
let u: User? = data as? User         // optional (none on failure)
```

### 8.2 Type Test (`is`)

Tests whether a value is of a specific type. Enables flow-sensitive type narrowing.

| Syntax | Result | Purpose |
|--------|--------|---------|
| `x is T` | `Bool` | Type test |
| `x is not T` | `Bool` | Negated type test |

#### Basic Usage

```relang
let value: Int | String = getData()

if (value is Int) {
    // value: Int in this branch
    print(value + 1)
} else {
    // value: String in this branch
    print(value.length())
}
```

#### Flow-Sensitive Narrowing

After an `is` check, the type is narrowed in the appropriate scope:

```relang
let r: Data | Failure = await task

if (r is Failure) {
    print("Failed: ${r.kind}")  // r: Failure
    return
}
// r: Data here (Failure eliminated)
print("Got: ${r}")
```

#### Negation

```relang
if (r is not Failure) {
    // r: Data
    use(r)
}
```

#### With Complex Unions

```relang
let role: User | Admin | Guest = getRole()

if (role is Admin) {
    role.adminPowers()  // role: Admin
} else {
    // role: User | Guest
}
```

### 8.3 Type Test (in patterns)

```relang
match value {
    i: Int -> "integer"
    s: String -> "string"
    _ -> "other"
}
```

---

## 9. Null-Safety Operators

### 9.1 Safe Navigation (`?.`)

```relang
user?.address?.city     // none if any step is none
```

### 9.2 Null Coalescing (`??`)

```relang
name ?? "Anonymous"     // default if none
```

### 9.3 Force Unwrap (`!`)

```relang
value!                  // propagates if none or Failure
await task!             // await and unwrap (special rule)
data as! User           // force conversion
```

**Special rule for `await`**: A trailing `!` after an `await` expression applies to the await result:

```relang
await expr!             // parsed as: (await expr)!
await http.get(url)!    // await, then unwrap
await (t1 and t2)!      // await coordination, then unwrap
```

This avoids requiring parentheses for the common "await and propagate" pattern.

---

## 10. Range Operators

### 10.1 Exclusive Range (`..`)

```relang
0..5        // 0, 1, 2, 3, 4
```

### 10.2 Inclusive Range (`..=`)

```relang
0..=5       // 0, 1, 2, 3, 4, 5
```

### 10.3 In Slicing

```relang
list[1..3]      // elements at index 1, 2
list[1..=3]     // elements at index 1, 2, 3
list[2..]       // from index 2 to end
list[..3]       // from start to index 2
```

---

## 11. Precedence Table

From highest to lowest precedence:

| Level | Operators | Associativity | Description |
|-------|-----------|---------------|-------------|
| 1 | `.` `[]` `()` | Left | Access |
| 2 | `!` | Postfix | Force unwrap |
| 3 | `-` (unary) `not` | Right | Unary |
| 4 | `as` `as!` `as?` | Left | Type conversion |
| 5 | `*` `/` `%` | Left | Multiplicative |
| 6 | `+` `-` | Left | Additive |
| 7 | `..` `..=` | Left | Range |
| 8 | `&` | Left | Product |
| 9 | `\|` | Left | Sum |
| 10 | `<` `<=` `>` `>=` | Left | Relational |
| 11 | `==` `!=` | Left | Equality |
| 12 | `is` `is not` | Left | Type test |
| 13 | `and` (bool) | Left | Logical AND |
| 14 | `or` (bool) | Left | Logical OR |
| 15 | `and` (await) | Left | Coordination AND |
| 16 | `or` (await) | Left | Coordination OR |
| 17 | `?.` | Left | Safe navigation |
| 18 | `??` | Right | Null coalescing |
| 19 | `await` | Right | Await resolution |

### 11.1 Precedence Examples

```relang
a + b * c           // a + (b * c)
a < b and c < d     // (a < b) and (c < d)
a ?? b ?? c         // a ?? (b ?? c)
t1 and t2 or t3     // (t1 and t2) or t3
x as Int + 1        // (x as Int) + 1
user?.name ?? "?"   // (user?.name) ?? "?"
await t1 or t2      // await (t1 or t2)
await http.get(u)!  // (await http.get(u))!
```

---

## 12. Operator Overloading

ReLang does **not** support user-defined operator overloading.

**Rationale:**
- Predictable semantics
- Simpler tooling
- Clearer error messages
- Consistent checkpoint serialization

Use named functions for custom operations:

```relang
// Instead of overloading +
fn addMoney(a: Money, b: Money): Money {
    Money { cents: a.cents + b.cents }
}
```

---

## 13. Summary by Category

### Arithmetic
`+` `-` `*` `/` `%` (unary `-`)

### Comparison
`==` `!=` `<` `<=` `>` `>=`

### Logical (Bool)
`and` `or` `not`

### Coordination (Awaitable)
`and` `or`

### Data Construction
`&` (product) `|` (sum, in types)

### Access
`.` `[]` `[]?` `()`

### Type
`as` `as!` `as?` `is` `is not`

### Null-Safety
`?.` `??` `!`

### Range
`..` `..=`

---

*End of proposal*

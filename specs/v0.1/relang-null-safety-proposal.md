# ReLang Null Safety

*Representing Absence of Value*

---

## 1. The Problem

Null references have caused countless bugs in programming languages. The core issue:

```java
// Java — any reference can be null
String name = user.getName();  // Could be null!
int len = name.length();       // NullPointerException at runtime
```

When any variable can be null, the type system provides no safety. The compiler cannot help.

---

## 2. ReLang's Solution

ReLang enforces null safety at compile time:

- **Non-optional types cannot be `none`** — `String` always contains a string
- **Optional types must be checked** — `String?` might be `none`, compiler enforces checking
- **No null pointer exceptions** — impossible by construction

---

## 3. User-Facing Syntax

### 3.1 The `none` Value

`none` represents absence of a value:

```relang
let x: String? = none           // no value
let y: String? = "hello"        // has value
```

### 3.2 Optional Types with `?`

`T?` means "T or no value":

```relang
let name: String? = user.nickname   // might be none
let age: Int = user.age             // never none
```

### 3.3 Checking for none

**Pattern matching:**
```relang
match name {
    s: String -> print("Name: ${s}")
    none -> print("No name")
}
```

**Comparison:**
```relang
if name != none {
    // name is String here (smart cast)
}
```

### 3.4 Safe Navigation

```relang
let len: Int? = user?.name?.length()    // none if any step is none
```

### 3.5 Default Values

```relang
let name: String = user.nickname ?? "Unknown"
```

### 3.6 Force Unwrap

```relang
let name: String = user.nickname!       // raises if none
```

### 3.7 The `!` Operator (Unified Semantics)

The postfix `!` operator has consistent semantics across ReLang:

```relang
// On optionals: unwrap or raise
let name: String = maybeString!         // raises if none

// On await results: unwrap success or propagate failure
let user: User = (await getUser())!     // raises if Failure

// On conversions: force conversion or raise
let user: User = data as! User          // raises if conversion fails
```

**Unified rule**: `x!` means "unwrap the success case or raise the failure case as a `Failure`".

---

## 4. Void Functions

Functions that don't return a meaningful value omit the return type:

```relang
fn logMessage(msg: String) {
    print(msg)
}
```

---

## 5. Non-Returning Functions

Functions that never return (always raise) also omit the return type:

```relang
fn fail(msg: String) {
    raise Failure { error: GenericError { message: msg } }
}

fn unreachable() {
    raise Failure { error: GenericError { message: "Should not reach here" } }
}
```

The compiler infers that these functions don't return, enabling proper type checking:

```relang
fn process(x: Int?): Int {
    match x {
        n: Int -> n
        none -> fail("Expected value")  // OK: compiler knows fail() doesn't return
    }
}
```

---

## 6. none vs Failure

These are distinct concepts:

| Concept | Meaning | Example |
|---------|---------|---------|
| `none` | Absence is expected | Cache miss, optional field |
| `Failure` | Operation failed | Database error, network timeout |

```relang
// none — absence is normal, not an error
let cached: User? = cache.get(id)       // none means "not cached"

// Failure — something went wrong
let result: User | Failure = await db.getUser(id)  // Failure means "DB error"
```

---

## 7. Internal Implementation

The following types exist internally but are not exposed to users:

| Internal Type | Purpose |
|---------------|---------|
| `None` (internal) | Runtime representation of `T?` |
| `Unit` | Return type for void functions |
| `Nothing` | Bottom type for non-returning functions |

Users never write these types directly — they use `none`, `T?`, and omit return types.

---

## 8. Summary

| Syntax | Meaning |
|--------|---------|
| `T` | Non-optional, never `none` |
| `T?` | Optional, may be `none` |
| `none` | Absence of value |
| `x?.y` | Safe navigation |
| `x ?? default` | Default if `none` |
| `x!` | Force unwrap |
| `fn f() { }` | Void or non-returning function |

**Guarantee:** If code compiles, there are no null pointer exceptions at runtime.

---

*End of specification*

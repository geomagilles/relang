# ReLang Modules

*Code Organization and Visibility*

---

## 1. Overview

ReLang uses a module system for organizing code into reusable units.

**Core principles:**
- One file = one module
- Explicit exports
- Qualified imports
- No circular dependencies

---

## 2. Module Structure

### 2.1 File as Module

Each `.relang` file is a module:

```
src/
├── main.relang           # module: main
├── orders.relang         # module: orders
├── users/
│   ├── types.relang      # module: users.types
│   └── service.relang    # module: users.service
└── utils/
    └── helpers.relang    # module: utils.helpers
```

### 2.2 Module Name

Module name is derived from file path:
- `src/orders.relang` → `orders`
- `src/users/types.relang` → `users.types`

---

## 3. Exports

### 3.1 Public by Default

Top-level declarations are public by default:

```relang
// orders.relang

type Order {                    // public
    id: String
    total: Int
}

fn calculateTotal(items: [Item]): Int {    // public
    items.map { it.price }.sum()
}

fn processOrder(orderId: String): Result {   // public
    // ...
}
```

### 3.2 Private Declarations

Use `private` for internal items:

```relang
// orders.relang

private const INTERNAL_RATE = 0.1

private fn internalHelper(x: Int): Int {
    x * 2
}

fn publicFunction(): Int {
    internalHelper(10)      // OK — same module
}
```

### 3.3 What Can Be Exported

- Types (`type`, `sealed`)
- Functions (`fn`)
- Functions (`fn`)
- Constants (`const`)
- Action families (`action family`)

---

## 4. Imports

### 4.1 Import Specific Items

```relang
import Order, OrderItem from orders
import User from users.types

let order = Order { id: "123", total: 100 }
```

### 4.2 Import All with Namespace

```relang
import * as Orders from orders

let order = Orders.Order { id: "123", total: 100 }
let total = Orders.calculateTotal(items)
```

### 4.3 Import All into Scope

```relang
import * from orders

let order = Order { id: "123", total: 100 }
```

**Use sparingly** — can cause name conflicts.

### 4.4 Aliased Import

```relang
import Order as CustomerOrder from orders
import User as CustomerUser from users.types

let order = CustomerOrder { id: "123", total: 100 }
```

---

## 5. Module Dependencies

### 5.1 No Circular Dependencies

```
// ✗ INVALID
orders.relang imports users.relang
users.relang imports orders.relang  // Circular!
```

### 5.2 Dependency Direction

Common pattern:
```
main.relang
    ├── orders.relang
    │       └── types.relang
    └── users.relang
            └── types.relang
```

Shared types in common module:
```
main.relang
    ├── orders.relang ──┐
    └── users.relang ───┴── common.types.relang
```

---

## 6. Package Structure

### 6.1 Package Definition

`package.relang` in project root:

```relang
package myapp {
    version: "1.0.0"
    main: "src/main.relang"

    dependencies: {
        "stdlib": "1.0"
        "http-client": "2.1"
    }
}
```

### 6.2 External Dependencies

```relang
import HttpClient from http-client
import Json from stdlib.json

let client = HttpClient.create()
```

---

## 7. Standard Library

### 7.1 Core Modules

| Module | Contents |
|--------|----------|
| `stdlib.json` | JSON parsing/serialization |
| `stdlib.text` | String utilities |
| `stdlib.time` | Duration, Timestamp helpers |
| `stdlib.collections` | Advanced collection functions |

### 7.2 Auto-Imported

These are available without import:
- Primitive types (`Int`, `Float`, `Bool`, `String`, etc.)
- Core types (`Failure`, `Unit`, `None`)
- Built-in functions (`print`, `assert`)

---

## 8. Visibility Rules

### 8.1 Access Levels

| Modifier | Visibility |
|----------|------------|
| (none) | Public — accessible from any module |
| `private` | Module-only — same file only |

### 8.2 Type Member Visibility

All type fields are public:

```relang
type User {
    id: String      // always accessible
    name: String    // always accessible
}
```

**Rationale**: Types are data containers. Encapsulation is at the module level, not type level.

---

## 9. Sealed Types Across Modules

### 9.1 Sealed in Same Module

```relang
// errors.relang

sealed AppError

type NotFound : AppError { resource: String }
type Unauthorized : AppError { reason: String }
type ServerError : AppError { message: String }
```

### 9.2 Cannot Extend Sealed Externally

```relang
// other.relang
import AppError from errors

// ✗ INVALID — cannot add variants to sealed from another module
type CustomError : AppError { code: Int }
```

**Rationale**: Sealed types enable exhaustive matching. External extension would break this guarantee.

---

## 10. Module Initialization

### 10.1 No Implicit Initialization

Modules do not have implicit initialization code:

```relang
// ✗ NOT ALLOWED — top-level expressions
print("Module loaded")
let x = computeSomething()

// ✓ OK — declarations only
const CONFIG = "value"
fn helper(): Int { 42 }
```

### 10.2 Workflow Entry Points

Execution starts from workflows, not module code:

```relang
// main.relang

fn main(): Unit {
    print("Application started")
    runApp()
}
```

---

## 11. Re-exports

### 11.1 Re-export from Another Module

```relang
// api.relang — public API surface

// Re-export specific items
export Order, OrderItem from orders
export User from users.types

// Re-export with alias
export processOrder as submitOrder from orders
```

### 11.2 Aggregating Module

```relang
// models.relang — aggregate all model types

export * from orders.types
export * from users.types
export * from products.types
```

Usage:
```relang
import Order, User, Product from models
```

---

## 12. Summary

| Syntax | Meaning |
|--------|---------|
| `import X from mod` | Import specific item |
| `import X, Y from mod` | Import multiple items |
| `import * from mod` | Import all into scope |
| `import * as M from mod` | Import all under namespace |
| `import X as Y from mod` | Import with alias |
| `private fn/type/const` | Module-private declaration |
| `export X from mod` | Re-export from another module |

**Key principles:**
- One file = one module
- Public by default, `private` for internal
- Explicit imports, no globals
- No circular dependencies
- Sealed types cannot be extended externally

---

*End of proposal*

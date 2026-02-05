# ReLang Type System Proposal

*User-Defined Types as Immutable Data Containers*

---

## 1. Overview

This proposal defines ReLang's approach to user-defined types with these design goals:

1. **Immutable data only** — Types are pure data containers, no methods, no behavior
2. **Flat hierarchy** — No inheritance, optional sealed grouping for exhaustive matching
3. **Multiple definition sources** — Explicit declarations, Protocol Buffers, OpenAPI, AsyncAPI

These constraints align with ReLang's nature as a **durable execution orchestrator**:
- Immutable data is trivially serializable (checkpoint-safe)
- Flat types have simple serialization semantics (no polymorphic dispatch)
- External schema support enables interoperability with action systems

---

## 2. Design Commitments (Normative)

### 2.1 Types Are Immutable Data Containers

```relang
type User {
  id: String
  name: String
  email: String
}
```

A type declaration defines:
- A named record structure
- A set of fields with types
- An implicit constructor requiring all fields
- Structural equality based on all fields

A type declaration does **not** define:
- Methods or member functions
- Mutable fields
- Inheritance relationships (except sealed grouping)

### 2.2 No Inheritance Hierarchy

Types do not inherit from other types:

```relang
// INVALID: No inheritance
type Admin : User { ... }   // ✗ Not supported

// VALID: Composition
type Admin {
  user: User
  permissions: [Permission]
}
```

**Rationale**: Inheritance complicates serialization, adds hidden coupling, and is unnecessary for data containers. Composition provides equivalent expressiveness without the complexity.

### 2.3 Sealed Groups for Sum Types

The **only** hierarchy mechanism is `sealed` for defining exhaustive alternatives:

```relang
sealed PaymentMethod

type CreditCard : PaymentMethod {
  number: String
  expiry: String
  cvv: String
}

type BankTransfer : PaymentMethod {
  accountNumber: String
  routingNumber: String
}

type Crypto : PaymentMethod {
  walletAddress: String
  currency: String
}
```

Sealed groups enable:
- Exhaustive pattern matching (compiler warns on missing cases)
- Type-safe sum types
- Clear action-family error contracts (as in current spec)

Sealed groups do **not** enable:
- Code sharing via inheritance
- Method override
- Polymorphic dispatch

---

## 3. Type Definition Sources

### 3.1 Explicit Declaration (Native Syntax)

```relang
// Simple type
type Point {
  x: Int
  y: Int
}

// Nested type
type Rectangle {
  topLeft: Point
  bottomRight: Point
}

// With optional field
type User {
  id: String
  name: String
  nickname: String?     // Optional (syntactic sugar for String | None)
}

// Sealed group
sealed Shape

type Circle : Shape {
  center: Point
  radius: Float
}

type Rectangle : Shape {
  topLeft: Point
  bottomRight: Point
}
```

### 3.2 Protocol Buffer Import

```relang
// Namespace import (all types under prefix)
import * as UserProtos from proto "user.proto"
// Use: UserProtos.User, UserProtos.Address

// Selective import (specific types, no prefix)
import User, Address from proto "user.proto"
// Use: User, Address

// Single type import
import User from proto "user.proto"
// Use: User

// Wildcard import (all types directly into scope)
import * from proto "user.proto"
// Use: User, Address, PhoneNumber (no prefix)
```

Proto mapping rules:

| Proto Construct | ReLang Equivalent |
|-----------------|-------------------|
| `message` | `type` |
| `enum` | `sealed` group with unit types |
| `oneof` | inline sum type (`A \| B \| C`) |
| `repeated` | `[T]` (list) |
| `map<K,V>` | `Map<K, V>` |
| `optional` | `T?` |
| nested message | nested type or qualified name |

Example proto:

```protobuf
// user.proto
syntax = "proto3";

message User {
  string id = 1;
  string name = 2;
  optional string email = 3;
  repeated Address addresses = 4;
}

message Address {
  string street = 1;
  string city = 2;
}

enum Status {
  UNKNOWN = 0;
  ACTIVE = 1;
  INACTIVE = 2;
}
```

Generated ReLang types:

```relang
// Automatically generated (conceptual)
type User {
  id: String
  name: String
  email: String?
  addresses: [Address]
}

type Address {
  street: String
  city: String
}

sealed Status
type Unknown : Status {}
type Active : Status {}
type Inactive : Status {}
```

### 3.3 OpenAPI Import

```relang
// Namespace import
import * as Api from openapi "api.yaml"
// Use: Api.Pet, Api.Order

// Selective import
import Pet, Order from openapi "api.yaml"
// Use: Pet, Order

// Single type
import Pet from openapi "api.yaml"

// Wildcard (all into scope)
import * from openapi "api.yaml"
```

OpenAPI mapping rules:

| OpenAPI Construct | ReLang Equivalent |
|-------------------|-------------------|
| `object` with properties | `type` |
| `enum` | `sealed` group with unit types |
| `oneOf` | sum type (`A \| B \| C`) |
| `allOf` | flattened type (all properties merged) |
| `anyOf` | sum type (like `oneOf`) |
| `array` | `[T]` |
| `additionalProperties` | `Map<String, V>` |
| `nullable: true` | `T?` |

Example OpenAPI:

```yaml
# api.yaml
components:
  schemas:
    Pet:
      type: object
      required: [id, name]
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        status:
          $ref: '#/components/schemas/PetStatus'

    PetStatus:
      type: string
      enum: [available, pending, sold]
```

Generated ReLang types:

```relang
// Automatically generated (conceptual)
type Pet {
  id: Int
  name: String
  status: PetStatus?
}

sealed PetStatus
type Available : PetStatus {}
type Pending : PetStatus {}
type Sold : PetStatus {}
```

### 3.4 AsyncAPI Import

```relang
// Namespace import
import * as Events from asyncapi "events.yaml"
// Use: Events.UserCreated, Events.OrderPlaced

// Selective import
import UserCreated, OrderPlaced from asyncapi "events.yaml"

// Wildcard
import * from asyncapi "events.yaml"
```

AsyncAPI mapping rules:

| AsyncAPI Construct | ReLang Equivalent |
|--------------------|-------------------|
| `message.payload` (object) | `type` |
| `message.payload` (with `oneOf`) | sum type (`A \| B \| C`) |
| `schemas` component | `type` |
| `enum` | `sealed` group with unit types |
| `array` | `[T]` |
| `additionalProperties` | `Map<String, V>` |
| `$ref` | resolved to referenced type |

Example AsyncAPI:

```yaml
# events.yaml
asyncapi: '2.6.0'
info:
  title: Order Events
  version: '1.0.0'

channels:
  orders/created:
    publish:
      message:
        $ref: '#/components/messages/OrderCreated'

components:
  messages:
    OrderCreated:
      payload:
        type: object
        required: [orderId, userId, items]
        properties:
          orderId:
            type: string
          userId:
            type: string
          items:
            type: array
            items:
              $ref: '#/components/schemas/OrderItem'
          status:
            $ref: '#/components/schemas/OrderStatus'

  schemas:
    OrderItem:
      type: object
      properties:
        productId:
          type: string
        quantity:
          type: integer

    OrderStatus:
      type: string
      enum: [pending, confirmed, shipped, delivered]
```

Generated ReLang types:

```relang
// Automatically generated (conceptual)
type OrderCreated {
  orderId: String
  userId: String
  items: [OrderItem]
  status: OrderStatus?
}

type OrderItem {
  productId: String
  quantity: Int
}

sealed OrderStatus
type Pending : OrderStatus {}
type Confirmed : OrderStatus {}
type Shipped : OrderStatus {}
type Delivered : OrderStatus {}
```

**Rationale**: AsyncAPI is the standard for event-driven architectures. Since ReLang orchestrates asynchronous workflows, importing message schemas from AsyncAPI enables type-safe event handling.

### 3.5 Import Syntax Summary

| Syntax | Meaning | Usage |
|--------|---------|-------|
| `import User from proto "..."` | Single type | `User` |
| `import User, Address from proto "..."` | Specific types | `User`, `Address` |
| `import * from proto "..."` | All types into scope | `User`, `Address`, etc. |
| `import * as P from proto "..."` | All under namespace | `P.User`, `P.Address` |

The same forms apply to `openapi` and `asyncapi` imports.

---

## 4. Type Construction and Access

### 4.1 Construction

Types are constructed using named arguments:

```relang
let user = User {
  id: "123"
  name: "Alice"
  email: "alice@example.com"
}
```

All required fields must be provided. Optional fields default to `None` if omitted:

```relang
let user = User {
  id: "123"
  name: "Alice"
  // email omitted, defaults to None
}
```

### 4.2 Field Access

Fields are accessed with dot notation:

```relang
let name = user.name
let email = user.email    // Type: String?
```

### 4.3 Copy-With (Functional Update)

Since types are immutable, updates produce new values:

```relang
let updatedUser = user with {
  name: "Bob"
  email: "bob@example.com"
}
```

The `with` expression:
- Creates a new instance
- Copies all fields from the original
- Overrides specified fields

### 4.4 Destructuring

Types can be destructured in pattern matching:

```relang
match user {
  User { name, email: Some(e) } => print("${name}: ${e}")
  User { name, email: None } => print("${name}: no email")
}
```

Or in let bindings:

```relang
let User { id, name, .. } = user    // .. ignores remaining fields
```

---

## 5. Interaction with Existing Features

### 5.1 Awaitable Results

Types are the natural result types for actions:

```relang
let t: *User = db.getUser("123")
let result = t.await()

match result {
  u: User => print("Found: ${u.name}")
  f: Failure => print("Error: ${f.error}")
}
```

### 5.2 Product Types (`&`)

User types compose with product types:

```relang
let t1: *User = db.getUser("123")
let t2: *Orders = db.getOrders("123")

let combined: *(User & Orders) = t1 and t2
let result = combined.await()

match result {
  (user & orders) => print("${user.name} has ${orders.items.length} orders")
  f: Failure => print("Error")
}
```

### 5.3 Sum Types (`|`)

User types compose with sum types and sealed groups:

```relang
// Ad-hoc sum
type Result = Success | Error

// Using sealed groups in pattern matching
fn processPayment(method: PaymentMethod): *Receipt {
  match method {
    cc: CreditCard => processCard(cc)
    bt: BankTransfer => processBank(bt)
    cr: Crypto => processCrypto(cr)
  }
}
```

### 5.4 Failure Payloads

Action families define sealed error types:

```relang
sealed DbError

type NotFound : DbError {
  table: String
  key: String
}

type ConnectionFailed : DbError {
  host: String
  port: Int
}

type Timeout : DbError {
  duration: Duration
}

type Cancelled : DbError {}
```

The contract guarantees that `await db.query(...)` returns `Result | Failure` where `Failure.error` is a `DbError`.

### 5.5 Serialization for Checkpoints

All user-defined types are automatically serializable because:
- They contain only immutable data
- No methods or closures
- No references to runtime resources
- Field types are recursively serializable

---

## 6. Primitive and Built-in Types

### 6.1 Primitive Types

| Type | Description | Example |
|------|-------------|---------|
| `Int` | 64-bit signed integer | `42` |
| `Float` | 64-bit floating point | `3.14` |
| `Bool` | Boolean | `true`, `false` |
| `String` | UTF-8 string | `"hello"` |
| `Bytes` | Byte array | `b"..."` |
| `Duration` | Time duration | `5s`, `100ms` |
| `Timestamp` | Point in time | `workflow.now()` |

### 6.2 Built-in Compound Types

| Type | Description | Construction |
|------|-------------|--------------|
| `[T]` | List of T | `[1, 2, 3]` |
| `Map<K, V>` | Key-value map | `{"a": 1, "b": 2}` |
| `T?` | Optional T | Sugar for `T \| None` |
| `A & B` | Product type | `(a & b)` |
| `A \| B` | Sum type | Via pattern matching |

### 6.3 Special Types

| Type | Description |
|------|-------------|
| `None` | Absence of value |
| `Unit` | Single value (like void) |
| `Failure` | Await failure container |

---

## 7. Type Compatibility and Conversions

### 7.1 Structural Compatibility

ReLang uses **nominal typing** for user-defined types:

```relang
type Point2D { x: Int, y: Int }
type Coordinate { x: Int, y: Int }

let p: Point2D = Point2D { x: 1, y: 2 }
let c: Coordinate = p    // ✗ Type error: Point2D ≠ Coordinate
```

Even with identical fields, types are distinct. Use explicit conversion:

```relang
let c: Coordinate = Coordinate { x: p.x, y: p.y }
```

### 7.2 Subtyping via Sealed Groups

Sealed subtypes are assignable to the parent:

```relang
let method: PaymentMethod = CreditCard { ... }    // ✓ OK
```

But not vice versa without pattern matching:

```relang
let cc: CreditCard = method    // ✗ Type error
```

### 7.3 Proto/OpenAPI Type Identity

Imported types are distinct from native types:

```relang
import * as P from proto "user.proto"

type User { id: String, name: String }

let protoUser: P.User = ...
let nativeUser: User = protoUser    // ✗ Type error
```

Explicit conversion required:

```relang
let nativeUser = User { id: protoUser.id, name: protoUser.name }
```

---

## 8. Grammar (Informative)

```ebnf
type_decl       = "type" IDENT type_extends? "{" field_list "}"
type_extends    = ":" IDENT
sealed_decl     = "sealed" IDENT

field_list      = (field ("," field)* ","?)?
field           = IDENT ":" type_expr

type_expr       = nullable_type
nullable_type   = base_type "?"?
base_type       = IDENT type_args?
                | list_type
                | map_type
                | "(" type_expr ")"
                | product_type
                | sum_type

type_args       = "<" type_expr ("," type_expr)* ">"
list_type       = "[" type_expr "]"
map_type        = "Map" "<" type_expr "," type_expr ">"
product_type    = type_expr "&" type_expr
sum_type        = type_expr "|" type_expr

import_decl     = "import" import_spec "from" import_kind STRING
import_spec     = "*"                              // wildcard into scope
                | "*" "as" IDENT                   // wildcard into namespace
                | ident_list                       // selective
ident_list      = IDENT ("," IDENT)*
import_kind     = "proto" | "openapi" | "asyncapi"
```

---

## 9. Consistency Analysis

### 9.1 Impact on Existing Spec

| Area | Impact | Resolution |
|------|--------|------------|
| Failure model | Validates sealed groups | Already uses sealed HttpError pattern |
| Product types | User types compose with `&` | Natural fit |
| Sum types | User types compose with `\|` | Natural fit |
| Await results | User types as T in `T \| Failure` | Natural fit |
| Serialization | All types serializable by construction | Satisfies checkpoint requirement |

### 9.2 New Considerations

| Consideration | Status |
|---------------|--------|
| Generic types | Deferred (future proposal) |
| Type aliases | Not included (simple enough to omit) |
| Visibility modifiers | Not included (all types public for now) |
| Nested type definitions | Supported via import namespacing |

### 9.3 Ripple Effects

1. **Pattern matching**: Must support destructuring user types — addressed in Section 4.4
2. **Error families**: Sealed groups enable current error model — validates existing design
3. **Serialization**: All fields must be serializable types — constraint is compositional
4. **Proto/OpenAPI**: Import creates new namespace — no collision with native types

---

## 10. Design Rationale

### 10.1 Why Immutable Only?

1. **Checkpoint safety**: Mutable state requires tracking mutation points
2. **Simplicity**: No need for clone/copy semantics
3. **Predictability**: Values don't change unexpectedly
4. **Concurrency**: No synchronization needed (though ReLang is single-threaded)

### 10.2 Why No Methods?

1. **Separation of concerns**: Types are data, functions are behavior
2. **Serialization**: Methods can't be serialized
3. **Simplicity**: No dispatch, no virtual tables
4. **Interoperability**: Proto/OpenAPI don't have methods

Functions operate on types via free functions:

```relang
fn fullName(user: User): String {
  "${user.firstName} ${user.lastName}"
}
```

### 10.3 Why Sealed Groups vs Full Inheritance?

1. **Exhaustiveness**: Sealed groups enable compiler warnings for missing cases
2. **Simplicity**: No diamond problem, no fragile base class
3. **Serialization**: No polymorphic dispatch to serialize
4. **Error modeling**: Perfect fit for action-family errors

### 10.4 Why Support Proto/OpenAPI/AsyncAPI?

1. **Ecosystem integration**: Most action systems use these formats
2. **Type safety**: Schema → types → compile-time checks
3. **Evolution**: Schema evolution rules are well-understood
4. **Tooling**: Extensive tooling already exists
5. **Event-driven**: AsyncAPI covers message/event schemas for async workflows

---

## 11. Examples

### 11.1 E-Commerce Order Processing

```relang
import * as Order from proto "order.proto"

sealed PaymentResult

type PaymentSuccess : PaymentResult {
  transactionId: String
  amount: Int
}

type PaymentFailed : PaymentResult {
  reason: String
  code: Int
}

fn processOrder(order: Order.Order): *PaymentResult {
  let payment = order.payment

  match payment.method {
    Order.CreditCard { number, expiry, cvv } =>
      chargeCard(number, expiry, cvv, order.total)

    Order.BankTransfer { account } =>
      initiateTransfer(account, order.total)
  }
}
```

### 11.2 Multi-Source Data Aggregation

```relang
import * as Users from openapi "users-api.yaml"
import * as Orders from openapi "orders-api.yaml"

type UserSummary {
  user: Users.User
  recentOrders: [Orders.Order]
  totalSpent: Int
}

fn getUserSummary(userId: String): *UserSummary {
  let userTask: *Users.User = users.getUser(userId)
  let ordersTask: *[Orders.Order] = orders.getByUser(userId)

  let result = (userTask and ordersTask).await()

  match result {
    (user & orders) => {
      let total = orders.map(|o| o.amount).sum()
      UserSummary { user, recentOrders: orders, totalSpent: total }
    }
    f: Failure => raise f
  }
}
```

### 11.3 Event-Driven Workflow

```relang
import OrderCreated, OrderItem from asyncapi "events.yaml"
import * as Inventory from proto "inventory.proto"

fn handleOrderCreated(event: OrderCreated): *Unit {
  // Check inventory for all items in parallel
  let checks = event.items.map(|item|
    inventory.checkStock(item.productId, item.quantity)
  )

  let results = and(checks).await()

  match results {
    stocks: [Inventory.StockStatus] => {
      let allAvailable = stocks.all(|s| s.available)
      if allAvailable {
        confirmOrder(event.orderId)
      } else {
        rejectOrder(event.orderId, "Insufficient stock")
      }
    }
    f: Failure => notifyFailure(event.orderId, f)
  }
}
```

### 11.4 Error Handling with Sealed Types

```relang
sealed ApiError

type NotFound : ApiError { resource: String, id: String }
type Unauthorized : ApiError { reason: String }
type RateLimited : ApiError { retryAfter: Duration }
type ServerError : ApiError { message: String }

fn handleApiError(error: ApiError): String {
  match error {
    NotFound { resource, id } => "Could not find ${resource} with id ${id}"
    Unauthorized { reason } => "Access denied: ${reason}"
    RateLimited { retryAfter } => "Too many requests, retry in ${retryAfter}"
    ServerError { message } => "Server error: ${message}"
  }
}
```

---

## 12. Open Questions

### 12.1 Generic Types

Should ReLang support parameterized types?

```relang
type Result<T, E> {
  // ???
}
```

**Recommendation**: Defer to future proposal. Current design works without generics.

### 12.2 Default Field Values

Should fields support defaults?

```relang
type Config {
  timeout: Duration = 30s    // ???
  retries: Int = 3
}
```

**Recommendation**: Support for proto3 compatibility. Optional fields implicitly default to `None`.

### 12.3 Field Validation

Should types support invariants?

```relang
type PositiveInt {
  value: Int
  // invariant: value > 0   // ???
}
```

**Recommendation**: No. Validation belongs in functions, not type definitions. Keep types as pure data.

### 12.4 Visibility

Should types/fields have visibility modifiers?

```relang
pub type PublicUser { ... }
type InternalUser { ... }
```

**Recommendation**: Defer until module system is designed. For now, all types are public.

---

## 13. Summary

| Aspect | Decision |
|--------|----------|
| Mutability | Immutable only |
| Hierarchy | Flat, sealed groups only |
| Methods | None (use free functions) |
| Definition | Native syntax, proto, OpenAPI, AsyncAPI |
| Serialization | Automatic (by construction) |
| Generics | Deferred |
| Visibility | All public (deferred) |

This design prioritizes:
- **Simplicity**: Types are just data
- **Serializability**: Everything checkpointable
- **Interoperability**: Industry-standard schema support
- **Safety**: Exhaustive pattern matching via sealed groups

---

*End of proposal*

# ReLang Actions

*Side Effects and External Operations*

---

## 1. Overview

**Actions** are the boundary between ReLang's deterministic workflow execution and the non-deterministic external world.

Actions:
- Perform side effects (I/O, network, database)
- Return awaitables
- Persist resolved results in execution snapshots
- Belong to **action families** with defined error types

---

## 2. Design Philosophy

### 2.1 The Determinism Boundary

```
┌─────────────────────────────────────┐
│          WORKFLOW (deterministic)   │
│                                     │
│   let user = await db.getUser(id)   │
│              │                      │
│              ▼                      │
│   ┌─────────────────────┐           │
│   │  ACTION (boundary)  │──────────►│ External World
│   └─────────────────────┘           │ (non-deterministic)
│              │                      │
│              ▼                      │
│   Resolved result persisted in      │
│   durable execution state           │
│                                     │
└─────────────────────────────────────┘
```

### 2.2 Core Principles

1. **All I/O through actions**: No hidden network calls
2. **Actions are durable**: Resume reuses already resolved results
3. **Actions have identity**: For deduplication
4. **Actions define errors**: Sealed error families

---

## 3. Action Families

### 3.1 What Is an Action Family?

An action family is a category of related actions with:
- A common namespace
- A shared error type
- Consistent behavior patterns

### 3.2 Built-in Action Families

| Family | Purpose | Error Type | Specification |
|--------|---------|------------|---------------|
| `http` | HTTP requests | `HttpError` | [action-http.md](action-http.md) |
| `grpc` | gRPC calls | `GrpcError` | [action-grpc.md](action-grpc.md) |
| `openapi` | OpenAPI service clients | `OpenApiError` | [action-openapi.md](action-openapi.md) |
| `shell` | Shell command execution | `ShellError` | [action-shell.md](action-shell.md) |
| `container` | Container operations | `ContainerError` | [action-container.md](action-container.md) |

Each family has a detailed specification document covering API, error types, options, and examples.

### 3.3 Family-Specific Errors

```relang
sealed HttpError
type Timeout : HttpError { duration: Duration }
type DnsFailure : HttpError { host: String }
type ConnectionRefused : HttpError { host: String, port: Int }
type HttpStatus : HttpError { status: Int, body: Bytes? }
type Cancelled : HttpError {}

sealed DbError
type NotFound : DbError { table: String, key: String }
type ConstraintViolation : DbError { constraint: String }
type ConnectionFailed : DbError { host: String }
type QueryTimeout : DbError { duration: Duration }
type Cancelled : DbError {}
```

---

## 4. Using Actions

### 4.1 Action Invocation

```relang
let task: *HttpResponse = http.get("https://api.example.com/users")
let result: HttpResponse | Failure = await task
```

### 4.2 Common Patterns

```relang
// HTTP
let resp = (await http.get(url))!
let resp = (await http.post(url, body))!
let resp = (await http.put(url, body))!
let resp = (await http.delete(url))!

// Database
let user = (await db.query("SELECT * FROM users WHERE id = ?", [id]))!
let rows = (await db.execute("UPDATE users SET name = ? WHERE id = ?", [name, id]))!

// gRPC
let result = (await grpc.call(service, method, request))!

// Queue
let msg = (await queue.receive("orders"))!
(await queue.send("notifications", message))!

// Storage
let data = (await storage.read("bucket", "key"))!
(await storage.write("bucket", "key", data))!
```

---

## 5. Action Definition

### 5.1 Declaring an Action Family

```relang
action family payments {
    errors: PaymentError

    fn charge(card: CardInfo, amount: Money): *ChargeResult
    fn refund(chargeId: String, amount: Money?): *RefundResult
    fn getCharge(chargeId: String): *Charge
}
```

### 5.2 Error Type Declaration

```relang
sealed PaymentError
type CardDeclined : PaymentError { reason: String }
type InsufficientFunds : PaymentError { available: Money, required: Money }
type InvalidCard : PaymentError { field: String }
type ProcessorError : PaymentError { code: String, message: String }
type Cancelled : PaymentError {}
```

### 5.3 Action Implementation

Actions are implemented by the runtime, not in ReLang code:

```relang
// Declaration only — implementation is external
action family email {
    errors: EmailError

    fn send(to: String, subject: String, body: String): *SendResult
    fn sendTemplate(to: String, template: String, data: Json): *SendResult
}
```

---

## 6. Action Properties

### 6.1 Action Identity

Every action invocation has a unique identity:

```relang
let task = http.get(url)
task.id         // unique identifier
task.createdAt  // when action was created
```

### 6.2 Action Status

```relang
task.isResolved()   // Bool — has completed (success or failure)
task.isSucceeded()  // Bool — completed successfully
task.isFailed()     // Bool — completed with failure
```

### 6.3 Action Result (after resolution)

```relang
task.data()         // T? — success value if succeeded
task.failure()      // Failure? — failure if failed
task.resolvedAt()   // Timestamp? — when resolved
```

---

## 7. Action Options

### 7.1 Timeout Pattern (v0.1 Core)

```relang
let request = http.get(url)
let winner = await (request or timer(30s))

match winner {
    r: HttpResponse -> handle(r)
    _ -> timeoutExceeded()
}
```

### 7.2 Retry (Runtime Policy, Not Core Syntax)

```relang
let policy = RetryPolicy {
    maxAttempts: 3,
    initialDelay: 1s,
    backoff: Exponential
}

// Bound by runtime/policy configuration for this action
let task = http.get(url)
```

### 7.3 Headers and Options

```relang
let task = http.get(url, headers: {
    "Authorization": "Bearer ${token}",
    "Accept": "application/json"
})

let task = http.post(url, body, contentType: "application/json")
```

---

## 8. Action Results

### 8.1 Await Returns Union

```relang
let result: HttpResponse | Failure = await http.get(url)
```

### 8.2 Handling Results

```relang
match result {
    resp: HttpResponse -> {
        if resp.status == 200 {
            process(resp.body)
        } else {
            handleError(resp.status)
        }
    }
    f: Failure -> match f.kind {
        af: ActionFailed -> match af.error {
            t: Timeout -> retryLater()
            d: DnsFailure -> reportDnsIssue(d.host)
            _ -> propagateError(f)
        }
        _ -> propagateError(f)
    }
}
```

### 8.3 Force Unwrap

```relang
let resp = (await http.get(url))!   // propagates Failure on error
```

---

## 9. Durability of Action Results

### 9.1 Resume Semantics

After resume, completed actions reuse persisted resolved results:

```relang
fn example(): Result {
    let a = (await http.get(url1))!   // First run: executes
                                       // After resume: already resolved
    let b = (await http.get(url2))!   // Same behavior
    combine(a, b)
}
```

### 9.2 What Is Memoized

- Success values
- Failure values
- Resolution timestamp

### 9.3 Idempotency Keys

For explicit deduplication:

```relang
let task = http.post(url, body, idempotencyKey: "order-${orderId}")
```

Same idempotency key → same result (even across workflows).

---

## 10. Cancellation

### 10.1 Action Cancellation

```relang
let task = http.get(url)
cancel(task)    // request cancellation
```

### 10.2 Cancellation Result

If cancellation succeeds before completion:

```relang
match await task {
    r: HttpResponse -> // completed before cancel
    f: Failure -> match f.kind {
        af: ActionFailed -> match af.error {
            c: Cancelled -> // cancellation succeeded
            _ -> // other action error
        }
        _ -> // other failure
    }
}
```

### 10.3 Shielding Actions

```relang
let protected = shield(criticalAction())
// protected won't be cancelled by parent cancellation
```

---

## 11. Coordination with Actions

### 11.1 Parallel Actions

```relang
let t1 = http.get(url1)
let t2 = http.get(url2)
let t3 = http.get(url3)

let (r1 & r2 & r3) = (await (t1 and t2 and t3))!
```

### 11.2 Racing Actions

```relang
let primary = http.get(primaryUrl)
let fallback = http.get(fallbackUrl)

let result = (await (primary or fallback))!
```

### 11.3 List of Actions

```relang
let urls = ["url1", "url2", "url3"]
let tasks = urls.map { url -> http.get(url) }
let results = (await and(tasks))!
```

---

## 12. Custom Action Families

### 12.1 Declaration Syntax

```relang
action family inventory {
    errors: InventoryError

    fn checkStock(productId: String): *StockInfo
    fn reserve(productId: String, quantity: Int): *Reservation
    fn release(reservationId: String): *Unit
}
```

### 12.2 Error Declaration

```relang
sealed InventoryError
type OutOfStock : InventoryError { productId: String, available: Int }
type ProductNotFound : InventoryError { productId: String }
type ReservationExpired : InventoryError { reservationId: String }
type Cancelled : InventoryError {}
```

### 12.3 Usage

```relang
fn orderItem(productId: String, quantity: Int): OrderResult {
    let stock = (await inventory.checkStock(productId))!

    if stock.available < quantity {
        return OrderResult.outOfStock(stock.available)
    }

    let reservation = (await inventory.reserve(productId, quantity))!
    processOrder(reservation)
}
```

---

## 13. Action Constraints

### 13.1 Actions Cannot

- Be called from pure functions
- Be called synchronously (must await)
- Contain other await calls
- Return non-serializable types

### 13.2 Actions Must

- Belong to an action family
- Have serializable parameters
- Have serializable results
- Define all possible error types

---

## 14. Summary

| Concept | Description |
|---------|-------------|
| Action | External operation returning `*T` |
| Action Family | Group of related actions with shared errors |
| Await | Resolve action to `T \| Failure` |
| Durability | Completed actions reused after resume |
| Cancellation | Best-effort request to stop action |

**Key principles:**
- All I/O through actions (no hidden calls)
- Actions persist resolved outcomes for resume
- Each family defines sealed error types
- Actions have identity for deduplication
- Cancellation is best-effort, observed at resolution

---

*End of proposal*

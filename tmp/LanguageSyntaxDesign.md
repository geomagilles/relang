# Relang: A Modern Workflow Orchestration DSL – Syntax Design

## 1. Design Principles

### 1.1 Core Language Principles

Relang is not a general-purpose programming language. Its aim is to provide a efficient and reliable way to process services that are inherently brittle. A typical use case could be the orchestration of distributed services, including AI agents, in a way that is resilient to failures, outages, and interruptions:

- **Resumable**: Relang explicitly supports resumable execution. Before any side-effect, the runtime can capture the complete internal state of the workflow, allowing it to resume from that exact point after failures, restarts, or outages.

- **First-Class Concurrency**: Concurrency is built-in. You can invoke multiple activities in parallel and coordinate agents using high-level constructs (`and`/`or` composition, channels) without low-level thread management. Those activities being run in different services or locations is a first-class concern.

- **AI-oriented**: Relang includes built-in primitives for defining, spawning, and coordinating AI agents. Multi-agent workflows are as natural to express as sequential code, including inter-agent communication patterns.

### 1.2 LLM-Ready Design

Relang is designed for the AI era. This manifests in three ways:

#### Syntax Designed for LLM Generation

The syntax is designed so LLMs can reliably read and write Relang code:

- **Consistent structure**: Every block uses `{}` braces—workflows, agents, conditionals, loops. No special cases.
- **Explicit side-effects**: All operations that leave the workflow return `Deferred<T>` aka a future result. Applying an `await()` method will wait for the result. An LLM can see at a glance which operations are side-effects.
- **Self-documenting keywords**: `send`, `ask`, `broadcast`, `.await()` describe what they do.
- **Minimal punctuation**: No semicolons, no unnecessary symbols. Code reads like a description of what it does.
- **JSON-compatible literals**: Data structures use familiar JSON-like syntax.
- **Predictable patterns**: One idiomatic way to express parallel calls (`and`), first-success (`or`), error handling (`match`).

#### Code Generation Primitives

Relang includes primitives to call LLMs and generate code as part of workflow execution:

```
// Generate code dynamically
let code = llm.chat(
    prompt: "Write Python to parse CSV with headers: " + headers
).await()

// Execute generated code in sandbox
let result = python.code(code, inputData).await()
```

This enables dynamic transformations, adaptive logic, and code synthesis at runtime—all within the resumable execution model.

#### AI Agent Primitives

Relang provides first-class constructs for defining and orchestrating AI agents:

```
agent FraudAnalyst {
    system_prompt: "You are a Fraud Analyst AI..."
    tools: [ DatabaseLookup, WebSearch ]
    memory: last_n_messages(10)
}

// Spawn, communicate, compose
let agent = agent.spawn(FraudAnalyst, context: data).await()
let result = ask(agent, query).await()
```

Agents integrate with the same `Deferred` model as other operations—they're resumable, composable, and can run in parallel.

## 2. Core Concepts

Relang is built around three foundational capabilities that distinguish it from general-purpose languages and traditional workflow engines:

1. **Resumable Execution**: Relang captures its complete internal state before any side-effect, enabling workflows to resume from the exact point of failure—even after crashes, restarts, or infrastructure outages.

2. **Distributed Orchestration**: Relang provides native primitives for orchestrating distributed services. Asynchronous remote calls (HTTP, gRPC, scripts, etc.) are first-class citizens with built-in support for parallelism, composition, and fault tolerance.

3. **AI-Native**: Relang includes built-in primitives for defining, spawning, and coordinating AI agents—making multi-agent workflows as natural to express as sequential code.

These three pillars work together: AI agents and distributed services are orchestrated through the same resumable execution model, ensuring that complex workflows remain reliable and recoverable.

### 2.1 Resumable Execution

Relang workflows are inherently resumable. Before executing any side-effect (HTTP call, gRPC request, script execution, etc.), the runtime can output the workflow's complete internal state. This state can be provided back as input to resume execution from exactly where it left off.

#### How It Works

Relang operates as a pure state machine. Given a program and an optional input state, it executes until the next side-effect, then outputs its state:

```bash
# First run - executes until first side-effect, outputs state
relang program.re --state-out state.json

# Resume from checkpoint, execute to next side-effect
relang program.re --state-in state.json --state-out state.json
```

The runtime doesn't execute side-effects directly—it returns a description of what side-effect to perform. An external executor performs the actual call, captures the result, and feeds it back on the next invocation. This architecture means:

- **The workflow is always resumable**: State is externalized at every side-effect boundary
- **Execution is deterministic**: Given the same state, the workflow always produces the same next step
- **Recovery is trivial**: Restart from the last persisted state after any failure

#### Why This Matters

Consider this workflow:

```
workflow ProcessOrder(order: Order) -> Receipt | OrderError {
    let validated = validateOrder(order)           // pure - no state change
    let inventory = InventoryService.check(order.items).await()  // checkpoint A
    let payment = PaymentService.charge(order.total).await()     // checkpoint B
    let shipment = ShippingService.schedule(order).await()       // checkpoint C
    Receipt { ... }
}
```

If `PaymentService.charge()` fails due to an infrastructure error (service timeout, crash, etc.):

1. **State preserved**: The workflow state after checkpoint A is already persisted
2. **Resume available**: Re-run with `--state-in` to retry from checkpoint B
3. **No repetition**: The inventory check is not re-executed—its result is in the state

This enables:

- **Operational resilience**: Fix infrastructure issues and resume without data loss
- **Cost efficiency**: Expensive API calls and computations are not repeated
- **Simple deployment**: The runtime is stateless; state lives in your storage

### 2.2 Distributed Orchestration {#concurrency-model}

Relang is designed to orchestrate distributed services. Rather than treating remote calls as an afterthought, Relang provides native primitives for asynchronous operations, parallel execution, and result composition.

#### The Deferred Model

All side-effecting operations return `Deferred<T>` immediately—they don't block. The result type `T` is a union representing all possible outcomes (success types and business error types):

```
// Activity call returns Deferred immediately (no blocking)
let deferred: Deferred<User | NotFound | ApiError> = http.get(userUrl)

// .await() blocks, creates a checkpoint, and returns the result
let result: User | NotFound | ApiError = deferred.await()
```

#### Native Primitives

Relang provides built-in support for common distributed operations:

- **HTTP calls**: `http.get()`, `http.post()`, `http.put()`, `http.delete()`
- **gRPC calls**: `Service.Method(request)`
- **Script execution**: `python.code()`, `python.file()`, `bash.code()`, `bash.file()`
- **Sub-workflows**: `OtherWorkflow(input)`
- **Timers**: `timer.sleep(duration)`

#### Parallel Execution

Because `Deferred` values are first-class, parallelism emerges naturally:

```
// Start both activities concurrently (no blocking yet)
let d1 = http.get(urlA)
let d2 = http.get(urlB)

// Single checkpoint - wait for both to complete
let (a, b) = (d1 and d2).await()
```

#### Composition Operators

| Expression          | Semantics                        | Success Type          | Error Type   |
|---------------------|----------------------------------|-----------------------|--------------|
| `d.await()`         | Wait for single deferred         | `T`                   | `E`          |
| `(a and b).await()` | Wait for ALL (first error fails) | `(T, T)` or `List<T>` | `E`          |
| `(a or b).await()`  | Wait for FIRST success           | `T`                   | `List<E>`    |
| `list.await()`      | Wait for all in list             | `List<T>`             | `E`          |

The `and`/`or` operators provide intuitive semantics:
- `and` = "I need all of these"
- `or` = "I need any of these (first success wins)"

### 2.3 AI-Native

Relang treats AI agents as first-class constructs. You can define, spawn, and coordinate agents using built-in primitives—no external frameworks required.

#### Agents as First-Class Entities

An agent in Relang has:
- A unique identity
- Isolated state (conversation history, intermediate results)
- A set of tools it can use
- A system prompt defining its behavior

```
agent FraudAnalystAgent {
    system_prompt: """
        You are a Fraud Analyst AI. Evaluate transactions for fraud risk.
        Respond with: APPROVE, REJECT, or UNCLEAR, with reasoning.
    """
    tools: [ DatabaseLookupTool, WebSearchTool ]
    memory: last_n_messages(10)
}
```

#### Agent Operations

Agent operations integrate seamlessly with the deferred model:

```
// Spawn an agent instance
let fraudAgent = agent.spawn(FraudAnalystAgent, context: transactionDetails).await()

// Request-response communication
let assessment = ask(fraudAgent, FraudCheckRequest { transaction: tx }).await()

// Fire-and-forget messaging
send(fraudAgent, StatusUpdate { message: "Case closed" })

// Parallel agent queries
let (legal, compliance) = (ask(legalAgent, query) and ask(complianceAgent, query)).await()
```

#### Multi-Agent Patterns

Relang supports common multi-agent topologies:

- **Supervisor/Sub-agents**: One agent delegates to specialists and aggregates results
- **Handoff (Pipeline)**: Sequential processing through a chain of agents
- **Router**: Dynamic dispatch to specialists based on content
- **Swarm**: Peer agents collaborate via shared channels

Because agents use the same `Deferred` model as other distributed operations, they benefit from the same resumability and composition capabilities.

### 2.4 Two-Tier Error Model

Relang distinguishes between two fundamentally different kinds of errors:

**Business Errors** are expected domain-level outcomes—represented as values in union types:
```
let result: Receipt | InsufficientFunds | Declined = PaymentService.charge(amount).await()
```
You handle these via pattern matching. They're part of your workflow logic.

**Infrastructure Errors** are low-level failures—network timeouts, service unavailability, crashes. These are *not* represented in Relang. There's no `try/catch` for a socket timeout. Instead, the runtime handles them via declarative policies (retries, backoffs, circuit breakers) and the resumable execution model.

This separation keeps workflow code focused on business logic while infrastructure concerns are handled consistently by the runtime.

#### Example

```
// Business errors - handled in code via pattern matching
match PaymentService.charge(amount).await() {
    r: Receipt -> processReceipt(r)
    InsufficientFunds -> notifyUser("Insufficient funds")
    Declined -> notifyUser("Card declined")
}

// Infrastructure errors - handled by runtime
// If PaymentService is unreachable, runtime retries per policy
// If retries exhausted, workflow suspends for manual resume
```

This design follows the "let it crash" philosophy: model what you can handle in code, let the runtime handle what you cannot.

## 3. Syntax Reference

Below we propose concrete syntax choices for Relang constructs, with examples, rationale, and alternatives considered
for each.

### 3.1 Variable Binding & Type Annotations

Syntax Examples:

```
let count: Int = 5               // Explicit type annotation (Int)
let name = "Alice"               // Type inferred as String
let isValid: Bool = check(data)  // Boolean, possibly inferred from function
// Immutability by default: reassigning count = 6 would be illegal unless declared mutable
var total = 0                   // 'var' for a rare mutable variable (if allowed at all)
total = total + count           // reassign (only if declared var)
```

**Rationale**: We use a let keyword for binding new variables, which clearly distinguishes definition from mere
assignment.
By default, these are immutable, aligning with functional programming best practices – once a value is set, it doesn't
change, which prevents a class of bugs and simplifies reasoning about state changes. Types can be optionally annotated
with a colon for clarity or left for the compiler to infer. This approach (similar to languages like Rust, Kotlin, F#)
yields concise code without sacrificing type safety. The let keyword is explicit and easy for both humans and AI to
recognize as a definition, avoiding confusion with equality or comparison operators. We include an explicit var for
mutability if needed (e.g., loop counters or accumulating results in an imperative style), but encourage its sparing
use – the keyword makes it clear where mutation happens.

**Alternatives Considered**: We considered using no keyword (e.g., Python-style name = "Alice" for both binding and
assignment), but this was rejected because it can blur the line between creating a new variable and updating an existing
one, especially in an expression-oriented language. We also considered requiring const/val vs var distinction as in
Scala/Kotlin. Ultimately, a single let for most cases keeps syntax uniform, and an opt-in var covers mutation scenarios
explicitly. Some purely functional DSLs disallow mutation entirely, but we chose to allow it in a controlled way for
practicality (loops, algorithmic needs) – it's there if you need it, but obvious when used.

### 3.2 Expressions & Operators

Syntax Examples:

```
// Basic arithmetic and logical expressions
let sum = price * quantity + tax
let isEligible = (age >= 18) && hasID

// Function call as expression
let fullName = concatenate(firstName, lastName)

// Ternary-style conditional expression
let status = if (count > 0) "active" else "empty"
```

**Rationale**: Relang supports all the usual operators (`+`, `-`, `*`, `/`, comparison operators, boolean operators,
etc.) in an expression context. Since it's expression-oriented, even constructs like `if` or `match` produce values.
The syntax is designed to be familiar to developers coming from mainstream languages while remaining clean and
LLM-friendly.

**Alternatives Considered**: We considered more exotic operator sets or custom operators, but decided to stick with
well-known symbols to minimize learning curve and maximize readability.

### 3.3 Control Flow (If, Match, Loops)

Syntax Examples:

```
// If as an expression - single expression per branch (inline)
let fee = if (amount > 1000) 0.02 * amount else 0.05 * amount

// If with multiple expressions - use {} for blocks
let fee = if (amount > 1000) {
    log("Large transfer detected")
    0.02 * amount
} else {
    0.05 * amount
}

// Pattern matching with match expression
match paymentResult {
    r: Receipt -> log("Payment succeeded with ID " + r.id)
    InsufficientFunds -> notifyUser("Payment failed: insufficient funds")
    e: Error -> {
        notifyUser("Payment failed: " + e.message)
        return e
    }
}

// Looping over a collection (for-each)
for item in itemList {
    process(item)
}

// Using a loop to retry business logic a fixed number of times (if needed)
var attempts = 0
while (attempts < maxTries && not success) {
    attempts = attempts + 1
    success = attemptOperation()
}
```

**Rationale**: Conditional logic in Relang is handled by if expressions and powerful match expressions for pattern
matching. The if syntax is expression-oriented (similar to Kotlin or Rust). Parentheses around the condition clearly
delimit where the condition ends. For single expressions, no braces are needed, keeping simple cases concise. For
multiple expressions, braces `{}` group them into a block. This approach avoids redundant syntax - you never need both
a keyword like `then` and braces at the same time.

The match expression provides a clear, declarative way to handle different cases of union types. Pattern syntax uses
`name: Type` for binding (consistent with variable declarations) or just `TypeName` when no binding is needed. Each
match case is separated by a newline and uses `->` to indicate the result expression for that pattern. We allow a
catch-all using a root type (`e: Error`) or wildcard (`_`) for default handling. Pattern matching draws inspiration
from Java 21, Kotlin, and Scala, giving a concise yet expressive way to branch on types and values.

**For loops**, Relang provides a for … in … construct for iterating over collections, and a while loop for generic
conditions. In a workflow orchestration context, pure looping isn't as common as in general programming (since many
workflows are more step-by-step than heavy data crunching), but it's still useful for things like polling an external
resource until a condition is met, or iterating over a list of items to process each (though often even that might be
done with a parallel map, see below). We included for/while for familiarity and completeness. Both use `{}` for their
body blocks, consistent with the rest of the language. These constructs execute imperatively inside the
workflow, which is acceptable since within a single workflow execution thread they don't introduce nondeterminism (all
pure operations each iteration). If the loop body involves activities, the developer can decide where to place
`.await()`
calls (e.g., inside the loop if each iteration calls an external API and you want to checkpoint each time).

**Alternatives Considered**: We considered not having traditional loops at all, pushing users towards higher-level
constructs (like mapping functions or recursion for iteration, or explicit workflow constructs for things like "retry").
However, given that some scenarios (especially involving waiting/polling or simple aggregations) are more naturally
expressed with a loop, we included them for pragmatism. Another alternative for conditional syntax was a ternary
operator (cond ? expr1 : expr2) or a multi-line ternary, but we felt an if/else expression with indentation is clearer
and avoids ternary's association with less readable one-liners. For pattern matching, an alternative was a switch/case
statement like in some languages, or using if/else chains. We rejected those in favor of true pattern matching because
the latter is more powerful (can deconstruct data) and aligns with our functional ethos. Lastly, to maintain
expression-orientation, we decided that if and match should be usable as expressions (returning values) but also allowed
as statements when the return value is not needed (as in the example where match paymentResult is used for side
effects).

### 3.4 Functions

Functions are reusable blocks of code. They must be **closed**: they can only access their parameters, not variables
from outer scopes or global state (no free variables). This constraint exists because functions may be re-executed
independently during workflow replay.

Functions can contain activity calls (which return `Deferred<T>` where T is a union type) - being closed means no
external variable capture, not absence of side effects.

##### 3.4.1 Function Definition

```
// Basic function
fn add(a: Int, b: Int): Int {
    a + b
}

// Single expression (implicit return)
fn double(x: Int): Int = x * 2

// Multiple parameters with different types
fn formatUser(name: String, age: Int, verified: Bool): String {
    let status = if (verified) "verified" else "unverified"
    name + " (" + age + "), " + status
}

// Function returning a record
fn createUser(name: String, age: Int): User {
    User { name: name, age: age, verified: false }
}
```

Note: `:` is used for type annotations (consistent with variables), while `->` is reserved for lambdas and match arms.

##### 3.4.2 Generic Functions

```
// Generic function
fn first<T>(list: List<T>): T? {
    if (list.isEmpty()) null else list[0]
}

// Multiple type parameters
fn map<A, B>(list: List<A>, f: (A) -> B): List<B> {
    // ...
}

// With constraints (if supported)
fn sum<T: Numeric>(list: List<T>): T {
    list.reduce(0, (acc, x) -> acc + x)
}
```

##### 3.4.3 Lambda Expressions

Like functions, lambdas must be **closed** - they cannot capture variables from outer scopes.

```
// Lambda syntax
let double = (x: Int) -> x * 2
let add = (a: Int, b: Int) -> a + b

// Used in higher-order functions
let doubled = nums.map((x) -> x * 2)
let evens = nums.filter((x) -> x % 2 == 0)

// Multi-line lambda with block
let process = (x: Int) -> {
    let y = x * 2
    let z = y + 1
    z
}

// Used in higher-order functions
let activeNames = data.filter((x) -> x.active).map((x) -> x.name)

// NOT allowed - capturing outer variable
let multiplier = 10
let scale = (x: Int) -> x * multiplier  // ERROR: cannot capture 'multiplier'
```

**Rationale**: Both functions and lambdas must be closed (no free variables) because the workflow engine may re-execute
them independently during replay. By only allowing access to parameters, the engine can safely cache and replay results.
They can still call activities (which return `Deferred<T>` where T is a union type) - being closed is about variable
capture, not side effects.

### 3.5 Activity Calls

Any operation that produces side effects is an **activity call**. All activity calls return `Deferred<T>` (where T is
a union type) and require `.await()` to get the result (see [Concurrency Model](#concurrency-model)).

This model ensures exactly-once semantics: if the process crashes during an activity, the orchestrator reloads
the last checkpoint and retries, rather than re-running earlier steps.

##### 3.5.1 Script Execution

For script execution, each supported language has its own namespace with two methods:

- `<lang>.code(code, input)` - for inline code (the common case for small glue logic)
- `<lang>.file(path, input)` - for referencing external script files

All script calls return `Deferred` and require `.await()` to get the result.

Syntax Examples:

```
// Python - inline code
let score = python.code("""
    import pandas as pd
    df = pd.DataFrame(input)
    return df.mean()
""", inputData).await()

// Python - file reference
let csvData = python.file("format_csv.py", inputData).await()

// JavaScript - inline code
let transformed = javascript.code("return input.map(x => x * 2)", data).await()

// JavaScript - file reference
let report = javascript.file("generate_report.js", reportData).await()

// Ruby - inline code
let parsed = ruby.code("input.split(',').map(&:strip)", rawText).await()

// Ruby - file reference
let result = ruby.file("process_data.rb", inputData).await()

// Parallel script execution
let d1 = python.file("analyze.py", data1)
let d2 = python.file("transform.py", data2)
let (analysis, transformed) = (d1 and d2).await()
```

##### 3.5.2 gRPC Calls

gRPC services are called through imported Protocol Buffer definitions. This provides full type safety with
parameters and return types derived from the `.proto` schema.

```
// Import service from proto definition
import UserService from "services/user.proto"
import OrderService, PaymentService from "services/commerce.proto"

// Call service methods - returns Deferred, await to get result
let user = UserService.GetUser({ id: userId }).await()
let users = UserService.ListUsers({ limit: 10, offset: 0 }).await()

// Parallel calls
let d1 = UserService.GetUser({ id: id1 })
let d2 = UserService.GetUser({ id: id2 })
let (user1, user2) = (d1 and d2).await()

// Streaming (if supported by the service)
let stream = OrderService.WatchOrders({ customerId: custId }).await()
```

##### 3.5.3 OpenAPI Calls

HTTP APIs are called through imported OpenAPI definitions. Operations, parameters, and response types are
derived from the `.openapi.yaml` schema.

```
// Import API from OpenAPI definition
import PetStoreAPI from "apis/petstore.openapi.yaml"
import PaymentsAPI from "apis/payments.openapi.yaml"

// Call operations - returns Deferred, await to get result
let pet = PetStoreAPI.getPetById({ petId: 123 }).await()
let created = PetStoreAPI.createPet({ name: "Fluffy", type: "cat" }).await()
let deleted = PetStoreAPI.deletePet({ petId: 123 }).await()

// With optional base URL override
let pet = PetStoreAPI.getPetById({ petId: 123 }, baseUrl: "https://staging.api.com").await()

// Parallel API calls
let d1 = PetStoreAPI.getPetById({ petId: 1 })
let d2 = PetStoreAPI.getPetById({ petId: 2 })
let (pet1, pet2) = (d1 and d2).await()
```

##### 3.5.4 Generic HTTP Calls

For ad-hoc or dynamic HTTP calls (when no schema is available), generic HTTP methods are available.
These return `Deferred<HttpResponse | HttpError>` - parsing the body is the developer's responsibility.

```
// HttpResponse type (built-in)
type HttpResponse {
    status: Int,
    headers: Map<String, String>,
    body: String
}

// GET request - returns Deferred<HttpResponse | HttpError>
let response = http.get("https://api.example.com/user", query={ id: userId }).await()

// POST request
let response = http.post("https://api.example.com/orders", body=orderData).await()

// PUT request
let response = http.put("https://api.example.com/user/123", body=userData).await()

// DELETE request
let response = http.delete("https://api.example.com/user/123").await()

// With headers
let response = http.get(
    "https://api.example.com/secure",
    headers={ Authorization: "Bearer " + token }
).await()

// Parse body manually to get typed data
let user: User = json.parse(response.body)

// Parallel HTTP calls
let d1 = http.get(url1)
let d2 = http.get(url2)
let (r1, r2) = (d1 and d2).await()
```

Schema-based calls (3.5.2, 3.5.3) are preferred as they provide compile-time type checking.

##### 3.5.5 Shell Commands

Shell/Bash commands follow the same pattern as script execution, with `bash.code()` for inline commands and
`bash.file()` for shell scripts. All return `Deferred` and require `.await()`.

Syntax Examples:

```
// Inline shell command
let files = bash.code("ls -la /data", {}).await()

// Shell script file
let output = bash.file("cleanup.sh", { dir: targetDirectory }).await()

// Parallel shell commands
let d1 = bash.file("backup_db.sh", { db: "users" })
let d2 = bash.file("backup_db.sh", { db: "orders" })
let (r1, r2) = (d1 and d2).await()
```

##### 3.5.6 LLM and Agent Calls

AI-related operations have their own namespaces for clarity. All return `Deferred` and require `.await()`.

Syntax Examples:

```
// LLM chat completion (retry policy defined externally in policies block)
let response = llm.chat(prompt = conversationHistory).await()

// Spawning an agent as an activity
let fraudAgent = agent.spawn(FraudAnalystAgent, withState=caseDetails).await()

// Parallel LLM calls
let d1 = llm.chat(prompt = prompt1)
let d2 = llm.chat(prompt = prompt2)
let (r1, r2) = (d1 and d2).await()
```

##### Rationale and Alternatives

**Rationale**: The `namespace.method()` pattern provides a consistent, discoverable API surface. Each activity type
has its own namespace (`python`, `javascript`, `http`, `bash`, `llm`, `agent`), making it clear what kind of
operation is being performed. The `.code()` vs `.file()` distinction for scripts avoids ambiguity about whether a
string parameter is inline code or a file path.

**Alternatives Considered**: We considered a generic `script()` function with language as a parameter, but
language-specific namespaces are clearer and allow for language-specific options in the future.

### 3.6 Business Error Handling

Business errors are represented as union types and handled via pattern matching.

#### Type Hierarchy

Relang has two root types:

- `Success` - root type for all successful outcomes
- `Error` - root type for all error outcomes

```
        Success                         Error
           │                              │
     ┌─────┴─────┐              ┌─────────┼─────────┐
  Receipt    ShipmentOk    InsufficientFunds  Declined  FraudSuspected
```

#### Syntax Examples

```
// Activity returns a union type
let result: Receipt | InsufficientFunds | Declined = PaymentService.charge(amount).await()

// Handling business outcomes with pattern matching
match result {
    r: Receipt -> {
        log("Payment successful: " + r.id)
        updateLedger(r)
    }
    InsufficientFunds -> {
        log("Payment failed: insufficient funds")
        return InsufficientFunds  // propagate error up the workflow
    }
    Declined -> {
        log("Payment declined by provider")
        compensateOrder(orderId)
        return Declined
    }
}

// Catch-all using root type
match result {
    r: Receipt -> processReceipt(r)
    e: Error -> {
        log("Payment failed: " + e)
        return e  // propagate any error
    }
}
```

#### Pattern Matching Rules

| Pattern          | Meaning                         | Example                    |
|------------------|---------------------------------|----------------------------|
| `TypeName`       | Match specific type, no binding | `InsufficientFunds -> ...` |
| `name: TypeName` | Match type, bind to name        | `r: Receipt -> r.id`       |
| `name: Error`    | Match any error type            | `e: Error -> log(e)`       |
| `name: Success`  | Match any success type          | `s: Success -> s`          |
| `_`              | Wildcard, match anything        | `_ -> defaultHandler()`    |

#### Exhaustiveness Checking

The compiler ensures all cases are handled:

```
// ERROR: Declined not handled
match PaymentService.charge(amount).await() {
    r: Receipt -> processReceipt(r)
    InsufficientFunds -> notifyUser("...")
}

// OK: catch-all covers remaining cases
match PaymentService.charge(amount).await() {
    r: Receipt -> processReceipt(r)
    InsufficientFunds -> notifyUser("...")
    e: Error -> log("Other error: " + e)
}
```

**Rationale**: Business errors are represented as ordinary types in Relang, not wrapped in a `Result` container. This
direct approach:

1. **Reduces ceremony**: No `Ok(...)` / `Err(...)` wrapping/unwrapping
2. **Leverages the type hierarchy**: The `Success` and `Error` root types enable catch-all patterns
3. **Aligns with the type system**: Union types `A | B | C` are first-class, not special-cased
4. **Enables exhaustiveness checking**: The compiler knows all possible types in a union

We encourage defining specific error types (InsufficientFunds, Declined, etc.) rather than generic error strings. This
makes the code self-documenting and enables exhaustiveness checking. If a new error type is added to an activity's
return union, the compiler will point out unhandled cases.

**Alternatives Considered**: We considered a `Result<T, E>` wrapper type (as in Rust), but found it adds unnecessary
ceremony for a workflow DSL. The direct union type approach is simpler and more consistent with the rest of the type
system. We also considered a `?` operator for error propagation, which may be added as syntactic sugar in the future.

### 3.7 Infrastructure Policies

Infrastructure policies are defined **externally** to the workflow code, keeping business logic completely free of
infrastructure concerns. Policies are matched to activity calls based on service/method patterns.

##### 3.7.1 Policy Definition

Policies are defined in a separate configuration block or file:

```
policies {
    // Default for all HTTP calls
    http.* {
        retry: 3
        backoff: "exponential"
        timeout: 30s
    }

    // Specific service policies
    UserService.* {
        retry: 5
        timeout: 10s
    }

    // Method-specific override
    PaymentService.charge {
        retry: 0           // no retry for payments - idempotency concerns
        timeout: 60s
    }

    // Circuit breaker for flaky service
    DataService.* {
        retry: 3
        circuit_breaker: {
            failureThreshold: 10
            resetTimeout: 60s
        }
    }

    // LLM calls - longer timeout, limited retries
    llm.* {
        timeout: 120s
        retry: 2
    }
}
```

##### 3.7.2 Workflow Code (Clean)

The workflow code contains only business logic - no infrastructure concerns:

```
workflow ProcessOrder(orderId: String) -> OrderConfirmation | OrderError {
    // All these calls have policies applied automatically based on patterns
    let user = UserService.GetUser({ id: orderId }).await()
    let report = http.get(reportServiceURL, query={ id: 42 }).await()
    let payment = PaymentService.charge({ amount: 100 }).await()
    let analysis = ask(AnalysisAgent, AnalysisRequest{ input: text }).await()

    // ... business logic only
}
```

##### 3.7.3 Workflow-Level Policies

Workflows themselves can have policies applied:

```
policies {
    workflow ProcessOrder {
        timeout: 5m       // entire workflow must complete in 5 minutes
    }

    workflow BatchImport {
        timeout: 1h
    }
}
```

**Rationale**: Infrastructure policies (retry, timeout, circuit breaker) are fundamentally different from business
logic.
By defining them externally:

1. **Separation of concerns**: Workflow code focuses purely on domain logic. Developers writing business logic don't
   need to think about retry strategies or timeout values.

2. **Operational flexibility**: Policies can be adjusted without modifying workflow code. Operations teams can tune
   timeouts and retry counts based on production behavior without requiring code changes.

3. **Consistency**: Patterns like `http.*` ensure all HTTP calls follow the same policy, avoiding inconsistent
   configurations scattered throughout the codebase.

4. **Clarity**: When reading workflow code, you see only what matters for understanding the business process. Policy
   details are available separately when needed.

The runtime matches each activity call against the policy patterns (most specific match wins) and applies the configured
behavior automatically. If an activity fails due to infrastructure errors, the runtime handles retries, backoffs, and
circuit breaking transparently. The workflow code only sees the final result (success or failure after all retries
exhausted).

**Alternatives Considered**: We considered inline annotations (`@retry(max=3)` before calls) which would keep policies
visible at the call site. However, this approach has drawbacks: (1) it mixes infrastructure concerns into business
logic, (2) it's ambiguous when applied to complex expressions like `(a and b).await()`, and (3) it makes policies
harder to manage consistently across a codebase. The external policy approach cleanly separates these concerns while
still allowing fine-grained control through pattern matching.

### 3.8 Parallel Execution Patterns

For the core `Deferred<T>` model (where T is a union type) and composition operators (`and`/`or`), see the
[Concurrency Model](#concurrency-model) section.

##### 3.8.1 Dynamic Parallelism

Lists of deferreds can be awaited directly:

```
// Dispatch to all items - each call returns Deferred
let deferreds: List<Deferred<Data | ApiError>> = items.map(item -> processItem(item))

// Await all results - returns List<Data> or first ApiError
let results: List<Data> | ApiError = deferreds.await()
```

##### 3.8.2 Handling Composed Results

```
let a: Deferred<User | ApiError> = getUser()
let b: Deferred<User | ApiError> = getProfile()

// 'or' returns first success, or list of all errors
match (a or b).await() {
    u: User -> greet(u)
    errors: List<Error> -> errors.forEach(e -> log("Failed: " + e))
}

// 'and' returns list of successes, or first error
match (a and b).await() {
    users: List<User> -> processAll(users)
    e: Error -> log("Failed: " + e)
}
```

**Rationale**: The deferred model (inspired by Infinitic) is more flexible than rigid `parallel { }` blocks.
Deferreds are first-class values that can be stored, passed, and composed.

### 3.9 Type System

Relang has a strong static type system with support for records, enums, union types, tuples, and generics. This enables
compile-time safety while remaining expressive enough for complex workflow patterns.

##### 3.9.1 Records (Structs)

Records define structured data with named fields:

```
// Record type definition
type User { name: String, age: Int, verified: Bool }

// Creating a record
let alice = User { name: "Alice", age: 30, verified: true }

// Accessing fields with dot notation
log(alice.name)              // "Alice"
log(alice.age)               // 30
let isVerified = alice.verified   // true

// Nested records
type Address { street: String, city: String }
type Customer { user: User, address: Address }

// Accessing nested fields
let customer = Customer {
    user: alice,
    address: Address { street: "123 Main St", city: "Paris" }
}
log(customer.user.name)      // "Alice"
log(customer.address.city)   // "Paris"
```

Records can also be imported from external schema definitions:

```
// Import specific types from Protocol Buffers
import User from "schemas/user.proto"
import User, Address, Order from "schemas/user.proto"

// Import all types
import * from "schemas/user.proto"

// Import from AsyncAPI specification
import PaymentEvent, RefundEvent from "schemas/events.asyncapi.yaml"

// Import from OpenAPI specification
import * from "schemas/api.openapi.yaml"
```

The file extension indicates the format (`.proto`, `.asyncapi.yaml`, `.openapi.yaml`). This enables seamless
integration with existing service definitions and ensures type consistency across the system.

##### 3.9.2 Enums (Sum Types)

Enums define a fixed set of variants, optionally with associated data:

```
// Simple enum
enum Status { Pending, Approved, Rejected }

// Enum with associated data (algebraic data type)
enum PaymentResult {
    Success { receiptId: String, timestamp: DateTime },
    Declined { reason: String },
    Pending { retryAfter: Duration }
}

// Usage - pattern matching with destructuring
let result = processPayment(order)
match result {
    s: Success -> log("Paid: " + s.receiptId)
    d: Declined -> notifyUser("Payment declined: " + d.reason)
    p: Pending -> scheduleRetry(p.retryAfter)
}
```

##### 3.9.3 Root Types and Type Widening

Relang has two root types that serve as the top of the type hierarchy:

- `Success` - root type for all successful completion types
- `Error` - root type for all failure types

**No inheritance**: There is no inheritance between user-defined types. Types are flat - they don't extend other
types. The only "inheritance" is implicit widening to root types when combining different types.

**Type widening rules:**

- Same types stay specific: `T | T` = `T`
- Different types widen to root: `T1 | T2` = `Success` (for success types) or `Error` (for error types)

```
// Same types - stays specific
User | User = User

// Different types - widens to Success
User | CachedUser = Success

// Different error types - widens to Error
AuthError | CacheError = Error
```

This design:

1. Keeps the type system simple (no complex inheritance hierarchies)
2. Ensures any deferred composition is valid (types widen if needed)
3. Encourages consistent type usage (use same types to keep precision)

##### 3.9.4 Tuples

Tuples are fixed-size, heterogeneous collections:

```
// Tuple type
let point: (Int, Int) = (10, 20)
let named: (String, Int, Bool) = ("Alice", 30, true)

// Destructuring
let (x, y) = point
let (name, age, _) = named   // _ ignores a value
```

##### 3.9.5 Lists and Maps

```
// List literal (homogeneous)
let nums: List<Int> = [1, 2, 3, 4]
let names: List<String> = ["Alice", "Bob", "Charlie"]

// Map literal (JSON-style)
let settings: Map<String, Any> = {
    "retries": 3,
    "verbose": false,
    "tags": ["alpha", "beta"]
}

// Accessing entries
let retryCount = settings["retries"]
```

##### 3.9.6 Json Type

The `Json` type provides safe access to dynamic/unstructured JSON data. Field access returns `Json?` (nullable),
avoiding runtime crashes when fields are missing.

```
// Parse response body as Json
let r: Json = response.body as Json

// Access fields - returns Json? (nullable, no crash if missing)
let title: Json? = r.title
let name: Json? = r.user.name
let first: Json? = r.items[0]

// Chain safely - returns null if any part is missing
let city: Json? = r.user.address.city

// Convert to typed values with 'as'
let titleStr: String? = r.title as String?
let age: Int? = r.user.age as Int?
let tags: List<String>? = r.tags as List<String>?

// With default values
let count: Int = (r.count as Int?) ?? 0
```

`Json` sits between fully typed (schema-based) and raw string data:

- **Schema-based**: `PetStoreAPI.getPetById()` → `Pet` (fully typed, compile-time safe)
- **Json**: `response.body as Json` → `Json` (dynamic, runtime safe with nullability)
- **Raw**: `response.body` → `String` (must parse manually)

##### 3.9.7 Generics

Types can be parameterized:

```
// Generic record
type Box<T> { value: T }

// Deferred is generic over the result union type
type Deferred<T>   // built-in, where T is typically a union like Receipt | Error
```

##### 3.9.8 Type Aliases

```
type UserId = String
type Amount = Decimal
type TransferOutcome = TransferReceipt | InsufficientFunds | AccountFrozen
```

##### 3.9.9 Union Types

Union types express that a value can be one of several types:

```
// Activity that can succeed or fail
workflow charge(amount: Decimal) -> Receipt | InsufficientFunds | Declined {
    // ...
}

// Pattern matching on union types
match charge(100) {
    r: Receipt -> log("Paid: " + r.id)
    InsufficientFunds -> notifyUser("Not enough funds")
    Declined -> notifyUser("Card declined")
}

// Root type catch-all
match charge(100) {
    r: Receipt -> processReceipt(r)
    e: Error -> log("Failed: " + e)  // catches InsufficientFunds AND Declined
}
```

**Rationale**: A strong type system is essential for workflow reliability. Types catch errors at compile time,
document intent, and enable powerful IDE support.

**Design Decisions**:

- **Union types over Result wrapper**: Instead of `Result<T, E>`, Relang uses direct union types `T | E1 | E2`. This
  reduces ceremony and aligns with the type hierarchy (Success/Error root types).
- **No inheritance**: Relang does not support type inheritance. Composition is preferred over inheritance.
- **Root type widening**: When combining different types, they widen to `Success` or `Error`. This makes deferred
  composition always valid while encouraging consistent type usage.
- **Nominal typing**: Records and enums use nominal typing (two types with the same fields are still different types).

**Alternatives Considered**: We considered a `Result<T, E>` wrapper type (as in Rust/Haskell) but found the direct
union type approach simpler for a workflow DSL. It reduces ceremony and leverages the root type hierarchy naturally.

### 3.10 Workflow Definition

Syntax Examples:

```
workflow TransferFunds(request: TransferRequest) -> TransferReceipt | InsufficientFunds | AccountFrozen {
    tags: ["finance", "AI-assisted"]
    timeout: 10m

    // Workflow steps go here
    let validation = validateRequest(request)
    match validation {
        v: ValidatedRequest -> {}              // continue with validated data
        e: Error -> return e                   // immediately return business error
    }

    let fraudAgent = agent.spawn(FraudAgent, withData=validation.details).await()
    let fraudResult = ask(fraudAgent, FraudCheckInquiry{transaction: validation}).await()
    ...
    // rest of workflow logic
}

workflow BulkSync() -> Void {
    description: "Sync inventory from all warehouses"
    // (no input, no output example)
    ...
}
```

**Rationale**: A workflow in Relang is defined with a clear signature and optional metadata. The syntax workflow Name(
paramName: ParamType, ...) -> ReturnType { ... } introduces a new workflow function. Inside the braces, you can list
metadata attributes and then the sequence of steps. In the example, TransferFunds takes a
TransferRequest object as input and returns a union type `TransferReceipt | InsufficientFunds | AccountFrozen`. That
means the workflow completes with either a receipt (success) or a business error. Many workflows naturally return a
union type because they encapsulate a process that can fail or succeed in business terms.

Inside, we put metadata like tags (just an example of user-defined metadata – perhaps used for documentation,
categorization, or runtime logging) and timeout at the top. These are optional and could include things like version,
author, or default retry policies, etc. We chose a key: value syntax for metadata to keep it declarative. It's
reminiscent of workflow YAMLs but kept inline for convenience.

After metadata, the body of the workflow is simply a series of expressions/steps that are executed in order (unless
control flow directs otherwise). Because we allow expressions and not just statements, you can have a let binding as a
step, or a match handling a step result, or even just a function call on a line by itself (though that's likely
meaningless unless it has side effects or is last). You use return to produce the final output of the workflow when
needed (or you could let it fall off the end with the last expression being the result, but explicit return on success
or error makes it clear).

The example shows early return on validation error (business error). It also shows spawning an agent as part of the
workflow steps.

By making the workflow definition look function-like, we ease understanding for programmers – it's basically a function
with some special powers (suspension, etc.). Unlike a normal function, this one's execution can be paused and resumed by
the runtime, but that's abstracted away.

**Alternatives Considered**: We considered a more declarative YAML/JSON style where the workflow steps are data (like
some
workflow engines do). We rejected that because writing complex logic (like conditionals, nested agent interactions)
becomes very cumbersome in pure YAML. Instead, a code-first approach with a DSL gives more flexibility and familiarity.
We use braces `{}` around the workflow body for consistency with other block constructs in the language (if, match, for,
while). This makes the syntax uniform and avoids mixing indentation-based and brace-based blocks.

### 3.11 Sub-Workflows

Workflows can call other workflows as sub-workflows. A sub-workflow call is an activity like any other - it returns a
`Deferred<T>` where T is the union type, and the `.await()` call is the checkpoint.

##### 3.11.1 Calling Sub-Workflows

```
// Define a reusable workflow
workflow ValidateUser(userId: String) -> User | InactiveUser | NotFound {
    let user = UserService.GetUser({ id: userId }).await()
    match user {
        u: User -> {
            if (u.status == "active") u
            else InactiveUser
        }
        e: Error -> NotFound
    }
}

workflow ProcessPayment(amount: Int, method: PaymentMethod) -> Receipt | InsufficientFunds | Declined {
    // ... payment logic
}

// Main workflow calling sub-workflows
workflow ProcessOrder(orderId: String, userId: String) -> OrderConfirmation | InvalidUser | PaymentFailed {
    // Sub-workflow calls return Deferred - same as any activity
    let user = ValidateUser(userId).await()
    match user {
        e: Error -> return InvalidUser { cause: e }
        u: User -> {}
    }

    let order = OrderService.GetOrder({ id: orderId }).await()

    // Call payment sub-workflow
    let payment = ProcessPayment(order.total, order.paymentMethod).await()
    match payment {
        e: Error -> return PaymentFailed { cause: e }
        r: Receipt -> {}
    }

    OrderConfirmation { orderId: orderId, receipt: r }
}
```

##### 3.11.2 Parallel Sub-Workflows

Sub-workflows can be executed in parallel using the same deferred composition as other activities:

```
workflow FullAnalysis(data: AnalysisInput): AnalysisReport {
    // Run multiple analysis workflows in parallel
    let d1 = RiskAnalysis(data)
    let d2 = ComplianceCheck(data)
    let d3 = FraudDetection(data)

    // Wait for all to complete
    let (risk, compliance, fraud) = (d1 and d2 and d3).await()

    AnalysisReport {
        risk: risk,
        compliance: compliance,
        fraud: fraud
    }
}
```

##### 3.11.3 Sub-Workflow Behavior

Sub-workflows have the following characteristics:

- **Independent execution**: Each sub-workflow runs as a separate durable execution with its own checkpoints
- **Failure isolation**: If a sub-workflow fails, it doesn't automatically fail the parent - the parent receives an
  error result and can handle it
- **Separate retry policies**: Sub-workflows can have their own retry policies (defined in the external policies block)
- **Reusability**: The same workflow can be called from multiple parent workflows or as a top-level entry point

```
policies {
    // Policy for sub-workflow calls
    workflow.ValidateUser {
        timeout: 10s
        retry: 2
    }

    workflow.ProcessPayment {
        timeout: 60s
        retry: 0    // no retry for payments
    }
}
```

**Rationale**: Sub-workflows enable code reuse and separation of concerns. Common operations (validation, payment
processing, notification sending) can be extracted into reusable workflows. By treating sub-workflow calls the same as
other activities (returning `Deferred<T>` where T is the union type), the model remains consistent and composable.
Sub-workflows also provide a natural boundary for failure handling - the parent workflow decides how to handle
sub-workflow errors rather than having failures propagate automatically.

**Alternatives Considered**: We considered nested workflow definitions (defining a workflow inside another), but this
would complicate scoping and make workflows harder to test independently. Instead, all workflows are top-level
definitions that can call each other. We also considered a special `call` keyword for sub-workflows, but decided to
keep the syntax identical to other activity calls for consistency.

### 3.12 Agent Definition

Syntax Examples:

```
agent FraudAnalystAgent {
    system_prompt: """
        You are a Fraud Analyst AI. Evaluate the given transaction for fraud risk.
        Respond with one of: APPROVE, REJECT, or UNCLEAR, and provide reasoning.
    """
    tools: [ BankDBTool, WebSearchTool ]   // can query bank database or web if needed
    memory: full_conversation_history()   // keep entire dialog context
    persona: { role: "Fraud Department", experience: 10 }
}

// (The agent's behavior is largely driven by the LLM with the above prompt and tools.)

agent ComplianceReviewer {
    system_prompt: "You are a Compliance Officer AI. You double-check transactions flagged by FraudAnalyst."
    tools: []
    memory: last_n_messages(5)           // this agent only keeps the last 5 messages in context
}
```

**Rationale**: An agent definition in Relang establishes a template or class for an AI agent. The syntax is similar to
the workflow: agent Name { ... } with braces around its properties. We chose a declarative style for agents – you
list its configuration (prompt, tools, etc.), rather than writing imperative code for the agent. This is because for AI
agents (especially LLM-based), the behavior often comes from the prompt and tools available, not from custom code.

**Key fields in an agent definition**:

- system_prompt (or similar): This is the instruction or role given to the AI agent. By using a triple-quoted string, we
  allow multi-line prompts for richer instructions. This essentially is the personality and policy of the agent.
- tools: a list of tool capabilities the agent can use. We list them by name or reference. In FraudAnalystAgent, perhaps
  DatabaseLookupTool and WebSearchTool are predefined integrations that allow the agent to query a database or the web.
  Listing them here means at runtime the agent will have access to those.
- memory: configuration for how the agent manages its conversation memory. full_conversation_history() might be a
  built-in that indicates this agent should retain all messages in its context (ideal if you want it to have long-term
  recall, but could risk going over token limit), whereas another agent might only keep a window of the last N messages.
  This makes the agent's state handling explicit.
- We could include other metadata: in the example, persona is a custom field (maybe not needed since system_prompt
  covers it, but one could imagine structured metadata like role name or years of experience that could be injected into
  the prompt dynamically).

The agent definition does not include a function body with behavior code (no explicit message handlers in these
examples). The assumption is that by providing the prompt and tools, when this agent is sent a message (via ask or
send), the runtime will invoke the LLM with the given system prompt + conversation (and possibly allow the agent to use
tools to formulate an answer). If we wanted to support non-AI or more scripted agents, we could allow defining handlers,
but that would complicate the syntax. Likely, most agents in this context are AI-driven, so the "code" for their
behavior is effectively in the language of prompts.

By defining agents at the top level, you make them reusable. The workflow can spawn or reference these agent templates
multiple times if needed (like spawning two FraudAnalystAgents for two different cases would yield two isolated
instances with the same behavior configuration).

**Alternatives Considered**: We considered whether agent definitions should be purely data or allow some inline code.
One
idea was to allow a form of pattern matching on message types within an agent (like on MessageTypeX: do Y). This would
be akin to an actor handling different messages. While powerful, we realized that for LLM agents, that pattern is less
useful – they generally handle any prompt via the AI model, not via explicit branching code. So we opted not to include
it in this design, keeping agent definitions declarative. For simpler tool-only agents (say an agent that just fetches
from an API when asked), one might implement it as a function or an activity rather than an agent, or wrap a tool with
an AI anyway. So we didn't see a strong need for coding inside agent definitions at this level of design.

We also debated the syntax for multi-line prompts. We chose triple quotes for multi-line strings (similar to Python) to
make writing lengthy prompts easier without worrying about escaping newlines or quotes. Alternatively, prompts could be
external files referenced by path, but that complicates the DSL – better to keep it inline for now (tools could
certainly load external prompt text if needed).

Another alternative is not introducing a new keyword agent at all, and instead treating agent definitions as special
data passed to some library. But given how central agents are to our multi-agent workflows, giving them a first-class
syntax (agent Name:) makes it clear and helps tooling (like an IDE or analyzer can list all agent types readily).

Finally, some might argue to incorporate agent definitions into workflow definitions if they're only used there (like
define an agent inside a workflow). We lean toward making them top-level like workflows or types, so they can be shared
and tested independently (and perhaps even instantiated outside of a workflow, in an interactive way).

### 3.13 Agent Communication Syntax

Syntax Examples:

```
// Assuming fraudAgent and compAgent are agent references (spawned earlier or pre-defined singletons):

// Fire-and-forget send:
send(fraudAgent, TransactionData{ id: tx.id, amount: tx.amount })

// Ask (request-response):
let analysis: FraudAssessment = ask(fraudAgent, FraudCheckInquiry{ transaction: tx }).await()
if (analysis.risk == "high") {
    log("Fraud risk is high: " + analysis.reason)
}

// Broadcast to a channel (all subscribers get the message):
broadcast(RiskDiscussionChannel, { type: "status_update", text: "New transaction under review." })

// Subscribe agents to a channel:
channel RiskDiscussionChannel
subscribe fraudAgent, compAgent to RiskDiscussionChannel

// Agents can now communicate via broadcasts on that channel:
send(fraudAgent, { type: "request_opinion", target: compAgent.id, details: analysis })
// (Alternatively, fraudAgent itself might broadcast on the channel which compAgent hears)

// The workflow can wait for a response from an agent on a channel:
let responseMsg = await_message(from=compAgent, channel=RiskDiscussionChannel, timeout=30s)
```

**Rationale**: The syntax for agent communication uses simple verbs that make it clear what's happening:

- send(agent, message) is straightforward: you provide an agent reference (or identifier) and a message object. This
  enqueues the message into that agent's mailbox and returns immediately. There's no result to capture (unless maybe we
  return a message ID or promise, but in the fire-and-forget we typically don't).
- ask(agent, message) looks similar, but under the hood it expects that agent to eventually return a reply (likely via a
  special reply message or the call's protocol). In syntax, we treat it like a function call that yields a result (of a
  known type if the agent is expected to return a certain schema). It is effectively sugar for sending a message and
  waiting for a corresponding response message. We might implement it by generating a unique request ID and having the
  agent's reply include that ID, etc., but that's internal.

We explicitly choose function-call-like syntax for send and ask (with parentheses and comma separation) to align with
normal function calls. It keeps things uniform and is easier for parsing and generation. We considered keywords (like
send agent, message) but that could introduce parsing ambiguities or require special-case grammar. As functions, they
can be easily recognized.

For broadcasting, we introduced a broadcast(channel, message) built-in. To use it, a channel must be declared (e.g.,
channel RiskDiscussionChannel). Declaring a channel could be top-level or within a workflow, but in syntax it looks like
just writing channel Name. This would set up a pub-sub channel identified by Name. Then subscribe A, B to Name attaches
those agents to the channel. We chose a natural language-like syntax for subscription to make it obvious. Alternatively,
subscribe(agentA, Name) function could have been used, but the infix to reads nicely.

The await_message (or some similar construct) is used if the workflow itself wants to wait for a message on a channel or
from an agent. In the example, the workflow waits for a message from compAgent on the channel, with a timeout. We might
integrate this waiting differently (for example, an ask could actually be implemented by internally doing a wait on a
channel for a reply). But showing an explicit wait like this demonstrates that the workflow can pause until it hears
something from the multi-agent conversation. We used await_message here as a conceptual placeholder; it could also be an
activity like `receive(channel, filter).await()` which would persist the state and wait for an event. The exact syntax
can
be refined, but the idea is to give the workflow a way to synchronize with agent-driven events if needed.

Messages themselves are often simple data objects. In examples we show TransactionData{...} and an inline
{type: "...", ...}. Since messages are structured, you might define message types similarly to records or use generic
records. Using type fields for messages (like a tagged union) is common, but we might also use different message types (
classes) and pattern match on those in agent code if that existed. But since agent behavior is AI, likely the agent
doesn't do an explicit pattern match; rather, the content of the message is interpreted via the prompt or tool usage.
Still, the structure is good for the sending side to ensure it sends all needed info.

**Alternatives Considered**: We considered not exposing a low-level send at all and only using higher-level constructs (
like
always using ask and if you don't care about reply, just ignore it). But send is important for cases where you truly
don't expect a response. It also aligns with messaging systems and is intuitive.

Another alternative was to allow agents to send messages to each other directly (i.e., inside their "thinking", an agent
could decide to send to another). In our design, since we don't script inside the agent, direct agent-to-agent send
isn't in the syntax. Instead, an agent could produce an output that the orchestrator interprets as a message to another.
For example, FraudAnalystAgent might output, "I need a second opinion from compliance," and the orchestrator could then
route that as a message or decision to involve ComplianceAgent. This requires some orchestration logic that is beyond
basic syntax (likely part of patterns or the runtime's understanding of agent outputs). We mention it conceptually but
don't formalize it here.

For channels, an alternative was to manage them implicitly (like any broadcast to a string topic name and agents
subscribe via code or config). We chose an explicit channel declaration to catch errors (using an undeclared channel
name could be a mistake, so better to declare it). Also it allows perhaps typing or constraining what messages go on a
channel (maybe future extension: channel X of TypeY to restrict message types).

We also thought about multi-cast to a set of agents without a channel, like send([agent1, agent2], message) to send
directly to multiple. That could be a convenient sugar but isn't strictly necessary if you have channels (you could
subscribe those two to a transient channel and broadcast). For simplicity, we left that out in favor of channels which
handle 1-to-many elegantly.

### 3.14 External Agents (A2A Protocol)

For communication with **external agents** (agents from other organizations, third-party services, or agents built with
different frameworks), Relang supports the [Agent2Agent (A2A) protocol](https://a2a-protocol.org/). This enables
cross-organization interoperability while maintaining the simplicity of internal agent communication.

##### 3.14.1 Internal vs External Agents

| Aspect        | Internal Agents          | External Agents (A2A)    |
|---------------|--------------------------|--------------------------|
| Scope         | Same workflow/system     | Cross-organization       |
| Discovery     | Static definitions       | Agent Cards              |
| Communication | `send`/`ask`/`broadcast` | Task submission          |
| Transport     | In-process               | HTTP/JSON-RPC 2.0        |
| Trust         | Full control             | Opaque, capability-based |

##### 3.14.2 Importing External Agents

External agents are discovered via their Agent Card URL:

```
// Import external agent from A2A-compatible endpoint
import SupplierAgent from "https://supplier.example.com/.well-known/agent.json"
import ShippingAgent from "https://logistics.example.com/.well-known/agent.json"

// Agent Card provides: identity, capabilities, skills, endpoint, auth requirements
```

##### 3.14.3 Task-Based Communication

External agents use A2A's task-based model rather than direct messaging:

```
workflow OrderFromSupplier(order: OrderRequest) -> OrderConfirmation | SupplierRejected {
    // Submit task to external agent - returns Deferred<Task<Artifact>>
    let task = SupplierAgent.submitTask({
        skill: "process-order",
        input: DataPart { data: order }
    })

    // Wait for task completion
    let result = task.await()

    match result.status {
        "completed" -> {
            // Extract artifacts (deliverables) from completed task
            let confirmation = result.artifacts[0].data as OrderConfirmation
            confirmation
        }
        "failed" -> SupplierRejected { error: result.error }
    }
}
```

##### 3.14.4 Long-Running Tasks

A2A tasks can be long-running with status updates:

```
workflow ProcessWithExternalReview(document: Document): ReviewResult {
    // Submit long-running task
    let task = ReviewAgent.submitTask({
        skill: "compliance-review",
        input: FilePart { file: document }
    })

    // Task progresses through states: submitted -> working -> completed/failed
    // .await() blocks until terminal state
    let result = task.await()

    // Or poll for status updates
    while (task.status() == "working") {
        log("Review in progress: " + task.progress())
        sleep(10s)
    }

    result.artifacts[0].data as ReviewResult
}
```

##### 3.14.5 Parallel External Tasks

External agent tasks compose with the same `and`/`or` operators:

```
workflow GetQuotes(rfq: RequestForQuote): List<Quote> {
    // Submit to multiple external suppliers in parallel
    let t1 = SupplierA.submitTask({ skill: "quote", input: DataPart { data: rfq } })
    let t2 = SupplierB.submitTask({ skill: "quote", input: DataPart { data: rfq } })
    let t3 = SupplierC.submitTask({ skill: "quote", input: DataPart { data: rfq } })

    // Wait for all quotes
    let (r1, r2, r3) = (t1 and t2 and t3).await()

    [r1.artifacts[0].data, r2.artifacts[0].data, r3.artifacts[0].data] as List<Quote>
}
```

##### 3.14.6 A2A Concepts Mapping

| A2A Concept | Relang Representation              |
|-------------|------------------------------------|
| Agent Card  | Import URL metadata                |
| Task        | `Deferred<Task<Artifact>>`         |
| Message     | Input to `submitTask`              |
| Artifact    | Task result output                 |
| Part        | `TextPart`, `FilePart`, `DataPart` |

**Rationale**: Relang distinguishes between internal and external agents because they have fundamentally different
trust models and interaction patterns. Internal agents (`send`/`ask`/`broadcast`) are tightly coupled, controlled
within the same system, and optimized for low-latency coordination. External agents (A2A) are opaque services where
you only know their advertised capabilities - the task-based model is appropriate because you're delegating work
rather than orchestrating step-by-step.

By supporting A2A, Relang workflows can integrate with the broader ecosystem of AI agents without sacrificing the
simplicity of internal orchestration. This follows the same philosophy as MCP integration: use the right protocol
for the right boundary.

**Alternatives Considered**: We considered using `ask()` for external agents too, but the semantics are different -
`ask()` implies a quick request/response, while external tasks can take minutes or hours. The explicit `submitTask()`
makes this distinction clear. We also considered automatic Agent Card caching and capability matching, but deferred
this to runtime implementation.

### 3.15 Agent Lifecycle

Syntax Examples:

```
// Spawning an agent instance from a defined agent type:
let fraudAgent = spawn FraudAnalystAgent(transactionId = tx.id)
// Here, FraudAnalystAgent is the template; we create an instance for a specific transaction.
// Optionally passing initial context data (like transactionId).

// Spawn with supervision options:
let worker = spawn DataFetcherAgent() supervise(restart=max(3))
// This means if DataFetcherAgent crashes, automatically restart up to 3 times.

// Terminating an agent when done:
terminate fraudAgent // gracefully shut down the fraudAgent instance

// Spawning multiple and tracking:
let agents = []
for id in caseIds {
    agents.append(spawn InvestigateAgent(case=id))
}
// Now 'agents' holds references to all spawned agents.

terminate agents // terminate all in list (if supported)
```

**Rationale**: The spawn keyword (or function) is used to create new agent instances at runtime. This is akin to calling
a constructor or launching a new process. We present it as spawn AgentType(args) which returns a reference (we used let
fraudAgent to capture it). You can optionally pass arguments for initialization – since agents might need some initial
data or state (like which transaction to analyze). This could map to seeding the agent's memory or prompt with specific
info.

By spawning on the fly, the workflow can scale the number of agents based on input (e.g., spawn one agent per item to
process in parallel, etc.). Each spawned agent is independent with its own state, even if they share the same
definition.

We included a supervision clause in the spawn example: supervise(restart=max(3)) suggests that this agent should be
automatically restarted if it stops unexpectedly (infrastructure failure or perhaps even if it returns a special failure
result, depending on policy). We choose a fluent style where spawn ... supervise(...) reads naturally. Under the hood,
this might register the agent with the orchestrator's supervision tree with the given strategy. Other strategies might
include supervise(escalate) (let the failure bubble up) or supervise(ignore) (just die quietly). For brevity, we only
showed restart with a limit.

terminate is straightforward: a keyword or built-in that stops an agent (or a list of agents). It might send a shutdown
message and do cleanup. We allow terminating by reference.

In many workflows, you might not need to manually terminate agents if they naturally finish (LLM agents might finish
when they produce an answer, but they might still be technically alive waiting for more messages). So an orchestrator
could automatically terminate all remaining agents at the end of a workflow. But having the ability to do it explicitly
is important for long-running workflows that spawn persistent agents (like a monitoring agent that you later decide to
stop).

**Alternatives Considered**: We considered more implicit lifecycle: e.g., spawn an agent and it auto-terminates itself
after sending a final response if coded that way. That could still happen (an agent could have a self-termination
condition),
but we wanted the workflow to have control as well. Another idea was to integrate termination into patterns – e.g.,
after a sequence completes, auto-terminate those agents. Possibly the runtime will do that anyway if it knows they won't
be used further. But to avoid magic, the syntax for terminate is provided.

We also debated whether spawn should return a `Deferred` since spawning could be asynchronous (starting a container
might take some time). Perhaps spawning an agent is quick (just allocating context) so it might not need a checkpoint,
but if an agent involves loading an AI model or establishing a connection, it could take time. If so,
`spawn(Agent).await()`
would treat it as an activity with a checkpoint.

We keep spawn as potentially instantaneous (no `.await()` in examples), assuming agent startup is quick relative to
calls. But this is something that could be refined – maybe spawning an agent that uses an external LLM implicitly does a
first call to set up context, etc.

Finally, an alternative syntax for supervision was to declare it in the agent definition. For instance, the
agent type could declare a default supervision policy (like "if I crash, restart me"). We opted for a spawn-level
specification for fine-grained control. Another approach is having different spawn functions like
spawn_supervised vs spawn_link (like Erlang has spawn_link). We could incorporate a notion of linking the lifetimes (if
supervisor dies, maybe kill children). Possibly all spawns are linked by default (if workflow ends, kill agents).

We assume the orchestrator cleans up any straggling agents if the workflow completes or is terminated, even if terminate
not called, but calling it explicitly makes the intent clear (like shutting down intermediate helpers to save
resources).

## 4. Complete Example: AI-Assisted Fraud Detection Workflow

Below is a fully worked out example combining many of the constructs above, implementing the scenario described:

Process: Process a wire transfer with AI-assisted fraud detection.

1. Validate transfer request (pure logic).
2. Spawn a FraudAnalyst agent to assess risk.
3. If risk is unclear, spawn a ComplianceReviewer agent for second opinion.
4. The two agents exchange messages to discuss the case (swarm pattern).
5. Collect their assessments and make final decision.
6. Execute the transfer if approved, or reject with explanation.
7. In parallel, send notifications of the outcome.

We define the necessary data types and agents, then the workflow:

```
// Business data types
type TransferRequest { fromAccount: String, toAccount: String, amount: Decimal }
type TransferReceipt { confirmationId: String, timestamp: DateTime }

// Error types (subtypes of Error root type)
type InsufficientFunds {}
type AccountFrozen {}
type FraudDetected {}
type ComplianceHold {}

// AI Agents definitions
agent FraudAnalystAgent {
    system_prompt: """
    You are a Fraud Analyst AI. Evaluate the given transaction for fraud risk.
    Respond with one of: APPROVE, REJECT, or UNCLEAR, and provide reasoning.
    """
    tools: [ BankDBTool, WebSearchTool ]   // can query bank database or web if needed
    memory: last_n_messages(10)
}

agent ComplianceReviewerAgent {
    system_prompt: """
    You are a Compliance Officer AI. If a transaction is flagged as unclear,
    you review it and decide to APPROVE or REJECT based on regulations.
    Discuss with the Fraud Analyst if needed to reach a decision.
    """
    tools: [ PolicyDBTool ]
    memory: last_n_messages(10)
}

// Workflow Definition
workflow ProcessTransfer(request: TransferRequest) -> TransferReceipt | InsufficientFunds | AccountFrozen | FraudDetected | ComplianceHold {
    timeout: 30m // overall time limit for this workflow

    // Step 1: Validate the request (pure logic)
    let validation = validateRequest(request)   // returns ValidatedRequest | InsufficientFunds | AccountFrozen
    match validation {
        validated: ValidatedRequest -> { /* proceed with validated data */ }
        e: Error -> return e   // e.g., InsufficientFunds or AccountFrozen directly returned
    }

    // Step 2: Spawn FraudAnalyst agent
    let fraudAgent = spawn FraudAnalystAgent(transactionDetails = validated)
    // (Passing the transaction details so the agent has context. Could also be sent in the message.)

    // Step 2b: Ask FraudAnalyst for risk assessment (retry policy defined externally)
    let fraudAssessment = ask(fraudAgent, FraudCheckInquiry { transaction: validated }).await()
    // Assume FraudCheckInquiry is a record type message the agent understands.

    // Step 3: If risk is unclear, spawn ComplianceReviewer for second opinion
    let finalDecision = if (fraudAssessment.decision == "UNCLEAR") {
        // Spawn compliance agent and set up communication channel for swarm discussion
        let compAgent = spawn ComplianceReviewerAgent(caseId = validated.transactionId)
        channel DiscussionChannel
        subscribe fraudAgent, compAgent to DiscussionChannel

        // Step 4: Initiate discussion between agents
        broadcast(DiscussionChannel, { type: "start_discussion", details: validated })
        // The two agents now exchange messages on DiscussionChannel (swarm pattern)

        // Wait for a consensus or decision from the agents, with a timeout
        let conclusionMsg = await_message(from=compAgent, channel=DiscussionChannel, timeout=60s)
        terminate compAgent   // once we got the compliance response, we can stop that agent
        // Determine outcome based on compliance's conclusion message
        if (conclusionMsg.decision == "APPROVE") "APPROVE" else "REJECT: " + conclusionMsg.reason
    } else {
        // Fraud agent gave a clear decision (APPROVE or REJECT)
        fraudAssessment.decision + (fraudAssessment.reason ? ": " + fraudAssessment.reason : "")
    }

    // Step 5: Make final decision (based on finalDecision string)
    if (finalDecision.startsWith("APPROVE")) {
        // Step 6: Execute transfer (retry/timeout policies defined externally)
        let receipt = http.post(bankAPI + "/transfer", body={
            from: request.fromAccount, to: request.toAccount, amount: request.amount
        }).await()
        match receipt {
            data: TransferData -> {
                // Completed successfully
                // Step 7: send notifications in parallel using deferreds
                let notifyUser = http.post(notifyService + "/user", body={ user: request.fromAccount, status: "Transfer completed", id: data.confirmationId })
                let notifyAudit = http.post(notifyService + "/audit", body={ txId: data.confirmationId, status: "COMPLETED" })
                (notifyUser and notifyAudit).await()
                return TransferReceipt { confirmationId: data.confirmationId, timestamp: data.timestamp }
            }
            e: Error -> {
                // If the bank API returned a business error, propagate it.
                return FraudDetected
            }
        }
    } else {
        // finalDecision indicates REJECT
        log("Transfer rejected: " + finalDecision)
        // Send notifications about rejection in parallel
        let notifyUser = http.post(notifyService + "/user", body={ user: request.fromAccount, status: "Transfer rejected", reason: finalDecision })
        let notifyAudit = http.post(notifyService + "/audit", body={ txId: request.id, status: "REJECTED", reason: finalDecision })
        (notifyUser and notifyAudit).await()
        return if (finalDecision.contains("Fraud")) FraudDetected else ComplianceHold
    }
}
```

**Explanation**: This workflow demonstrates a combination of sequential and parallel logic and agent orchestration:

- We start by validating the request with pure code (no external calls). If validation returns a business error (like
  InsufficientFunds or AccountFrozen), we immediately return that error – this aborts the workflow with the domain
  error,
  which the caller can handle or log.
- Next, we spawn a FraudAnalystAgent – this is an AI agent that will analyze the transaction. The spawn is quick, and we
  store a reference fraudAgent. We then use ask with a FraudCheckInquiry message to get an assessment. Retry policies
  for
  agent calls are defined externally (e.g., `ask.*` pattern in the policies block), so if the LLM call fails due to an
  API error or the agent's tool call failing, the runtime will retry automatically. The result is captured in
  fraudAssessment. We expect this to have a field like decision
  which might be "APPROVE", "REJECT", or "UNCLEAR", and maybe a reason string explaining.
- If the fraud agent couldn't decide (UNCLEAR), we escalate to a compliance review. We spawn a ComplianceReviewerAgent.
  Then we create a DiscussionChannel and subscribe both agents to it. This sets up a communication line between them (
  the
  swarm pattern). We broadcast a start_discussion message containing the transaction details to kick off their
  interaction.
- At this point, presumably, the FraudAnalystAgent might share its concerns, and the ComplianceAgent might ask questions
  or consult policies (this happens internally via the LLMs exchanging on the channel). We don't script each message;
  instead, we wait for a conclusion. We use await_message to wait for a message from the compliance agent on that
  channel,
  with a 60s timeout (to avoid hanging forever). We expect the Compliance agent to eventually send a message like
  {decision: "APPROVE"} or {decision: "REJECT", reason: "some explanation"} when done. Once we get it, we terminate the
  compliance agent (freeing resources, as it's no longer needed). We then set finalDecision based on that message (e.g.,
  a
  string "APPROVE" or "REJECT: [reason]"). If the agents took too long or didn't send a clear decision, the timeout
  would
  trigger and conclusionMsg might be null – we could handle that by defaulting to reject or escalate, but for brevity we
  assume success.
- If the fraud agent already gave a clear APPROVE or REJECT, we skip compliance. finalDecision just takes the fraud
  decision.
- Now for final action: If finalDecision starts with "APPROVE", we proceed to execute the transfer by calling the bank
  API. This is a side-effect (HTTP call), so we await it. If that returns a TransferData result,
  we got a confirmation (perhaps data contains a confirmationId and timestamp). We then run two notifications in
  parallel: one to the user (via a notification service) and one to an audit system. These run concurrently to save
  time.
  After sending notifications, we return a TransferReceipt with the details.
- If receipt came back as an error – perhaps a business error from the bank (though ideally if InsufficientFunds it
  would've been caught earlier, but imagine between validation and execution something changed or the bank applied other
  rules). We treat any error from final execution as FraudDetected for simplicity (or we could map specific codes).
  Essentially, if the transfer didn't go through, we fail the workflow.
- If finalDecision was reject, we log it and send out rejection notifications (also in parallel for efficiency). We then
  return an error type. We decide which error to return based on the reason: if it contains "Fraud", we return
  FraudDetected, otherwise use ComplianceHold (for example, if compliance just had a policy issue). In real design, we
  might have more structured information from the agents to decide this, but here it's simplified.

Throughout this example, we see:

- Use of pure functions (validateRequest),
- Pattern matching on union types (match on validation),
- Spawning and messaging agents,
- Swarm channel usage,
- Suspend on external calls,
- Parallel for notifications,
- Policies for retry/timeout on critical calls,
- Domain errors being returned directly as typed values, not caught via exceptions.

The separation of concerns is evident: we didn't catch network errors around the ask or http.post – instead we declared
what to do (retry, timeout) and focused the code on the business decisions (whether to escalate to compliance, whether
to approve or reject). This makes the flow relatively linear and easy to follow.

Even though agents introduce concurrency and complexity, the use of await_message and the structured swarm pattern
ensures we only proceed when we have a definite outcome from them, preserving determinism of the workflow's progression.

## 5. Grammar (Pseudo-BNF)

Below is a high-level grammar outline for core Relang constructs to illustrate how the syntax might be formally
structured. This is not exhaustive, but covers key elements:

```
<workflow-def> ::= "workflow" <Ident> "(" <param-list>? ")" "->" <type> "{" <workflow-body> "}"

<param-list>  ::= <param> ("," <param>)*
<param>      ::= <Ident> ":" <type>
<type>       ::= <Ident> [ "<" <type-params> ">" ]  |  // e.g., List<Int>
                 "String" | "Int" | "Bool" | "Decimal" | ...  |
                 <union-type> |                     // e.g., Receipt | Error
                 <record-type> | <enum-type>        // user-defined types

<workflow-body> ::= { <statement> NEWLINE }* [ "return" <expr> ]

<statement> ::= <binding>
             |  <expr-stmt>
             |  <if-expr>
             |  <match-expr>
             |  <for-loop>
             |  <while-loop>
             |  <activity-call>
             |  <deferred-composition>
             |  <await-expr>
             |  <subscribe-stmt>
             |  <channel-decl>
             |  <terminate-stmt>
             |  (any allowed construct used for its effect)

<binding> ::= "let" <Ident> [ ":" <type> ] "=" <expr>
            | "var" <Ident> [ ":" <type> ] "=" <expr>

<expr-stmt> ::= <expr>    // an expression used as a statement (when side-effects only)

<if-expr> ::= "if" "(" <expr> ")" <expr-or-block> [ "else" <expr-or-block> ]
<expr-or-block> ::= <expr> | "{" <expr-block> "}"
// Single expression needs no braces; multiple expressions use braces

<match-expr> ::= "match" <expr> "{" { <pattern> "->" <expr-or-block> NEWLINE }+ "}"
<pattern>   ::= <literal>
             |  <Ident>                            // type name without binding (e.g., InsufficientFunds)
             |  <Ident> ":" <Ident>                // binding with type (e.g., r: Receipt)
             |  <Ident> "{" <field-pattern-list> "}"  // for record destructuring
             |  "_"                                 // wildcard
<field-pattern-list> ::= <Ident> ":" <pattern> ("," <Ident> ":" <pattern>)*

<for-loop> ::= "for" <Ident> "in" <expr> "{" <workflow-body> "}"
<while-loop> ::= "while" "(" <expr> ")" "{" <workflow-body> "}"

<activity-call> ::= [<annotations>] <activity-invocation>
// All activity calls return Deferred<T> (union type) by default
// Use .await() to get the result: let x = http.get(...).await()
<activity-invocation> ::=
       "http." <Ident> "(" <arg-list>? ")"            // e.g., http.get(url, options)
     | "grpc.call(" <Service.Method> "," <arg> ")"    // e.g., grpc.call(Service.Method, request)
     | "python.code(" <stringLit> "," <expr>? ")"     // Python inline code
     | "python.file(" <stringLit> "," <expr>? ")"     // Python script file
     | "javascript.code(" <stringLit> "," <expr>? ")" // JavaScript inline code
     | "javascript.file(" <stringLit> "," <expr>? ")" // JavaScript script file
     | "bash.code(" <stringLit> "," <expr>? ")"       // Bash inline code
     | "bash.file(" <stringLit> "," <expr>? ")"       // Bash script file
     | "llm.chat(" <llm-params> ")"
     | "tool.call(" <ToolName> "," <expr> ")"
     | "agent.spawn(" <AgentType> "," <initParams>? ")"
     | <Ident> "(" <arg-list>? ")"    // could cover calling another sub-workflow or local function

<annotations> ::= { "@" <Ident> "(" <param-assign-list>? ")" NEWLINE }*
// An annotation starts with @, may have parameters inside (...). Could allow multiple stacked.

// Deferred composition for parallel execution
// All activity calls return Deferred<T> (union type) by default
<deferred-composition> ::= <deferred-expr> ("and" <deferred-expr>)+
                        |  <deferred-expr> ("or" <deferred-expr>)+
<deferred-expr> ::= <Ident> | <activity-invocation> | "(" <deferred-composition> ")"
<await-expr> ::= <deferred-expr> ".await()"       // checkpoint - awaits and returns result

// Union types for deferred composition results
<union-type> ::= <type> ("|" <type>)+             // e.g., String | Int | Error
<tuple-type> ::= "(" <type> ("," <type>)+ ")"     // e.g., (A, B, C)

<channel-decl> ::= "channel" <Ident>
<subscribe-stmt> ::= "subscribe" <Ident-list> "to" <Ident>
<Ident-list> ::= <Ident> ("," <Ident>)*

<send-call> ::= "send(" <expr> "," <expr> ")"    // likely parsed as an activity call internally
<ask-call>  ::= "ask(" <expr> "," <expr> ")"
<broadcast-call> ::= "broadcast(" <Ident> "," <expr> ")"

<terminate-stmt> ::= "terminate" <expr>    // expr can be agent reference or list of agents

<agent-def> ::= "agent" <Ident> "{" <agent-body> "}"
<agent-body> ::= { <agent-field> NEWLINE }*
<agent-field> ::= "system_prompt:" <multiline_string>
               | "tools:" "[" <Ident-list> "]"
               | "memory:" <expr>
               | <Ident> ":" <expr>        // allow custom fields or configurations

<type-def> ::= "type" <Ident> "{" { <field-def> (NEWLINE)? } "}"
<field-def> ::= <Ident> ":" <type>
<enum-def> ::= "enum" <Ident> "{ " <Ident> ("," <Ident>)* " }"

// Basic lexical tokens
<Ident> ::= /[A-Za-z_][A-Za-z0-9_]*/  (typical identifier rules)
<stringLit> ::= double-quoted string (with possible escapes)
<multiline_string> ::= triple-quoted string literal
<number> ::= integer or decimal numeric literal
<literal> ::= <number> | <stringLit> | "true" | "false"
```

This grammar sketch illustrates how Relang might be structured. Braces `{}` define blocks consistently throughout the
language. We allow statements and expressions to intermix as an expression-oriented design. Notably,
activity-call, send/ask/broadcast, etc., can be parsed in a similar way as function calls since we wrote them with
parentheses.

The grammar would need refining and formal verification, but it captures the spirit: a mix of familiar imperative
constructs (for, if) given expression semantics, and special forms (match, deferred composition) for the unique
features.

## 6. Open Questions and Trade-offs

Finally, there are some open design questions and trade-offs we'd want to consider and get feedback on:

- Block Syntax: We chose a consistent brace-based `{}` syntax for all blocks (workflows, agents, if, match, for, while).
  This follows Kotlin's approach: single expressions don't need braces, but multiple expressions require them. This
  avoids mixing indentation-based and brace-based styles, making the language more predictable and
  easier to parse. Parentheses `()` are used around conditions in `if` and `while` to clearly delimit the condition from
  the body.
- Error Propagation Sugar: In the current design, handling union types often means writing a match with an error branch
  that returns. We might consider adding a propagation operator (like Rust's `?`) or a construct to automatically unwrap
  successes or return-on-error to reduce boilerplate. This could make workflows even more concise when many steps can
  just propagate errors upward. The trade-off is added complexity in the language. Recommendation: Prototype a `?`
  operator that effectively expands to an error return, and see if it reads clearly in Relang. If it introduces too much
  magic, stick with explicit pattern matches.
- Agent-to-Agent Direct Calls: In our design, agents interact via messaging orchestrated by the workflow (except in
  swarm where they broadcast freely). A question is whether agents should have the ability to directly call each other (
  e.g., AgentA invokes AgentB without the main workflow's immediate involvement). This would require embedding messaging
  logic in agent behaviors or providing some autonomous ability. It could increase complexity and blur the
  single-threaded
  nature of the workflow. Recommendation: Keep the orchestrator (workflow runtime) as the communication mediator for
  now,
  to maintain clarity and control. Possibly simulate direct calls by high-level patterns (router, etc.) rather than
  truly
  independent agent threads sending messages behind the scenes.
- Complex Data Manipulation vs Simplicity: We included a decent set of data structuring tools (records, maps, lists,
  pattern matching). One might question if a simpler, more JSON/YAML approach (without custom types, with dynamic
  typing)
  would suffice for a workflow DSL, as many current workflow engines use JSON schemas. We opted for stronger typing to
  help catch errors and enable larger programs. However, this increases the learning curve and implementation
  complexity (
  type checker, generics, etc.). Is it worth it for a DSL that might be used by less experienced developers or even
  directly by AI? Feedback needed: Are the benefits of strong typing in workflow logic worth the added
  verbosity/intricacy, or should we lean more toward a minimalist dynamically-typed DSL and rely on testing to catch
  issues? Perhaps a middle ground is to keep types but make annotations mostly optional, which we attempted.
- LLM Agent Behavior Customization: In the current design, agent behavior is largely controlled by prompts and tools.
  This is flexible but also somewhat opaque; if you wanted an agent to, say, always forward certain messages or have a
  deterministic subroutine, you can't easily code that. We could consider allowing an agent to have handlers (like on
  messageType do X as code) or even allow an agent to be implemented by a code routine instead of an LLM. This would
  make
  the agent concept more general (actors that can be either AI or code). It complicates the language though. Open
  question: Should Relang agents remain primarily AI (LLM) driven with minimal internal logic, or expand to general
  actors
  with full coding ability? This impacts syntax (we might need a way to differentiate AI agents vs code agents, or allow
  both in one definition).
- Testing and Simulation: How do we test workflows and agents? We might want a way to simulate activities (e.g., a
  http.post returns a dummy value in test mode) or mock agent responses. Designing a syntax for test expectations or a
  mode where activity calls can be stubbed could be useful. For example, allowing a test { ... } block where you provide
  fake outputs for activities. This isn't covered in our design yet. Trade-off: adding such features increases DSL
  complexity but greatly aids developer confidence. Possibly this can be handled outside the core language (via a
  testing
  framework that intercepts calls), so maybe not a syntax issue.
- Workflow Composition: Should workflows be able to call other workflows as subroutines? We touched on calling a
  sub-workflow as an activity (callWorkflow). If so, is it just via the same activity mechanism or a special syntax?
  This
  might be useful for reuse (like call a common payment workflow from multiple places). We'd likely treat sub-workflows
  like any other service call (perhaps via `SubWorkflow(input).await()` which under the hood starts that workflow and
  waits for result). Not a big syntax addition, but something to formalize.

Each of these points would benefit from feedback and careful consideration. Our current design leans towards
explicitness, clarity, and leveraging known good practices (from functional programming, actor model, etc.). With
further iteration and user testing, we can refine these choices to ensure Relang is both powerful and approachable for
orchestrating the next generation of resilient, AI-infused workflows.

# Relang Syntax Design Specification Plan

**Created**: 2025-01-15
**Status**: Ready for execution
**Type**: Language Design Specification (Document Only)

---

## Summary

Design a complete syntax specification for Relang, a workflow orchestration language with first-class support for AI agents, resumable execution, and a two-tier error model. The deliverable is a comprehensive specification document—no implementation.

### Key Design Decisions (Confirmed)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Scope | Complete redesign | New paradigm, not incremental |
| Deliverable | Specification document | Foundation for future implementation |
| Runtime design | Out of scope | Syntax only; runtime is separate |
| Syntax style | Kotlin-Rust hybrid | LLM-reliable, familiar, JSON-native |
| Block delimiters | `{ }` braces | Copy-paste safe, JSON-friendly |
| Semicolons | Optional (newline-terminated) | Cleaner, Kotlin/Go style |
| Expression-oriented | Yes | Last expression is return value |

---

## Acceptance Criteria

- [ ] Complete specification document covering all 13 constructs
- [ ] Each construct has: syntax, 2-3 examples, rationale, alternatives considered
- [ ] Error model deep-dive: business vs infrastructure errors clearly distinguished
- [ ] Agent communication deep-dive: primitives, patterns, topology configuration
- [ ] Complete wire transfer example fully implemented in proposed syntax
- [ ] Pseudo-BNF grammar sketch for all constructs
- [ ] Open questions section with trade-offs for feedback

---

## Implementation Plan

### Phase 1: Foundation Constructs

#### Task 1.1: Design Principles Document
**Output**: Section 1 of spec - "Design Principles Summary"

Codify the 5-7 guiding rules:
1. Expression-oriented: everything returns a value
2. Immutable by default: `let` for values, `var` for mutation
3. Effects at the boundary: only activities perform I/O
4. Two-tier errors: business in-language, infrastructure out-of-language
5. Agent-first: communication primitives are built-in
6. LLM-friendly: consistent patterns, JSON-native, minimal punctuation
7. Resumable: `suspend` marks persistence points

#### Task 1.2: Variable Binding & Types
**Output**: Section 4.1 of spec

Design:
- `let` for immutable bindings
- `var` for mutable bindings (discouraged)
- Type inference with optional annotations
- Primitive types: `Int`, `Long`, `Float`, `String`, `Bool`, `Duration`
- Generic types: `List<T>`, `Map<K,V>`, `Set<T>`
- Result type: `Result<T, E>`
- Optional type: `Option<T>` or `T?`

Examples to include:
```
let amount = 1000
let name: String = "Alice"
let items: List<String> = ["a", "b", "c"]
```

#### Task 1.3: Expressions & Operators
**Output**: Section 4.2 of spec

Design:
- Arithmetic: `+`, `-`, `*`, `/`, `%`
- Comparison: `==`, `!=`, `<`, `<=`, `>`, `>=`
- Logical: `&&`, `||`, `!`
- Pipeline: `|>` for chaining
- Null coalescing: `??`
- Elvis operator: `?.`
- Range: `..` and `..=`

Examples to include:
```
let result = data
  |> validate
  |> transform
  |> serialize

let name = user?.profile?.name ?? "Anonymous"
```

#### Task 1.4: Control Flow
**Output**: Section 4.3 of spec

Design:
- `if`/`else` as expressions
- `match` for pattern matching (Rust-style)
- `for`/`in` for iteration
- `while` for loops (discouraged in workflows)
- Guard clauses with `if` in patterns

Examples to include:
```
let status = if amount > 0 { "credit" } else { "debit" }

match result {
  Ok(value) => process(value)
  Err(NotFound) => default()
  Err(e) => escalate(e)
}

for item in items {
  process(item)
}
```

#### Task 1.5: Data Structures
**Output**: Section 4.8 of spec

Design:
- Records (structural typing): `{ field: Type }`
- Named records: `record Name { fields }`
- Lists: `[a, b, c]`
- Maps: `{ "key": value }` (JSON-native)
- Destructuring in patterns
- Spread operator: `...`

Examples to include:
```
record TransferRequest {
  amount: Long
  currency: String
  sender: Account
  recipient: Account
}

let request = TransferRequest {
  amount: 1000
  currency: "USD"
  sender: { id: "123", name: "Alice" }
  recipient: { id: "456", name: "Bob" }
}

let { amount, currency } = request
```

---

### Phase 2: Effects & Error Model

#### Task 2.1: Error Model Deep-Dive
**Output**: Section 2 of spec - "Error Model Deep-Dive"

Document:
- Business errors: values, pattern-matched, in-language
- Infrastructure errors: invisible, policy-driven, out-of-language
- Why this matters (simplicity, LLM-friendliness, separation of concerns)
- Comparison with Temporal, Rust, Go error models

Design business error types:
```
error InsufficientFunds { available: Long, requested: Long }
error AccountFrozen { reason: String, until: Timestamp? }
error FraudDetected { confidence: Float, signals: List<String> }
```

#### Task 2.2: Activity Calls
**Output**: Section 4.4 of spec

Design:
- Activity syntax (looks like function call but is side-effecting)
- `suspend` keyword for persistence points
- Activity namespaces: `http.get`, `llm.chat`, `shell.exec`
- Trailing lambda for configuration

Examples to include:
```
// Simple activity
let response = http.get("https://api.example.com/users")

// With suspend (persistence point)
let result = suspend http.post("/transfers") {
  body: transferRequest
  headers: { "Authorization": "Bearer ${token}" }
}

// LLM activity
let analysis = suspend llm.chat {
  model: "gpt-4"
  messages: conversation
  temperature: 0.7
}
```

#### Task 2.3: Business Error Handling
**Output**: Section 4.5 of spec

Design:
- Result type propagation: `?` operator
- Pattern matching on results
- `map`, `flatMap`, `mapErr` combinators
- Early return with `return Err(...)`

Examples to include:
```
// Propagate with ?
fn processTransfer(req: TransferRequest) -> Result<Receipt, TransferError> {
  let validated = validate(req)?
  let balance = checkBalance(validated.sender)?
  let receipt = executeTransfer(validated)?
  Ok(receipt)
}

// Explicit matching
match checkFraud(request) {
  Ok(Safe) => proceed()
  Ok(Suspicious { confidence }) if confidence > 0.9 => {
    Err(FraudDetected { confidence })
  }
  Ok(Suspicious { confidence }) => reviewManually()
  Err(e) => Err(e)
}
```

#### Task 2.4: Infrastructure Policies
**Output**: Section 4.6 of spec

Design:
- Annotations/decorators on activities
- `@retry(count, backoff)` - retry policy
- `@timeout(duration)` - execution timeout
- `@circuit_breaker(threshold, window)` - circuit breaker
- `@rate_limit(requests, per)` - rate limiting
- Workflow-level defaults

Examples to include:
```
@retry(3, backoff: exponential(base: 1.second, max: 30.seconds))
@timeout(5.minutes)
let result = suspend http.post("/external-api") { ... }

// Workflow-level defaults
@defaults {
  retry: { count: 3, backoff: exponential }
  timeout: 30.seconds
}
workflow ProcessPayment { ... }
```

#### Task 2.5: Parallel & Concurrent Execution
**Output**: Section 4.7 of spec

Design:
- `parallel { }` block for concurrent activities
- `race { }` block for first-to-complete
- `all([...])` for waiting on multiple futures
- `any([...])` for first success

Examples to include:
```
// All in parallel, wait for all
let (users, orders, inventory) = parallel {
  fetchUsers()
  fetchOrders()
  fetchInventory()
}

// Race - first to complete wins
let result = race {
  primaryService.fetch()
  fallbackService.fetch()
}

// Parallel with named results
parallel {
  notifications <- sendEmail(user)
  audit <- logAudit(event)
  metrics <- recordMetrics(data)
}
```

---

### Phase 3: Workflow Structure

#### Task 3.1: Workflow Definition
**Output**: Section 4.9 of spec

Design:
- `workflow` keyword for defining workflows
- Input/output type declarations
- Metadata annotations
- Versioning support
- Workflow composition (call other workflows)

Examples to include:
```
@version("1.0.0")
@description("Process a wire transfer with fraud detection")
@tags(["payments", "fraud"])
workflow ProcessTransfer(request: TransferRequest) -> Result<Receipt, TransferError> {
  // workflow body
}

// Workflow with complex inputs
workflow BatchProcess {
  input {
    items: List<Item>
    options: ProcessOptions = ProcessOptions.default()
  }
  output {
    results: List<Result<Processed, ProcessError>>
    summary: BatchSummary
  }

  // body
}
```

---

### Phase 4: Agent System

#### Task 4.1: Agent Communication Deep-Dive
**Output**: Section 3 of spec - "Agent Communication Deep-Dive"

Document:
- Actor model foundations (Erlang/Akka heritage)
- Message-passing vs shared state
- Communication primitives: `send`, `ask`, `broadcast`
- Channels for pub/sub
- State isolation guarantees
- Comparison with LangChain/LangGraph patterns

#### Task 4.2: Agent Definition
**Output**: Section 4.10 of spec

Design:
- `agent` keyword for defining agent templates
- Identity, model, prompt, tools, initial state
- Tool definitions and binding
- System prompt with templating

Examples to include:
```
agent FraudAnalyst {
  model: "gpt-4"

  prompt: """
    You are a fraud detection specialist at a financial institution.
    Analyze transactions for suspicious patterns including:
    - Unusual velocity (many transactions in short time)
    - Geographic anomalies (transactions from unusual locations)
    - Amount patterns (round numbers, just-under-limit amounts)

    Always explain your reasoning.
  """

  tools: [
    checkVelocity,
    checkGeolocation,
    queryHistoricalPatterns,
    flagForReview
  ]

  state {
    analysisHistory: List<Analysis> = []
    confidenceThreshold: Float = 0.7
  }
}

// Tool definition
tool checkVelocity(accountId: String, window: Duration) -> VelocityReport {
  description: "Check transaction velocity for an account"
  // Implementation provided by runtime
}
```

#### Task 4.3: Agent Communication Primitives
**Output**: Section 4.11 of spec

Design:
- `spawn` to create agent instance
- `send(agent, message)` - fire and forget
- `ask(agent, message) -> T` - request-response
- `broadcast(channel, message)` - pub/sub
- `subscribe(channel, handler)` - receive from channel
- Message types and schemas

Examples to include:
```
// Spawn an agent
let analyst = spawn FraudAnalyst

// Fire and forget
send(analyst, AnalyzeRequest { transaction: tx })

// Request-response (suspends until reply)
let assessment = ask(analyst, AssessRisk { transaction: tx })

// Channels
let updates = channel<StatusUpdate>("transfer-updates")
broadcast(updates, StatusUpdate { phase: "validation", status: "complete" })

// Subscribe
subscribe(updates) { update ->
  log("Transfer ${update.phase}: ${update.status}")
}
```

#### Task 4.4: Multi-Agent Topologies
**Output**: Section 4.12 of spec

Design:
- `topology` block for declaring agent relationships
- Supervisor pattern with `supervise { }`
- Handoff pattern with `handoff { }` state machine
- Router pattern with `route { }`
- Swarm pattern with `swarm { }`

Examples to include:
```
// Supervisor pattern
topology ResearchTeam {
  supervisor: Coordinator
  workers: [Researcher, Analyst, Writer]

  strategy: one_for_one  // restart failed agents individually
}

let team = spawn ResearchTeam
let report = ask(team.supervisor, ResearchRequest { topic: "AI trends" })

// Handoff pattern (state machine)
topology SupportFlow {
  handoff {
    Greeter -> Authenticator -> Specialist -> Closer
  }

  on_handoff { from, to, context ->
    log("Handing off from ${from} to ${to}")
  }
}

// Router pattern
topology QueryRouter {
  router: Classifier
  specialists: {
    "technical" -> TechSupport
    "billing" -> BillingAgent
    "general" -> GeneralAssistant
  }

  merge: Synthesizer  // combines results if multiple specialists
}

// Swarm pattern
topology DebatePanel {
  swarm {
    agents: [Optimist, Skeptic, Moderator]
    channel: debate_channel
    max_rounds: 5
    consensus: majority_vote
  }
}
```

#### Task 4.5: Agent Lifecycle
**Output**: Section 4.13 of spec

Design:
- `spawn` with configuration
- `terminate(agent)` for graceful shutdown
- Supervision strategies: `one_for_one`, `one_for_all`, `rest_for_one`
- Agent pools for scaling
- Lifecycle hooks

Examples to include:
```
// Spawn with timeout
let analyst = spawn FraudAnalyst {
  timeout: 5.minutes
  on_timeout: terminate_gracefully
}

// Terminate
terminate(analyst, reason: "task_complete")

// Agent pool
let pool = spawn_pool(Worker, count: 5) {
  strategy: round_robin
  max_overflow: 10
}

// Supervision
supervise(analyst) {
  strategy: restart(max: 3, within: 1.minute)
  on_failure: { error ->
    escalate(error)
  }
}
```

---

### Phase 5: Complete Example & Grammar

#### Task 5.1: Wire Transfer Complete Example
**Output**: Section 5 of spec - "Complete Example"

Implement the full wire transfer scenario:
1. Agent definitions (FraudAnalyst, ComplianceReviewer)
2. Error type definitions
3. Workflow with all features demonstrated
4. Infrastructure policies
5. Multi-agent swarm for edge case discussion
6. Parallel notifications

This should be a comprehensive, production-quality example.

#### Task 5.2: Grammar Sketch
**Output**: Section 6 of spec - "Grammar Sketch"

Produce pseudo-BNF for all constructs:
- Program structure
- Declarations (workflow, agent, tool, error, record)
- Statements and expressions
- Pattern matching
- Activity calls
- Agent communication

#### Task 5.3: Open Questions
**Output**: Section 7 of spec - "Open Questions"

Document trade-offs needing feedback:
- Null handling: `Option<T>` vs `T?` vs both
- String interpolation syntax
- Duration literals: `5.seconds` vs `5s` vs `Duration(5, Seconds)`
- Agent state: explicit vs implicit
- Channel typing: static vs dynamic
- Workflow versioning strategy
- Import/module system

---

## Output Structure

The final specification document will be structured as:

```
relang-docs/topics/language-specification.md
├── 1. Design Principles Summary
├── 2. Error Model Deep-Dive
├── 3. Agent Communication Deep-Dive
├── 4. Syntax Reference
│   ├── 4.1 Variable Binding & Types
│   ├── 4.2 Expressions & Operators
│   ├── 4.3 Control Flow
│   ├── 4.4 Activity Calls
│   ├── 4.5 Business Error Handling
│   ├── 4.6 Infrastructure Policies
│   ├── 4.7 Parallel & Concurrent Execution
│   ├── 4.8 Data Structures
│   ├── 4.9 Workflow Definition
│   ├── 4.10 Agent Definition
│   ├── 4.11 Agent Communication
│   ├── 4.12 Multi-Agent Topologies
│   └── 4.13 Agent Lifecycle
├── 5. Complete Example
├── 6. Grammar Sketch
└── 7. Open Questions
```

---

## Verification Steps

1. [ ] All 13 constructs have syntax, examples, rationale, alternatives
2. [ ] Error model clearly distinguishes business vs infrastructure
3. [ ] Agent patterns (supervisor, handoff, router, swarm) all have syntax
4. [ ] Wire transfer example compiles conceptually (consistent syntax)
5. [ ] Grammar sketch covers all constructs
6. [ ] Open questions identify real trade-offs

---

## Dependencies

- None (specification only, no implementation dependencies)

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Syntax inconsistencies across sections | Medium | High | Final consistency pass before completion |
| Over-engineering agent patterns | Medium | Medium | Focus on 4 core patterns, defer advanced |
| Grammar too informal | Low | Medium | Include enough detail for future parser |

---

## Estimated Effort

| Phase | Sections | Complexity |
|-------|----------|------------|
| Phase 1: Foundations | 1, 4.1-4.4, 4.8 | Medium |
| Phase 2: Effects & Errors | 2, 4.4-4.7 | High |
| Phase 3: Workflows | 4.9 | Medium |
| Phase 4: Agents | 3, 4.10-4.13 | High |
| Phase 5: Example & Grammar | 5-7 | Medium |

---

## Notes

- This is a **specification document**, not implementation
- Style: Kotlin-Rust hybrid (braces, optional semicolons, expression-oriented)
- All examples should be syntactically consistent with each other
- The document will serve as the foundation for future ANTLR grammar and Truffle implementation

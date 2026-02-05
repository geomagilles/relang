# Language Design Principles

Core principles from programming language theory for evaluating ReLang design decisions.

## Type System Design

### Structural vs Nominal Typing

| Approach | When to Use | ReLang Application |
|----------|-------------|-------------------|
| **Structural** | Data shapes, interop, flexibility | Product types (`A & B`), sum types (`A \| B`) |
| **Nominal** | Domain types, semantic meaning | Sealed error families, action types |

ReLang uses **structural types for data** (`&`, `|`) and **nominal types for effects** (action families).

### Type Inference

**Bidirectional inference** balances ergonomics and clarity:
- Local variables: infer from initialization
- Function parameters: require explicit types
- Return types: require explicit types (for documentation)

```relang
let x = 42                    // inferred: Int
fn add(a: Int, b: Int): Int   // explicit signatures
```

### Algebraic Data Types

**Products** (`&`): All components present
```relang
User & Orders              // Has both User AND Orders
```

**Sums** (`|`): Exactly one alternative
```relang
Success | Failure          // Either Success OR Failure
```

ReLang uses `&` and `|` as **data operators**, distinct from `and`/`or` coordination operators.

## Effect Systems

### Tracking Effects in Types

ReLang tracks async effects via `*T`:
- `T` — pure value, always available
- `*T` — in-flight effect, may succeed or fail

This makes effect boundaries **lexically visible**.

### Effect Composition

Effects compose via coordination operators:
- `*A and *B` — both effects, fail-fast
- `*A or *B` — either effect, fail-last

Type-level composition:
```relang
*A and *B : *(A & B)    // Product of results
*A or  *B : *(A | B)    // Sum of results
```

## Error Handling Strategies

### Approaches Compared

| Approach | Pros | Cons | ReLang Choice |
|----------|------|------|---------------|
| Exceptions | Concise happy path | Hidden control flow | No |
| Result types | Explicit, composable | Verbose | Partial (Failure) |
| Union returns | Clear alternatives | Complex signatures | Yes (`T \| Failure`) |
| Error codes | Simple | Easy to ignore | No |

ReLang uses **union returns with a single failure channel**:
```relang
await e : T | Failure
```

### Sealed Error Families

Each action family defines exhaustive error cases:
```relang
sealed HttpError
struct Timeout : HttpError { duration: Duration }
struct DnsFailure : HttpError { host: String }
```

Benefits:
- Exhaustive pattern matching
- IDE can suggest all cases
- New errors require version bump

## Control Flow Design

### Awaitable Model (Not Structured Concurrency)

ReLang awaitables are **values**, not scoped code blocks:

| Structured Concurrency | ReLang Awaitables |
|------------------------|-------------------|
| Operations bound to lexical scope | Awaitables are first-class values |
| Automatic cleanup on scope exit | Explicit coordination via `and`/`or` |
| No orphans by construction | Awaitables can be passed around freely |

ReLang's coordination model:
- **Single execution point**: Workflow code has one "program counter" at all times
- Parallelism is external (awaitables in-flight), not internal (no concurrent code paths)
- `and`/`or` define cancellation behavior (cancel losers)
- `shield()` controls cancellation propagation
- No implicit scope binding — awaitables are values you manage explicitly
- Child workflows (future feature) will provide hierarchical structure

### Pattern Matching

First-class pattern matching for:
- Sum type discrimination
- Product destructuring
- Error handling

```relang
match result {
  (a & b) => ...           // Destructure product
  err: Failure => ...      // Match failure
}
```

## Syntax Design Heuristics

### Huffman Coding Principle

Common operations should have short syntax:
- `!` for failure propagation (very common)
- `.await()` for resolution (common)
- `shield(t)` for cancellation control (less common)

### Familiarity vs Innovation

| Feature | Familiar Syntax | Innovation Budget |
|---------|-----------------|-------------------|
| Variables | `let x = ...` | Low |
| Functions | `fn name(...): T` | Low |
| Pattern match | `match x { ... }` | Medium |
| Awaitables | `*T`, `and`, `or` | High (novel concept) |

Spend innovation budget on **novel concepts**, use familiar syntax elsewhere.

### Sigil Design

ReLang sigils:
- `*` — awaitable type (pointer-like, "points to future")
- `&` — product type (reference-like, "this AND that")
- `|` — sum type (union, "this OR that")
- `!` — propagate failure (bang = might explode)

Good sigils have **mnemonic value** and **visual distinctiveness**.

## Composition Principles

### Orthogonality

Features should combine freely without special cases:
```relang
timeout(retry(http.get(url), policy), 5s)   // Composes
retry(t1 and t2, policy)                     // Invalid: clear restriction
```

### Closure Under Operations

Types should be closed under language operations:
- `*A and *B : *(A & B)` — awaitables produce awaitables
- `(A & B) | Failure` — products and sums compose

### Referential Transparency

Where possible, expressions should be substitutable:
```relang
let t = http.get(url)
await t ≡ await http.get(url)   // Equivalent (for pure scheduling)
```

## Tooling Considerations

### Parse-ability

Design syntax that's:
- Unambiguous (no lookahead needed)
- Recoverable (can parse partial/broken code)
- Incrementally parseable

### IDE Support

Consider:
- Syntax highlighting (distinct token types)
- Auto-completion (predictable continuations)
- Error messages (localized, actionable)
- Refactoring (rename-safe identifiers)

### Error Messages

Good error messages:
- Point to exact location
- Explain what went wrong
- Suggest fixes

Design syntax to enable good errors:
- Required delimiters at recovery points
- Distinctive keywords for disambiguation

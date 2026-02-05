---
name: relang-designer
description: Expert consultant for designing ReLang programming language syntax and semantics. Use when making language design decisions, evaluating syntax alternatives, designing new features, or analyzing trade-offs for ReLang's durable execution primitives, awaitable types, coordination operators, and failure model.
---

# ReLang Language Design Consultant

Expert guidance for designing ReLang syntax and semantics, drawing on modern PL theory and distributed systems expertise.

## Design Phase Context

**ReLang is in active early design.** This means:
- **No backward compatibility** — Change anything that needs changing
- **Spec is a starting point** — `tmp/relang-awaitables-final.md` is base work, not final
- **Bold decisions encouraged** — Don't preserve something just because it exists
- **Get it right > Get it done** — Better to redesign now than accumulate debt

## Quick Reference

| Domain | Key Considerations | Reference |
|--------|-------------------|-----------|
| Type Systems | Structural vs nominal, inference, variance | [LANGUAGE-DESIGN-PRINCIPLES.md](LANGUAGE-DESIGN-PRINCIPLES.md) |
| Concurrency | Awaitables as values, cancellation, composition | [DISTRIBUTED-SYSTEMS.md](DISTRIBUTED-SYSTEMS.md) |
| ReLang Context | Current spec, constraints, design commitments | [RELANG-CONTEXT.md](RELANG-CONTEXT.md) |
| Syntax Patterns | Established patterns from modern languages | [SYNTAX-PATTERNS.md](SYNTAX-PATTERNS.md) |

## Design Workflow

Copy this checklist when evaluating language design decisions:

```
Design Evaluation:
- [ ] Step 1: Clarify the problem and constraints
- [ ] Step 2: Survey existing solutions in modern languages
- [ ] Step 3: Evaluate against ReLang's design principles
- [ ] Step 4: Analyze distributed systems implications
- [ ] Step 5: Propose concrete syntax alternatives
- [ ] Step 6: Assess trade-offs and edge cases
- [ ] Step 7: CONSISTENCY ANALYSIS — Identify and implement ripple effects
- [ ] Step 8: Document decision rationale
```

### Step 1: Clarify the problem

Before proposing syntax:
- What user problem does this solve?
- What are the semantic requirements?
- What existing ReLang features interact with this?

### Step 2: Survey existing solutions

Check how these languages handle similar problems:
- **Rust**: Ownership, traits, pattern matching, `?` operator
- **Kotlin**: Coroutines, null safety, sealed classes
- **Swift**: Optionals, async/await, result types
- **TypeScript**: Structural typing, union/intersection types
- **Go**: Goroutines, channels, error handling
- **Temporal/Cadence**: Workflow primitives, activities
- **Unison**: Content-addressed code, abilities
- **Akka/Actor Model**: Message-passing concurrency, supervision hierarchies, location transparency

### Step 3: Evaluate against ReLang principles

Core design commitments (see [RELANG-CONTEXT.md](RELANG-CONTEXT.md)):
- Resumable execution (checkpoint-based, not replay-based)
- Explicit effect boundaries (`*T` awaitable types)
- Fail-fast coordination (`and`), fail-last racing (`or`)
- Single failure channel (`T | Failure`)
- Cancellation as coordination control, not business logic

### Step 4: Analyze distributed implications

Consider (see [DISTRIBUTED-SYSTEMS.md](DISTRIBUTED-SYSTEMS.md)):
- Serializability of checkpoint state
- Resume behavior from any checkpoint
- Partial failure modes
- Cancellation propagation
- Timeout behavior

### Step 5: Propose syntax alternatives

For each alternative:
- Show concrete syntax examples
- Explain typing rules
- Demonstrate composition with existing features

### Step 6: Assess trade-offs

Evaluate along these dimensions:

| Dimension | Questions |
|-----------|-----------|
| Readability | Is intent clear at the call site? |
| Writability | Is it ergonomic for common cases? |
| Learnability | Does it follow familiar patterns? |
| Composability | Does it compose with existing features? |
| Correctness | Does it prevent misuse? |
| Performance | Any runtime overhead? |
| Tooling | Can editors/IDEs support it well? |

### Step 7: Consistency Analysis (CRITICAL)

**Every change has consequences. Identify them. Implement them.**

When a design decision is validated, systematically analyze its impact across all language areas:

```
Consistency Impact Checklist:
- [ ] Type system: Does this change typing rules elsewhere?
- [ ] Operators: Does this affect precedence or associativity?
- [ ] Keywords: Does this introduce ambiguity with existing keywords?
- [ ] Pattern matching: Can this be destructured? How?
- [ ] Error handling: What failures can occur? Which family?
- [ ] Cancellation: How does this interact with cancel/shield?
- [ ] Coordination: How does this compose with and/or?
- [ ] Timeouts: Can timeout() wrap this? What happens?
- [ ] Retries: Can retry() wrap this? What happens?
- [ ] Serialization: Is the state serializable for checkpointing?
- [ ] Existing syntax: Are there parallel constructs that need updating?
```

**Ripple Effect Analysis**:

| If you change... | Check impact on... |
|------------------|-------------------|
| A type rule | All expressions that produce that type |
| An operator | Precedence table, all existing combinations |
| A keyword | Parser grammar, reserved word list |
| Error behavior | All action families, failure handling patterns |
| Cancellation semantics | and/or defaults, shield behavior |
| Awaitable interface | All coordination operators |

**Consistency Enforcement Rule**:

> If a validated change implies modifications elsewhere in the language, those modifications are **part of the change**, not optional follow-ups. The change is incomplete until all consequences are addressed.

Example: Adding a new coordination operator `first(t1, t2)`:
1. Define typing rules → `*A first *B : *(A | B)`
2. **Consequence**: Must define await result → `A | B | Failure`
3. **Consequence**: Must define cancellation behavior → cancels loser
4. **Consequence**: Must define failure semantics → fail-fast or fail-last?
5. **Consequence**: Must define interaction with shield → `first(shield(t1), t2)`
6. **Consequence**: Must define interaction with timeout → `timeout(first(t1, t2), d)`
7. **Consequence**: Must update precedence table → where does `first` sit?
8. **Consequence**: Must check for redundancy → is this different from `or`?

All 8 items must be resolved before the change is complete.

### Step 8: Document decision

Record:
- Problem statement
- Alternatives considered
- Chosen approach and rationale
- Rejected alternatives and why
- **Consistency changes implemented** (list all ripple effects addressed)

## Design Principles

### Consistency Principles (PARAMOUNT)

1. **Uniform behavior**: Similar constructs behave similarly
2. **No special cases**: If X works with Y, it works with all Y-like things
3. **Predictable composition**: `f(g(x))` behaves as `f` applied to result of `g`
4. **Complete changes**: A change includes ALL its consequences

**The Consistency Test**: Can a developer who knows feature A predict how feature B works?

### Syntax Principles

1. **Explicit over implicit**: Make effects visible (`*T` not hidden async)
2. **Composition over configuration**: Small orthogonal features that combine
3. **Parse don't validate**: Use types to make illegal states unrepresentable
4. **Pit of success**: Make correct code easy, incorrect code hard
5. **Syntactic consistency**: Same meaning → same syntax pattern

### Semantic Principles

1. **Resumability**: Execution can continue from any checkpoint
2. **Structural clarity**: Types should reflect data shape
3. **Single responsibility**: Each operator does one thing well
4. **Graceful degradation**: Failures are data, not exceptions
5. **Semantic consistency**: Same syntax pattern → analogous semantics

### Distributed Systems Principles

1. **No hidden network calls**: All I/O is explicit via actions
2. **Assume partial failure**: Every operation can fail
3. **Cancellation is advisory**: Best-effort, not guaranteed
4. **Timeouts are policies**: Orchestration control, not action behavior

## Common Design Questions

### "Should this be a keyword or a function?"

**Keyword** when:
- It affects control flow fundamentally
- It has special scoping rules
- It cannot be expressed as a library function

**Function** when:
- It can be implemented in terms of primitives
- Users might want to customize behavior
- It follows standard calling conventions

### "Should this be an operator or a method?"

**Operator** when:
- It's a fundamental composition mechanism
- It has well-established mathematical precedent
- Chaining is the common use case

**Method** when:
- It's a query or transformation
- It operates on a single value
- Standard OO conventions apply

### "How should errors be represented?"

Follow ReLang's failure model:
- `await` returns `T | Failure`
- Action families define sealed error sets
- Pattern matching for handling
- `\!` for propagation

## Anti-patterns to Avoid

| Anti-pattern | Problem | Alternative |
|--------------|---------|-------------|
| Implicit async | Hidden suspension points | Explicit  types |
| Exception hierarchies | Unclear what can be thrown | Sealed error families |
| Global error handlers | Non-local reasoning | Pattern matching at call site |
| Stringly-typed APIs | No compile-time safety | Structured types |
| Magic syntax | Hard to learn/remember | Consistent patterns |
| **Partial changes** | Inconsistent language state | Complete all ripple effects |
| **Special cases** | Unpredictable behavior | Uniform rules |
| **"We'll fix it later"** | Technical debt, user confusion | Address now or don't ship |
| **Preserving for preservation's sake** | Suboptimal design | Change what needs changing |
| **Spec as constraint** | Limits better solutions | Spec is starting point, not law |

## Validation Questions

Before finalizing any design:

Design Validation:
- [ ] Does it support checkpoint/resume?
- [ ] Is state serializable for checkpointing?
- [ ] Does it compose with and/or/shield/timeout/retry?
- [ ] Is the failure mode clear?
- [ ] Can it be cancelled?
- [ ] Is the syntax consistent with existing ReLang?
- [ ] Have edge cases been considered?

Consistency Validation (REQUIRED):
- [ ] Have ALL ripple effects been identified?
- [ ] Have ALL ripple effects been implemented/specified?
- [ ] Does similar syntax elsewhere behave analogously?
- [ ] Can users predict this behavior from existing knowledge?
- [ ] Are there any special cases? (If yes, justify or eliminate)
- [ ] Has the spec been updated in ALL affected sections?

**A design is NOT complete until all consistency validations pass.**

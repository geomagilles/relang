# ReLang Language Design TODOs

## All Proposals Complete ✓

All 23 proposal documents have been written. The language design specification is complete.

---

## Core Language Specification

| Proposal | Description |
|----------|-------------|
| `relang-awaitables-final.md` | Awaitable types, coordination, failure, cancellation, timeouts, retries |
| `relang-types-proposal.md` | User-defined types, sealed groups, schema imports |
| `relang-primitives-proposal.md` | Int, Float, Bool, String, List, Json, Bytes, Duration, Timestamp |
| `relang-equality-proposal.md` | Equality semantics |
| `relang-null-safety-proposal.md` | Optional types, none handling |
| `relang-conditionals-proposal.md` | if, match, pattern matching |
| `relang-collections-proposal.md` | List operations |
| `relang-lambdas-proposal.md` | Inline anonymous functions (can capture scope, no type) |
| `relang-functions-proposal.md` | Named functions (self-contained, no scope capture) |
| `relang-variables-proposal.md` | Variable bindings, let/const, shadowing, scope |
| `relang-loops-proposal.md` | for-in, while, loop control (break, continue) |
| `relang-operators-proposal.md` | Complete operator reference, precedence table |
| `relang-modules-proposal.md` | Module system, file organization, visibility |

## Durable Execution Model

| Proposal | Description |
|----------|-------------|
| `relang-workflows-proposal.md` | Durable execution, `fn` + `spawn`, `self` introspection |
| `relang-actions-proposal.md` | Action families, activity definitions, side effects |
| `relang-scheduling-proposal.md` | Coordination execution order, eager/lazy, hedging |
| `relang-determinism-proposal.md` | Snapshot execution model, checkpoints, state capture |
| `relang-idempotency-proposal.md` | Action identity, deduplication, idempotency keys |
| `relang-lifecycle-proposal.md` | Awaitable introspection, state queries |

## Advanced Features

| Proposal | Description |
|----------|-------------|
| `relang-concurrency-proposal.md` | Resource limits, parallelism caps, rate limiting |
| `relang-error-evolution-proposal.md` | Sealed family versioning, backward compatibility |
| `relang-signals-proposal.md` | External signals, workflow interruption |
| `relang-timers-proposal.md` | Sleep, scheduled execution, cron |

---

## Original TODO Items (All Addressed)

| Item | Resolution |
|------|------------|
| Scheduling semantics | `relang-scheduling-proposal.md` |
| Retry model | `relang-awaitables-final.md` (Retries section) |
| Idempotency/deduplication | `relang-idempotency-proposal.md` |
| Timeout primitives | `relang-awaitables-final.md` (Timeouts section) |
| Resource/concurrency limits | `relang-concurrency-proposal.md` |
| Determinism boundary | `relang-determinism-proposal.md` |
| Error payload stability | `relang-error-evolution-proposal.md` |
| Task lifecycle introspection | `relang-lifecycle-proposal.md` |

---

## Consistency Fixes Applied

The following inconsistencies were identified and fixed across all documents:

1. **`or` failure handling**: Standardized to return single `Failure` with `AggregateFailure` payload (not `[Failure]`)
2. **`struct` vs `type` keyword**: Standardized on `type` throughout
3. **Lambda syntax**: Standardized on brace syntax `{ x -> x * 2 }` (not pipe syntax `|x| x * 2`)
4. **`None`/`none` capitalization**: Standardized on lowercase `none` for the value
5. **`!` operator**: Unified semantics for optional unwrap and failure propagation
6. **`and`/`or` disambiguation**: Type-based (data vs coordination) clarified in primitives
7. **Functions are self-contained**: All `fn` can only access parameters (no scope capture). Lambdas are the only construct that can capture from enclosing scope.
8. **No function types**: Lambdas have no type, cannot be stored or passed. Higher-order functions removed from `relang-functions-proposal.md`.
9. **Top-level only**: All functions must be defined at file level. Nested functions are not allowed.
10. **`spawn` keyword**: Removed `*fn` syntax. Now one function type (`fn`) with two calling modes:
    - `f()` — inline execution, returns `T`
    - `spawn f()` — parallel execution, returns `*T` (awaitable)
11. **`self` introspection**: Replaced `workflow.id`, `workflow.startedAt`, `workflow.parentId` with `self.id`, `self.createdAt`, `self.parentId`
12. **`now()` primitive**: Replaced `workflow.now()` with `now()` as a language primitive

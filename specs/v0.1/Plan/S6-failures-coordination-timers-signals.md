# S6 - Canonical Failure Model, Coordination, Timers, and Signals

## Objective

Stabilize failure and coordination semantics, which are essential for correct durable orchestration.

## Scope

IN:

- canonical `Failure` envelope.
- inline failure propagation.
- `and`/`or` awaitables.
- `AllFailed` (lexical order).
- `timer(...)`, `receive<T>()`.

OUT:

- `FunctionFailed` wrapping via `spawn` (S7).
- full external action families (S8+).

## Spec References

- `relang-failures-proposal.md`
- `relang-awaitables-proposal.md`
- `relang-timers-proposal.md`
- `relang-signals-proposal.md`

## Implementation Sequence

1. Implement runtime types `Failure`, `FailureKind`, `ExecutionRef`.
2. Validate invariants:
   - `cause` xor `causes`,
   - kind-specific constraints.
3. Implement `x!` propagation (same failure in inline execution).
4. Implement coordination:
   - `and` fail-fast,
   - `or` first-success,
   - `AllFailed` when all fail.
5. Guarantee lexical order in `AllFailed.causes`.
6. Implement `timer(Duration|Timestamp)`:
   - persist `fireAt`.
7. Implement `receive<T>()`:
   - durable awaitable,
   - resume after snapshot.
8. Add read-only awaitable introspection API.

## Explicit Error Management and DevEx Tasks

1. Standardize coordination diagnostics:
   - `and` fail-fast,
   - `or` all-failed,
   - timeout/signal errors.
2. Make root cause explicit:
   - `Failure.kind`,
   - `cause`/`causes`,
   - preserved lexical order.
3. Add contextual `help:` guidance:
   - manual retry,
   - fallback via `or timer(...)`,
   - expected signal guidance.
4. Add secondary notes:
   - source arm/awaitable that produced the failure.
5. Add `Failure` UX tests:
   - readable message without runtime-object internals.

## Deliverables

- v0.1-compliant failure semantics.
- Stable coordination behavior.
- Durable timers/signals.

## Mandatory Tests

- `and` returns first observed failure.
- `or` returns first success.
- `or` all fail -> lexically ordered `AllFailed`.
- timeout pattern with `or`.
- signal receive + resume.

## Risks and Safeguards

Risk: confusion between domain errors and runtime `Failure`.

Safeguard:

- explicit tests for domain vs infrastructure errors.

Risk: non-deterministic cause ordering under concurrency.

Safeguard:

- final ordering by source order, not runtime arrival order.

## Definition of Done

- Failure model and coordination validated by conformance tests.
- timers/signals operational with resume.

## End-of-Sprint Governance Gate

Mandatory before closure:

- `spec-delta` report: `Governance/04-spec-delta-review.md`.
- Conformance matrix update (status for touched requirements).
- Open-risk validation (accepted/replanned/fixed).

## Additional S6 Gate (Snapshot RFC Policy)

- Activate policy `Governance/05-snapshot-rfc-policy.md`.
- Require an RFC for every persisted schema change.
- Add CI control (or PR checklist gate) that blocks changes without RFC reference.

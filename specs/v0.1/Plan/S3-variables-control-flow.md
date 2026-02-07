# S3 - Variables, Scope, and Basic Control Flow

## Objective

Stabilize binding/scope rules and the core control flow needed for real application code.

## Scope

IN:

- `let`, type-safe local reassignment, shadowing.
- `if` expression, minimal `match`.
- Basic loops (`for`, `while`) without advanced features.

OUT:

- Complex sealed exhaustiveness.
- Advanced smart casts.
- Awaitables inside loops (later phase).

## Spec References

- `relang-variables-proposal.md`
- `relang-conditionals-proposal.md`
- `relang-loops-proposal.md`

## Implementation Sequence

1. Map variables to Truffle `FrameDescriptor` / slots.
2. Implement nested lexical scopes.
3. Implement reassignment with stable static type checks.
4. Implement explicit shadowing by scope.
5. Implement `if` as expression:
   - compatible branch types,
   - implicit `else` as `Unit` for statement usage.
6. Implement minimal `match`:
   - literals,
   - wildcard `_`,
   - `none`.
7. Implement loops:
   - `for ... in ...`,
   - `while Bool`,
   - `break`, `continue`.
8. Add diagnostics:
   - unknown variable,
   - out-of-scope usage,
   - reassignment type mismatch.

## Explicit Error Management and DevEx Tasks

1. Add dedicated binding diagnostics for:
   - unknown variable,
   - out-of-scope variable,
   - incompatible reassignment.
2. Include declaration-site notes when available.
3. Control diagnostic noise:
   - deduplicate by `(code, range)`,
   - avoid cascades after `UnknownType` recovery.
4. Add priority fix hints:
   - suggest `let <name> = ...` for missing variable.
5. Add control-flow UX tests:
   - `break/continue` outside loops with clear message and `help:`.

## Deliverables

- Stable binding engine.
- Control-flow AST nodes.
- Scope behavior tests.

## Mandatory Tests

- shadowing does not leak outside block.
- reassignment respects initial type.
- `match` with `_` works.
- `while` rejects non-bool condition.
- `break/continue` outside loops -> compile error.

## Risks and Safeguards

Risk: inconsistent frame state in loops.

Safeguard:

- tests with nested loops and same-name shadowing.

Risk: ambiguous `match` behavior.

Safeguard:

- document and test arm evaluation priority.

## Definition of Done

- Deterministic scope/variable behavior.
- Stable basic control flow.
- Precise compile-time errors.

## End-of-Sprint Governance Gate

Mandatory before closure:

- `spec-delta` report: `Governance/04-spec-delta-review.md`.
- Conformance matrix update (status for touched requirements).
- Open-risk validation (accepted/replanned/fixed).

## Additional S3 Gate (Conformance Matrix)

- Create `conformance-matrix-v0.1.csv` per `Governance/02-conformance-matrix.md`.
- Link each implemented requirement to at least one test.
- Add a CI check that fails if a touched `MUST` requirement has no status.

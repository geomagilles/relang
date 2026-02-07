# S2 - Primitive and Optional Types

## Objective

Deliver a reliable base type system to avoid structural errors in later sprints.

## Scope

IN:

- `Int`, `Float`, `Bool`, `String`, `Bytes`, `Duration`, `Timestamp`, `Json`, `Unit`.
- `T?` and `none`.
- Core primitive operators.
- Fundamental type errors.

OUT:

- Advanced unions/products.
- Full type narrowing.
- Awaitables.

## Spec References

- `relang-spec-v0.1-canonique.md` (types section).
- `relang-primitives-proposal.md`.
- `relang-null-safety-proposal.md`.

## Implementation Sequence

1. Define the internal type model (`RelangType`).
2. Add `OptionalType(innerType)`.
3. Encode `none` as a distinct language value.
4. Implement checks:
   - strict assignment,
   - no implicit `Int <-> Float` coercion.
5. Implement operators:
   - base arithmetic,
   - same-type comparison,
   - cross-type rejection.
6. Implement type check for `if` condition as `Bool`.
7. Add detailed diagnostics (message + position + suggestion).
8. Add null-safety non-regression tests.

## Explicit Error Management and DevEx Tasks

1. Assign stable diagnostic codes to S2 primitive type errors (`RL2xxx`).
2. Standardize wording:
   - `Type mismatch: expected <Expected>, found <Actual>.`
   - no runtime-internal jargon in user messages.
3. Add actionable `help:` for common failures:
   - invalid `none` assignment,
   - non-bool condition,
   - rejected implicit coercion.
4. Add contextual notes when relevant:
   - declared type vs observed type.
5. Add S2 diagnostic snapshots:
   - at least one snapshot per primitive error family.

## Deliverables

- Stable primitive type checker.
- Runtime values for primitives and `none`.
- Usable error messages.

## Mandatory Tests

Positive:

- valid primitive declarations.
- `String?` with `none`.
- `??` (if active in S2) or explicit placeholder rejection.

Negative:

- assign `none` to non-optional type.
- cross-type comparison without conversion.
- non-bool `if` condition.

## Risks and Safeguards

Risk: confusion between `Unit` and `none`.

Safeguard:

- dedicated tests for distinct semantics.

Risk: accidental introduction of implicit conversions.

Safeguard:

- strict tests that reject implicit coercion.

## Definition of Done

- All S2 primitive spec cases pass.
- Type errors are stable and readable.
- No unspecified implicit behavior.

## End-of-Sprint Governance Gate

Mandatory before closure:

- `spec-delta` report: `Governance/04-spec-delta-review.md`.
- Conformance matrix update (status for touched requirements).
- Open-risk validation (accepted/replanned/fixed).

## Additional S2 Gate (Runtime Contract)

- Publish `runtime-contract-v1` per `Governance/01-runtime-contract.md`.
- Add baseline schema compatibility tests (read/write).
- Block any contract evolution without explicit versioning.

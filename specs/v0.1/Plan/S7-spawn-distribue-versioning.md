# S7 - `spawn`, Distributed Boundary, and Resume Versioning

## Objective

Introduce the distributed execution boundary without breaking the durability contract.

## Scope

IN:

- `spawn f(...)` -> `*T`.
- child execution with `self.id`, `self.parentId`.
- parent-side wrapping `FunctionFailed(cause=...)`.
- minimal `codeVersion` / `stateVersion` policy.

OUT:

- complex production multi-region scheduler.
- full multi-version migration.

## Spec References

- `relang-spec-v0.1-canonique.md` (spawn + versioning)
- `relang-execution-model.md`
- `relang-failures-proposal.md`

## Implementation Sequence

1. Implement child-launch plan:
   - new execution id,
   - parentId set,
   - serialized args.
2. Implement local scheduler, then abstract distributed scheduler.
3. Implement `await spawn`:
   - child success -> value,
   - child failure -> `FunctionFailed` with `cause`.
4. Implement best-effort cancellation of losing children in `or`.
5. Add version metadata:
   - `codeVersion` attached to execution,
   - `stateVersion` on snapshot.
6. On resume:
   - same `codeVersion` -> OK,
   - otherwise explicit refusal (until migration exists).
7. Add state migration hooks (stubs) for S8+.

## Explicit Error Management and DevEx Tasks

1. Add dedicated distributed diagnostics for:
   - child `FunctionFailed`,
   - `codeVersion/stateVersion` incompatibility,
   - cross-version resume refusal.
2. Add parent/child correlation in errors:
   - `self.id`,
   - `parentId`,
   - child execution id.
3. Add operations-oriented `help:`:
   - migration required,
   - version rollback,
   - clean re-run strategy.
4. Ensure cross-boundary errors preserve:
   - stable code,
   - readable causality.
5. Add DX regression tests for parent/child/grandchild chains.

## Deliverables

- Observable parent/child execution pipeline.
- Compliant cross-boundary propagation.
- Active versioning gate.

## Mandatory Tests

- parent spawn child success.
- parent spawn child failure -> `FunctionFailed` wrapper.
- parent->child->grandchild chain preserves cause.
- resume rejected on incompatible `codeVersion`.

## Risks and Safeguards

Risk: confusion between inline and distributed execution.

Safeguard:

- mirrored tests:
  - inline `f(...)`,
  - `await spawn f(...)`,
  compare failure shape.

Risk: tight coupling to scheduler implementation.

Safeguard:

- injectable scheduler interface + test fake.

## Definition of Done

- `spawn` is stable and compliant.
- Distributed execution boundary is observable and tested.

## End-of-Sprint Governance Gate

Mandatory before closure:

- `spec-delta` report: `Governance/04-spec-delta-review.md`.
- Conformance matrix update (status for touched requirements).
- Open-risk validation (accepted/replanned/fixed).

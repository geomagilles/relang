# ReLang v0.1 Implementation Plan on Truffle

This folder contains an S1 to S11 execution plan organized by feature, with enough detail to delegate implementation to a team.

## Architecture Assumptions (Locked)

- Runtime: classic Truffle AST (no Bytecode DSL at the beginning).
- Execution model: snapshot-first, without full application replay.
- Durability: introduced early (S5) and extended later.
- Priority normative contract:
  - `relang-spec-v0.1-canonique.md`
  - `relang-failures-proposal.md`

## Cross-Phase Delivery Rules

- No feature is considered done without:
  - parser + type checker + runtime + tests.
- Any `await`-related feature must include:
  - checkpoint/resume tests,
  - `Failure` propagation tests.
- Any runtime schema evolution must include:
  - explicit versioning,
  - migration test or explicit resume-rejection test.
- Mandatory end-of-sprint gate:
  - `spec-delta` review,
  - conformance matrix update,
  - open-risk review.

## Backlog Convention

Each Sx phase includes:

1. Objective.
2. Scope IN / OUT.
3. Implementation sequence (strict order).
4. Explicit Error Management and DevEx Tasks.
5. Expected deliverables.
6. Mandatory tests.
7. Risks and safeguards.
8. Definition of Done (DoD).

## Critical Milestones

- M1 (end of S4): stable synchronous language (without full awaitables).
- M2 (end of S6): durable core (`await`, checkpoint/resume, failures, coordination, timer/signal).
- M3 (end of S7): stable distributed `spawn` boundary.
- M4 (end of S10): major action families + observability + hardening.
- M5 (end of S11): diagnostics and developer experience at world-class quality bar.

## Success Multipliers

1. Create a conformance suite in S2 and extend it every sprint.
2. Add a runtime golden trace in S5 to compare state transitions.
3. Add a sprint-end `spec-delta` review:
   - what is compliant,
   - what diverges,
   - explicit decision (accept or fix).
4. Isolate a `runtime-contract` module (snapshot types, failure envelope, execution refs) early.
5. Forbid structural snapshot refactors after S6 without an internal RFC.

## Governance Pack (Mandatory)

- `Governance/01-runtime-contract.md`
- `Governance/02-conformance-matrix.md`
- `Governance/03-golden-resume-tests.md`
- `Governance/04-spec-delta-review.md`
- `Governance/05-snapshot-rfc-policy.md`
- `Governance/06-diagnostic-quality-playbook.md`

## Planning Integration

- S2: version and lock `runtime-contract`.
- S3: activate conformance matrix as a gate.
- S5: enforce golden resume tests.
- S6+: enforce snapshot RFC policy.
- S1-S10: enforce `spec-delta` review at sprint end.
- S11: enforce diagnostic quality playbook + diagnostic snapshots as merge gates.

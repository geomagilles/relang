# S5 - Awaitables v1 and Early Checkpoint/Resume

## Objective

Introduce the durable core early to validate snapshot/resume feasibility before distributed complexity is added.

## Scope

IN:

- type `*T`.
- `await`.
- runtime awaitable table.
- checkpoint on suspension.
- resume from latest snapshot.

OUT:

- full distributed `spawn`.
- complete action families.
- advanced `and/or` coordination (arrives in S6).

## Spec References

- `relang-spec-v0.1-canonique.md` (execution + checkpoints)
- `relang-execution-model.md`
- `relang-awaitables-proposal.md`

## Implementation Sequence

1. Define internal `AwaitableHandle`:
   - id,
   - createdAt,
   - status,
   - result/failure.
2. Implement type check rule `await : *T -> T | Failure`.
3. Implement runtime suspension:
   - unresolved awaitable -> checkpoint,
   - resume continues execution.
4. Define snapshot schema v1:
   - execution metadata,
   - serialized locals,
   - program counter,
   - awaitable table.
5. Implement snapshot persistence (chosen backend).
6. Implement resume:
   - reload snapshot,
   - restore variables,
   - continue at correct program counter.
7. Guarantee no re-execution of already-resolved effects.
8. Add testing tools:
   - inject crash after checkpoint N,
   - resume and verify same final result.

## Explicit Error Management and DevEx Tasks

1. Define S5 runtime diagnostics (`RL3xxx`) for:
   - invalid `await`,
   - corrupted/incomplete snapshot,
   - impossible resume.
2. Attach clear remediation to each resume error:
   - restart from valid checkpoint,
   - verify code/schema version.
3. Include minimal runtime context in errors:
   - executionId,
   - checkpoint id/index when available.
4. Add readable CLI/LSP rendering for resume failures:
   - code,
   - message,
   - help,
   - position/source when available.
5. Add golden error tests:
   - crash/resume with stable, non-regressing diagnostics.

## Deliverables

- Minimal operational durable runtime.
- Versioned snapshot format (v1).
- Crash/resume test harness.

## Mandatory Tests

- `await` suspends and resumes correctly.
- already resolved results are not replayed.
- locals are restored exactly.
- snapshot corruption is detected with clear diagnostics.

## Risks and Safeguards

Risk: trying to serialize non-serializable Truffle runtime objects.

Safeguard:

- strict separation between:
  - serializable language state,
  - ephemeral runtime objects reconstructed on resume.

Risk: behavior divergence after resume.

Safeguard:

- golden tests: "full run" vs "run with crash/resume".

## Definition of Done

- Stable demonstration of the full cycle:
  - start,
  - suspend/checkpoint,
  - crash,
  - resume,
  - complete.

## End-of-Sprint Governance Gate

Mandatory before closure:

- `spec-delta` report: `Governance/04-spec-delta-review.md`.
- Conformance matrix update (status for touched requirements).
- Open-risk validation (accepted/replanned/fixed).

## Additional S5 Gate (Golden Resume Tests)

- Set up the crash-injection harness.
- Create golden traces for simple and chained `await` scenarios.
- Make CI fail if nominal run vs resumed run diverge.

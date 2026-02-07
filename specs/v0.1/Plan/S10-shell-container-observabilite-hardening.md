# S10 - Shell, Container, Observability, and Hardening

## Objective

Complete the v0.1 scope with system-level action families and lock operational quality.

## Scope

IN:

- shell action family.
- container action family.
- Truffle instrumentation (standard tags).
- performance/reliability/conformance hardening.

OUT:

- v0.2 extensions (streaming, retry DSL, first-class functions).

## Spec References

- `action-shell.md`
- `action-container.md`
- `relang-actions-proposal.md`
- `relang-spec-v0.1-canonique.md`

## Implementation Sequence

1. Implement shell family:
   - safe `run(program,args)`,
   - interpreted `command(...)`,
   - output modes,
   - successCodes,
   - dedicated errors.
2. Implement container family:
   - run-to-completion,
   - resource/mount/env options,
   - output modes,
   - dedicated errors.
3. Integrate shell safety controls:
   - documentation and injection warnings.
4. Add Truffle instrumentation:
   - `StatementTag`, `ExpressionTag`, `CallTag`, etc.
5. Expose runtime observability:
   - executionId/failure.id correlation,
   - checkpoint traces.
6. Run hardening campaign:
   - micro/macro perf,
   - crash/resume chaos,
   - endurance tests.
7. Execute final v0.1 conformance suite.

## Explicit Error Management and DevEx Tasks

1. Define dedicated shell/container diagnostics for:
   - unexpected exit code,
   - timeout,
   - cancellation,
   - environment errors.
2. Standardize operational error output with:
   - diagnostic code,
   - readable message,
   - minimal context (command/image, attempt, executionId).
3. Add runbook-oriented `help:`:
   - immediate action,
   - environment checks,
   - escalation path.
4. Guarantee incident-grade observability:
   - correlation IDs visible in diagnostics.
5. Add DevEx non-regression tests under degraded conditions:
   - structured logs + understandable diagnostics under load.

## Deliverables

- All v0.1 action families delivered.
- Observability usable in operations.
- v0.1 readiness report.

## Mandatory Tests

- shell success/error/timeout/cancel.
- container success/error/timeout/cancel.
- resume under load with mixed actions.
- snapshot compatibility on real datasets.
- conformance matrix at 100% for v0.1 MUST requirements.

## Risks and Safeguards

Risk: shell/container actions introduce environment fragility.

Safeguard:

- hermetic tests,
- dedicated runners,
- reproducible fixtures.

Risk: insufficient visibility during incidents.

Safeguard:

- minimal dashboards + structured logs + correlation IDs.

## Definition of Done

- Full v0.1 suite is green.
- Final report includes:
  - spec conformance,
  - remaining deviations,
  - v0.2 follow-up plan.

## End-of-Sprint Governance Gate

Mandatory before closure:

- `spec-delta` report: `Governance/04-spec-delta-review.md`.
- Conformance matrix update (status for touched requirements).
- Open-risk validation (accepted/replanned/fixed).

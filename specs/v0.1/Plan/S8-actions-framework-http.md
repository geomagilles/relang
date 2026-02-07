# S8 - Action Framework and HTTP Family

## Objective

Deliver the first complete external effect family on top of a robust action-family foundation.

## Scope

IN:

- shared action-family infrastructure.
- family-level error contract.
- complete HTTP action family for v0.1.
- idempotency key support.
- best-effort cancellation.

OUT:

- gRPC/OpenAPI/shell/container families (S9-S10).

## Spec References

- `relang-actions-proposal.md`
- `action-http.md`
- `relang-failures-proposal.md`

## Implementation Sequence

1. Define internal `ActionExecutor` interface:
   - validate input,
   - execute,
   - map error,
   - produce awaitable result.
2. Define action-family registry.
3. Implement family error mapping -> sealed `ActionError`.
4. Implement HTTP API:
   - methods,
   - options,
   - auth,
   - output modes.
5. Implement HTTP error mapping:
   - timeout,
   - DNS,
   - status,
   - protocol.
6. Integrate idempotency key.
7. Integrate best-effort cancellation (observable state).
8. Add latency + attempts instrumentation.

## Explicit Error Management and DevEx Tasks

1. Define a clear action-family error taxonomy:
   - timeout,
   - DNS,
   - HTTP status,
   - protocol/auth.
2. Expose actionable diagnostics with:
   - endpoint,
   - method,
   - attempt count,
   - idempotency key (when present).
3. Add class-specific `help:` guidance:
   - verify credentials,
   - tune timeout,
   - use retry/fallback.
4. Add stable mapping from family errors to diagnostic codes.
5. Add HTTP error DX tests:
   - readable message + essential technical details, without noise.

## Deliverables

- Extensible action framework.
- HTTP family usable in pilot production.

## Mandatory Tests

- HTTP 2xx success.
- HTTP non-2xx -> `ActionFailed`/`HttpStatus`.
- timeout pattern via `or timer(...)`.
- idempotency key returns same result.
- cancellation before completion.

## Risks and Safeguards

Risk: making HTTP a special case and breaking extensibility.

Safeguard:

- enforce common family contract first, HTTP implementation second.

Risk: poor diagnostics on action failures.

Safeguard:

- enrich `attempts` and keep runtime details linked to `failure.id`.

## Definition of Done

- HTTP family complete per v0.1 spec.
- Action infrastructure reusable for S9/S10.

## End-of-Sprint Governance Gate

Mandatory before closure:

- `spec-delta` report: `Governance/04-spec-delta-review.md`.
- Conformance matrix update (status for touched requirements).
- Open-risk validation (accepted/replanned/fixed).

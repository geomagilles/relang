# S9 - gRPC and OpenAPI Families with Generation

## Objective

Add compile-time typed integrations for gRPC and OpenAPI to reduce integration errors.

## Scope

IN:

- gRPC action family.
- client generation from proto.
- OpenAPI action family.
- client generation from OpenAPI specs.

OUT:

- native streaming (out of v0.1 scope).

## Spec References

- `action-grpc.md`
- `action-openapi.md`
- `relang-actions-proposal.md`

## Implementation Sequence

1. Define code-generation pipeline:
   - spec input,
   - validation,
   - ReLang stub generation.
2. Implement gRPC family:
   - unary calls,
   - metadata,
   - credentials,
   - `GrpcError` mapping.
3. Implement OpenAPI family:
   - operationId -> methods,
   - typed request/response,
   - `OpenApiError` mapping.
4. Integrate generation into build.
5. Add generation cache with spec-hash invalidation.
6. Add generation diagnostics:
   - invalid spec,
   - duplicate operationId,
   - unsupported schema.

## Explicit Error Management and DevEx Tasks

1. Require precise generation diagnostics with:
   - source spec file,
   - JSON/YAML/Proto path,
   - line/column when available.
2. Categorize failures:
   - spec parse,
   - schema validation,
   - unsupported type mapping.
3. Add concrete `help:` guidance:
   - operationId fix,
   - supported alternative schema,
   - exact spec section to modify.
4. Add stable diagnostic codes for generator failures (tooling/CI).
5. Add generation DX tests:
   - build failure must be understandable in one pass without manual debugging.

## Deliverables

- Stable generation tooling.
- Usable gRPC/OpenAPI families.

## Mandatory Tests

- simple proto generation.
- simple OpenAPI generation.
- gRPC success + error status call.
- OpenAPI success + ApiError call.
- clean build failure on invalid spec.

## Risks and Safeguards

Risk: real-world specs are highly heterogeneous.

Safeguard:

- strict mode + explicit limitation report.

Risk: long-term generator maintenance debt.

Safeguard:

- clear separation:
  - spec parser,
  - intermediate model,
  - code renderer.

## Definition of Done

- reproducible generation.
- gRPC/OpenAPI families compliant with v0.1.

## End-of-Sprint Governance Gate

Mandatory before closure:

- `spec-delta` report: `Governance/04-spec-delta-review.md`.
- Conformance matrix update (status for touched requirements).
- Open-risk validation (accepted/replanned/fixed).

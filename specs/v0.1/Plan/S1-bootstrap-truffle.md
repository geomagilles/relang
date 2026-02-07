# S1 - Truffle Bootstrap and Minimal Pipeline

## Objective

Establish an executable and testable foundation for the language without structural debt that would block later phases.

## Scope

IN:

- `TruffleLanguage` + `Context`.
- Minimal module parsing.
- Executable `RootNode`.
- Test harness.
- Basic local CI (build + test).

OUT:

- Advanced type system.
- Awaitables.
- Snapshots.
- External action families.

## Prerequisites

- Locked JDK/GraalVM version.
- Configured build system (Gradle/Maven).
- Agreed package/folder conventions.

## Implementation Sequence (Strict Order)

1. Create the language runtime module.
2. Implement `RelangLanguage extends TruffleLanguage<RelangContext>`.
3. Implement `RelangContext` (minimal config, mockable services).
4. Define `RelangRootNode` and one test `EvalRootNode`.
5. Add a temporary minimal parser (returns a constant AST).
6. Wire `parse(...)` -> `CallTarget`.
7. Add minimal developer CLI: `run/check`.
8. Add smoke tests:
   - empty module parse,
   - constant expression execution,
   - basic parse failures.
9. Add local CI scripts (build + test + lint if present).

## Explicit Error Management and DevEx Tasks

1. Define the baseline diagnostic contract:
   - stable code,
   - severity,
   - primary range,
   - user-facing message.
2. Standardize minimal S1 parse error wording:
   - `Expected <X>, found <Y>` when possible,
   - no raw ANTLR internal phrasing exposed directly.
3. Expose coherent CLI rendering for bootstrap errors:
   - code + message + position.
4. Add a DX non-regression test:
   - parse errors must be readable without Java stacktrace noise.
5. Document the error flow in one short page:
   - parser -> diagnostic -> CLI/tool rendering.
   - reference: `Governance/07-parser-diagnostic-flow.md`.

## Deliverables

- Compilable language bootstrap.
- Automatable smoke suite.
- Short architecture note (1 page) on parser/typechecker/runtime separation.

## Mandatory Tests

- `empty_module_executes`.
- `constant_expression_executes`.
- `invalid_source_returns_parse_error`.
- `context_created_once_per_execution`.

## Risks and Safeguards

Risk: mixing parser, type checker, and runtime concerns too early.

Safeguard:

- strict interfaces:
  - `Parser` -> raw AST,
  - `TypeChecker` -> annotated AST,
  - `Executor` -> runtime nodes.

Risk: brittle tests tied to internal implementation details.

Safeguard:

- test observable behavior, not internal classes.

## Definition of Done

- Project compiles cleanly.
- 100% of S1 tests are green.
- `run` executes a trivial program.
- Module structure validated in technical review.

## End-of-Sprint Governance Gate

Mandatory before closure:

- `spec-delta` report: `Governance/04-spec-delta-review.md`.
- Conformance matrix update (status for touched requirements).
- Open-risk validation (accepted/replanned/fixed).

# S4 - Functions, Inline Calls, and Modules

## Objective

Enable real code composition: top-level functions, inline calls, imports/modules, and portability constraints.

## Scope

IN:

- top-level `fn`.
- inline function call `f(...)`.
- named parameters + default values.
- modules/imports.
- no scope capture by named functions.

OUT:

- `spawn`.
- advanced lambdas.
- first-class functions (out of v0.1 scope).

## Spec References

- `relang-functions.md`
- `relang-modules-proposal.md`
- `relang-spec-v0.1-canonique.md` (function/module sections)

## Implementation Sequence

1. Implement top-level function declaration AST (`FnDecl`).
2. Implement module-level function symbol resolution.
3. Implement inline calls + argument evaluation:
   - positional,
   - named,
   - default values.
4. Reject:
   - nested functions,
   - external variable capture.
5. Implement imports:
   - item import,
   - namespace import,
   - alias.
6. Implement inter-module cycle detection.
7. Add visibility rules:
   - `private` at module scope.
8. Add function hoisting inside module.

## Explicit Error Management and DevEx Tasks

1. Introduce explicit diagnostics for:
   - unknown function,
   - unknown named argument,
   - invalid arity,
   - module import/cycle errors.
2. Add contextual notes:
   - target function declaration,
   - target parameter declaration,
   - cycle detection site.
3. Add corrective `help:` guidance:
   - rename symbol,
   - add missing argument,
   - fix import.
4. Require invalid calls to show:
   - stable code,
   - expected signature.
5. Add DX regression tests for inter-module symbol resolution.

## Deliverables

- Stable inline function execution engine.
- Module/import resolution.
- Enforced self-contained constraints.

## Mandatory Tests

- call before declaration (hoisting) is valid.
- external variable capture -> compile error.
- module cycle A<->B detected.
- invalid mixed named parameters rejected.
- nested function rejected.

## Risks and Safeguards

Risk: non-deterministic symbol resolution.

Safeguard:

- specified resolution order:
  1. local,
  2. explicit imports,
  3. namespace.

Risk: compatibility debt for upcoming `spawn`.

Safeguard:

- call API designed for both inline and distributed modes.

## Definition of Done

- Inline functions are production-ready.
- Modules/imports are robust.
- v0.1 portability rules are enforced.

## End-of-Sprint Governance Gate

Mandatory before closure:

- `spec-delta` report: `Governance/04-spec-delta-review.md`.
- Conformance matrix update (status for touched requirements).
- Open-risk validation (accepted/replanned/fixed).

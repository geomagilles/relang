# S11 - Diagnostics and Developer Experience Excellence

## Objective

Make ReLang diagnostics best-in-class for parser, type checker, runtime errors, CLI, and LSP.

The target quality bar is comparable to Kotlin and Rust:

- clear primary message,
- precise source range,
- expected vs found details,
- actionable help and suggested fixes,
- stable diagnostic codes for tooling and documentation.

## Scope

IN:

- End-to-end diagnostic model (syntax, type, runtime).
- Human-friendly parser error translation (ANTLR -> ReLang diagnostics).
- Type checker migration to structured diagnostics.
- CLI/REPL diagnostic rendering with source code frames.
- LSP diagnostics with stable code, related info, and fix metadata.
- Snapshot-based diagnostic tests and quality gates.
- Developer documentation and maintenance process.

OUT:

- Full interactive auto-fix engine in IDE (post-S11 extension).
- Telemetry backend and cloud analytics pipeline.

## Success Metrics

1. 100% of parse/type/runtime diagnostics have:
   - stable code,
   - primary message,
   - at least one precise range.
2. At least 80% of common user errors include a `help` hint.
3. At least 50% of common user errors include a suggested fix edit.
4. Diagnostic snapshot tests cover top 30 error scenarios.
5. No new language feature merges without diagnostic tests and docs updates.

## Target Architecture

### 1) Structured Diagnostic Domain

Introduce a shared diagnostic model in `relang-core`:

- `Diagnostic` (code, severity, title, message, primary range),
- `DiagnosticNote` (secondary context),
- `DiagnosticHelp` (actionable hint),
- `DiagnosticFix` (optional text edit proposal),
- `DiagnosticCategory` (`SYNTAX`, `TYPE`, `RUNTIME`).

### 2) Dedicated Builders

Replace ad-hoc strings with strongly named builders:

- `syntax.unexpectedToken(...)`,
- `type.mismatch(...)`,
- `type.undefinedVariable(...)`,
- `runtime.invalidOperation(...)`.

No direct raw message literals in checker/parser logic except in builders.

### 3) Renderers

- `CliDiagnosticRenderer`: code frame + caret + notes + help.
- `LspDiagnosticMapper`: maps internal model to LSP diagnostics, related information, and code actions.

### 4) ANTLR Translation Layer

Add parser diagnostic translation from ANTLR internals to user vocabulary:

- convert token names into language terms (`identifier`, `expression`, `')'`),
- rewrite `"mismatched input"` into `Expected <X>, found <Y>`,
- attach context-sensitive hints.

## Explicit Error Management and DevEx Tasks

1. Keep parser/type/runtime diagnostics in dedicated builders only (no ad-hoc wording in feature code paths).
2. Require stable code + primary range + user-facing message for every new error path.
3. Require `help:` for common user mistakes and track coverage continuously.
4. Keep LSP payload parity with CLI:
   - code,
   - range,
   - related info,
   - quick-fix metadata when high-confidence.
5. Enforce regression gates:
   - diagnostic matrix scenarios,
   - snapshot/contract tests,
   - CI failure on uncoded diagnostics.

## Prioritized Implementation Checklist

- [x] P0 - Diagnostic model and code taxonomy
  - Added diagnostic domain classes in `relang-core`.
  - Defined code space (`RL1xxx`, `RL2xxx`, `RL3xxx`, `RL9xxx`).
- [x] P0 - Parser diagnostics foundation
  - Added ANTLR message translator and syntax code mapping.
  - Kept first-error compatibility behavior.
- [x] P0 - Type checker migration to dedicated builders
  - `TypeDiagnostics` central builders are in place and `ReLangTypeChecker` now routes emissions through them.
- [x] P0 - Exception and transport integration
  - Structured diagnostics are carried in parser/type exceptions.
  - Renderer-based compatibility message path is active.
  - Runtime node failures now emit structured `RL3xxx` diagnostics with actionable `help:` hints.
- [x] P1 - CLI diagnostic renderer upgrade
  - Rust-style code-frame output is available with `help:` / `note:`.
- [x] P1 - LSP enrichment
  - `relang-lsp` emits diagnostics directly with structured `code`, `source`, `data.relangHelp`, `relatedInformation`, and quick-fix payloads for high-confidence cases.
  - Core type diagnostics now attach declaration-site notes for key errors (arity mismatch, argument mismatch, assignment mismatches, immutable parameter reassignment, and user-type field issues), which are surfaced in LSP `relatedInformation`.
- [x] P1 - Test suite hardening
  - Added diagnostic matrix regression tests (30+ scenarios) with code/help gates.
- [x] P0 - Documentation and governance baseline
  - `S11` plan and governance playbook published.
- [x] P1 - Governance enforcement in CI
  - Added executable gates in test suite: scenario matrix size floor, code expectations, and help-coverage threshold.

## Current Status Summary

- Completed now: P0 foundation for model, parser mapping, type builder consolidation, compatibility integration, baseline docs, and direct structured LSP `code` wiring.
- Completed now: P1 matrix-based regression coverage and governance gates in CI test flow.
- Completed now: runtime diagnostics baseline (`RL3001`-`RL3005`) is wired in core runtime node failures.
- Next critical target: extend runtime declaration/context notes and cover state-mismatch/resume errors with dedicated `RL3xxx` diagnostics.

## Module-by-Module Deliverables

- `relang-core`:
  - diagnostic domain,
  - parser/type/runtime diagnostic builders,
  - CLI/REPL rendering,
  - tests.
- `relang-lsp`:
  - LSP mapping validation,
  - integration tests for published diagnostics payload.
- `relang-vscode` and `relang-intellij`:
  - consume diagnostic code and quick-fix metadata,
  - validate UX in editors.
- `relang-docs`:
  - user-facing diagnostic reference and troubleshooting page.

## Mandatory Test Matrix

1. Syntax errors:
   - missing delimiter,
   - unexpected keyword,
   - unclosed interpolation,
   - invalid construct field list.
2. Type errors:
   - type mismatch,
   - undefined variable,
   - arity mismatch,
   - unknown function,
   - invalid condition type,
   - recursive return type inference failure.
3. Runtime errors:
   - invalid operation at runtime,
   - non-exhaustive match at runtime,
   - invalid runtime argument binding,
   - invalid runtime condition values,
   - state mismatch/resume related failures.
4. LSP:
   - diagnostic code present,
   - primary range precision,
   - related information mapping.

## Risks and Safeguards

Risk: Diagnostic migration breaks existing tests and tools expecting raw strings.

Safeguards:

- keep legacy message compatibility in early milestones,
- ship diagnostic code first, wording migration second,
- snapshot tests for old and new renderers during transition.

Risk: Error cascades create noisy editor output.

Safeguards:

- one root-cause diagnostic per failing node when possible,
- suppress duplicate diagnostics on identical range/code pair.

## Definition of Done

- Structured diagnostics used by parser, type checker, and runtime by default.
- CLI output includes code frame and actionable help.
- LSP diagnostics include stable code and precise ranges.
- Quality gates integrated into PR review and CI.
- Developer playbook adopted (Governance/06).

## End-of-Sprint Governance Gate

Before closing S11, all are mandatory:

- Diagnostic quality checklist review: `Governance/06-diagnostic-quality-playbook.md`.
- Conformance matrix update for changed language behavior.
- Spec delta review for grammar/type rule updates.

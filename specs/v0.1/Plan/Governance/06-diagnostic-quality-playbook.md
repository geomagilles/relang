# Diagnostic Quality Playbook

## Purpose

This playbook defines how ReLang diagnostics are designed, implemented, reviewed, and maintained over time.

It is mandatory for all parser, type checker, runtime, CLI, and LSP changes.

## Core Principles

1. Diagnostics are part of the language contract, not implementation noise.
2. Every error should help the user fix the code in one edit cycle.
3. Prefer precision over volume: avoid cascading and duplicate diagnostics.
4. Messages must stay stable enough for tests, docs, and tooling.
5. Every diagnostic must carry a stable code and a source range.

## Canonical Diagnostic Shape

Each diagnostic must include:

- `code` (stable, machine-readable, example: `RL2003`),
- `category` (`SYNTAX`, `TYPE`, `RUNTIME`),
- `severity` (`ERROR`, `WARNING`, optional `INFO`),
- `title` (short human summary),
- `message` (full user-facing explanation),
- `primary range` (required),
- `notes` (optional contextual details),
- `help` (optional actionable next step),
- `fixes` (optional text edits).

## Code Taxonomy

- `RL1xxx`: syntax and parse diagnostics.
- `RL2xxx`: static type checker diagnostics.
- `RL3xxx`: runtime execution diagnostics.
- `RL9xxx`: internal/tooling diagnostics (rare, non-user-facing).

Rules:

- A code is never reused for a different meaning.
- If semantics changes materially, create a new code.
- Deprecate old codes in release notes when needed.

## Message Style Guide

### Required Style

1. Start with a concise problem statement.
2. Use user vocabulary (language concepts), not parser internals.
3. Prefer `expected <X>, found <Y>` when applicable.
4. Include concrete symbol names (`variable 'x'`, `function 'foo'`).
5. Include actionable `help:` text when there is a clear next step.

### Avoid

- Raw ANTLR wording (`mismatched input`) exposed directly.
- Blame language ("you did X wrong").
- Internal class names or stack traces.
- Ambiguous placeholders without context.

### Recommended Templates

- Type mismatch:
  - `Type mismatch: expected <Expected>, found <Actual>.`
- Undefined symbol:
  - `Unknown variable '<Name>' in this scope.`
- Arity mismatch:
  - `Function '<Name>' expects <N> arguments, but <M> were provided.`
- Parse expectation:
  - `Expected <TokenOrConstruct>, found <ActualToken>.`

## Source Range Best Practices

1. Highlight the smallest useful span.
2. Point to the operator token for operator misuse.
3. Point to the value expression for assignment type mismatch.
4. Attach related ranges for:
   - declaration site,
   - previous conflicting type,
   - duplicate definitions.
5. Never emit a diagnostic with an unknown range when a valid token exists.

## Help and Fix Best Practices

1. `help` must be executable guidance, not generic advice.
2. Propose fixes only when confidence is high and intent is clear.
3. Keep fixes minimal and local; do not rewrite full files.
4. If multiple valid fixes exist, provide a short ranked set.

## Cascade Control Rules

1. One root cause should emit one primary diagnostic.
2. Suppress follow-up diagnostics that are direct consequences of unknown type recovery.
3. De-duplicate by `(code, primary range, normalized message key)`.
4. Keep output order stable: lexical order by file range.

## Implementation Rules for Contributors

1. Use diagnostic builder APIs, never inline ad-hoc strings in parser/type/runtime logic.
2. Add or update diagnostic codes in a central catalog.
3. Add snapshot tests for each new code path.
4. Update user docs for new common diagnostics or changed behavior.
5. Preserve backward compatibility for existing code IDs unless intentionally versioned.

## Pull Request Checklist (Mandatory)

1. Does every new diagnostic include code, message, and precise range?
2. Is wording user-facing and free of internal parser/runtime terms?
3. Is there at least one actionable help message for common failure paths?
4. Are parser/type/runtime snapshots updated and reviewed?
5. Are LSP diagnostics validated (code + range + related info)?
6. Are docs updated for user-visible diagnostic changes?

## CI Quality Gates

1. Diagnostic snapshot tests must pass.
2. LSP diagnostic contract tests must pass.
3. No uncoded diagnostics (`code == null`) in parser/type/runtime paths.
4. No duplicate code definitions in diagnostic catalog.
5. New grammar or type-rule features require at least one failure test.

## Workflow for Any New Language Feature

1. Add syntax and semantic implementation.
2. Define failure modes explicitly.
3. Add diagnostic codes for each failure mode.
4. Implement diagnostics via builders.
5. Add parser/type/runtime tests (success and failure paths).
6. Add LSP mapping tests for ranges and codes.
7. Update docs:
   - user-facing troubleshooting if relevant,
   - this playbook if conventions changed.

No feature is considered done until all seven steps are complete.

## Ownership and Governance

- Primary owners: `relang-core` maintainers.
- Secondary owners: IDE integration maintainers (`relang-lsp`, `relang-vscode`, `relang-intellij`).
- Governance review frequency: once per sprint for active language work.

This playbook is an active standard and must evolve with language complexity while preserving its core principles.

# Parser to Diagnostic Flow (S1 Reference)

## Purpose

Describe the minimal error flow from parser/type checker to CLI and LSP rendering.

## Flow

1. Source text is parsed with ANTLR in `ReLangTruffleParser.parseAntlr`.
2. ANTLR raw messages are translated by `SyntaxDiagnosticTranslator` into ReLang wording and stable codes (`RL1xxx`).
3. Syntax and type failures are converted to structured diagnostics:
   - syntax: `SyntaxError -> ReLangDiagnostic`,
   - type: `TypeError -> ReLangDiagnostic`.
4. Transport and rendering:
   - CLI/REPL: `CliDiagnosticRenderer` (code + message + position + optional notes/help),
   - LSP: `LspDiagnosticMapper` (code + range + related information + quick-fix metadata).
5. Runtime node failures use `ReLangTypeError` with `RuntimeDiagnostics` builders (`RL3xxx`) and are rendered consistently.

## Maintenance Rules

1. Do not emit raw parser/runtime internals directly to users.
2. Every new failure path must define:
   - stable code,
   - primary range,
   - user-facing message,
   - actionable `help:` when relevant.
3. Any wording/code change must update tests in:
   - `relang-core` diagnostics tests,
   - `relang-lsp` mapper/engine tests when payload shape changes.

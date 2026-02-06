# CLI Reference

This page defines the documentation contract for CLI behavior relative to v0.1.

## Scope

`specs/v0.1` is normative for language/runtime semantics, but does not standardize a full command-line interface.

Because of that, CLI flags are implementation-specific and may vary across launchers.

## Required Documentation Rule

Any runtime that exposes a `relang` command should document, in its own module README:

- invocation syntax
- input source conventions
- exit code contract
- suspend/resume operational flags (if any)
- diagnostics and logging flags

## Stable Semantic Expectations

Regardless of CLI shape, runtimes must preserve v0.1 semantics for:

- explicit `await`
- awaitable coordination (`and`, `or`)
- `Failure` propagation rules
- snapshot/resume behavior across effect boundaries

## Repository Pointers

- `/Users/gilles/dev/relang/relang-core/README.md`
- `/Users/gilles/dev/relang/relang-native/README.md`
- `/Users/gilles/dev/relang/relang-compiler/README.md`

## See Also

- [Language Syntax Reference](reference-language-syntax.md)
- [How to Run Programs](howto-run-programs.md)

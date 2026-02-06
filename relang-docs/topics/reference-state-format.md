# State Format Reference (v0.1)

This page documents the required semantic content of runtime snapshots for v0.1.

## Scope

The canonical spec defines what state must be preserved, not a mandatory on-disk wire format.

A runtime may use JSON, protobuf, binary, or another format, as long as semantics are preserved.

## Required Snapshot Content

A v0.1-compatible snapshot includes:

- execution identity/context (`self.id`, parent linkage, creation metadata)
- call stack, locals, and program counter
- awaitable table (identity, status, resolved values/failures)
- workflow time metadata (including persisted timer deadlines)
- state/code version metadata for compatibility checks

## Required Resume Behavior

On resume, runtime must:

1. load latest stable snapshot
2. restore stack, locals, and control position
3. restore already-resolved awaitables without replaying those effects
4. continue execution from the same semantic point

## Compatibility Rules

Canonical expectations from `specs/v0.1`:

- default resume only with same `codeVersion`
- version mismatch is rejected unless explicit migration is configured
- migration should use versioned state strategy

## Checkpoint Boundaries

Checkpointing is mandatory around unresolved effect waits (`await`, timer wait, signal wait) and awaitable status transitions observed by runtime.

## Implementation Notes

Any serialization format is acceptable if it preserves:

- deterministic restore of language-visible values
- durable linkage between failures and execution context
- stable interpretation of `Failure` and awaitable status fields

## See Also

- [How to Suspend and Resume](howto-suspend-resume.md)
- [How Resumability Works](explanation-resumability.md)

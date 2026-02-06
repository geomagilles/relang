# How Resumability Works (v0.1)

This explanation describes the conceptual v0.1 durability model.

## Core Idea

ReLang durability is snapshot-based:

- effects are explicit (`*T` awaitables)
- unresolved waits create runtime suspension points
- runtime persists execution state
- resume restores and continues without replaying already-resolved effects

## Where Suspension Happens

In canonical v0.1, suspension is tied to runtime-observed effect boundaries, especially:

- `await` on unresolved awaitables
- long waits (`receive<T>()`, timer waits)

There is no dedicated `checkpoint;` surface keyword in canonical syntax.

## What State Is Captured

A checkpoint captures at least:

- execution identity and context (`self`)
- call stack and local bindings
- control position (program counter)
- awaitable table and resolution status
- version metadata used for compatibility checks

## Resume Flow

1. load latest stable snapshot
2. restore stack/locals/control position
3. reuse already-resolved awaitable results
4. continue execution from the restored point

This avoids re-running completed external effects.

## Version Safety

Default behavior is same-code-version resume only.

If code version changes, resume is rejected unless explicit state migration is configured.

## Why This Model

Compared with full replay systems:

- fewer implicit assumptions about replay safety
- clearer effect boundaries in source code (`await`)
- operationally robust for long-running orchestration

## See Also

- [State Format Reference](reference-state-format.md)
- [Truffle Architecture](explanation-truffle.md)

# ReLang Documentation

This documentation is aligned with the normative spec in `/Users/gilles/dev/relang/specs/v0.1/relang-spec-v0.1-canonique.md` and its companion `/Users/gilles/dev/relang/specs/v0.1/relang-failures-proposal.md`.

## What ReLang v0.1 Is

ReLang v0.1 is a durable orchestration language:

- Effects are explicit and return awaitables (`*T`)
- `await` is explicit (`await e`)
- Runtime snapshots execution state at effect boundaries
- Resume continues from the latest stable snapshot

## Canonical Concepts

- Awaitables: `*T`
- Coordination: `and`, `or`
- Optional values: `T?` with `none`
- Failure contract: single `Failure` envelope with `FailureKind`
- Function invocation modes: inline (`f(...)`) and distributed (`spawn f(...)`)

## Documentation Structure

This module follows Diataxis:

- Tutorials: guided learning paths
- How-to Guides: task-focused instructions
- Reference: precise syntax/contracts
- Explanation: conceptual internals and design rationale

## Minimal Canonical Example

```relang
type Order { id: String, total: Int }
type Receipt { id: String }

fn charge(order: Order): *Receipt {
  payments.charge(order)
}

fn process(order: Order): Receipt | Failure {
  let payment = await charge(order)
  match payment {
    r: Receipt -> r
    f: Failure -> f
  }
}
```

## Source of Truth

When a documentation page conflicts with implementation experiments or historical drafts, use `specs/v0.1` as source of truth.

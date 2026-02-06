# Truffle Architecture

This page explains how ReLang is implemented on Truffle while respecting the v0.1 language contract.

## High-Level Pipeline

1. Parse ReLang source
2. Build Truffle AST nodes
3. Execute through Truffle interpreter
4. Let Graal optimize hot paths

## Why Truffle Fits ReLang

ReLang needs:

- precise control of evaluation points (`await`, `spawn`, control flow)
- state capture/restore support for durable resume
- efficient execution for orchestration-heavy workloads

Truffle provides a strong base for all three.

## Interaction with Durability

Truffle execution frames and node structure map naturally to snapshot state:

- frame locals become serializable bindings
- control position can be represented at AST/block boundaries
- runtime can restore and continue from saved interpreter state model

The exact internal mechanism is implementation-specific, but language-visible behavior must follow `specs/v0.1`.

## Language vs Implementation

`specs/v0.1` is normative for semantics.

Truffle internals are an implementation strategy and can evolve as long as these semantics remain stable:

- `await` contract
- failure propagation model
- snapshot/resume observable behavior

## See Also

- [How Resumability Works](explanation-resumability.md)
- [Language Syntax Reference](reference-language-syntax.md)

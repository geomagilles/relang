# Distributed Systems Considerations

Design principles for ReLang's durable execution primitives from a distributed systems perspective.

**Note**: These are guiding principles, not constraints. If a principle conflicts with better language design, the principle can be revised. The goal is a coherent language, not adherence to any particular distributed systems dogma.

## ReLang as Orchestrator

ReLang is an **orchestration language**, not a general-purpose runtime:

- **Coordinates** external work via awaitables
- **Does not execute** the actual effects (HTTP calls, DB queries, etc.)
- **Requires infrastructure** to run durably in production

### Infrastructure Dependency

| Component | Responsibility |
|-----------|---------------|
| ReLang runtime | Execute orchestration logic, manage state |
| Message broker | Deliver events, trigger workflow steps |
| Checkpoint store | Persist state for resume |
| Outbox | Reliable event publication |
| Action executors | Run external effects (HTTP, DB, etc.) |

For development/testing, ReLang can run end-to-end. For production, the infrastructure provides durability guarantees that ReLang assumes but doesn't implement.

## Durable Execution Model

### Core Model: Resumable Execution

ReLang uses **checkpoint-based resumption**, NOT event-sourcing replay:

| Aspect | ReLang (Resumable) | Event Sourcing (Replayable) |
|--------|-------------------|----------------------------|
| Recovery | Load state from checkpoint | Replay all events from start |
| State | Explicit checkpoint snapshots | Derived from event history |
| Resume point | Any checkpoint | Beginning only |
| Requirement | Serializable state | Deterministic execution |

### How Resumption Works

```
Execute → Checkpoint(state) → [crash/pause] → Resume(state) → Continue
```

1. Execution proceeds normally
2. At checkpoint, full state is serialized and saved
3. On resume, state is loaded and execution continues from that point
4. No need to re-execute previous operations

**Design implication**: All state at checkpoint boundaries must be serializable.

### Single Execution Point

**A ReLang workflow has exactly ONE execution point at any time.**

- Workflow code is single-threaded — one "program counter"
- External parallelism: awaitables execute in parallel externally
- Internal sequentiality: workflow code proceeds one statement at a time
- No concurrent access to workflow variables
- No synchronization primitives needed

```
Workflow code:     ──────●──────────────●──────────────●──────→
                         │              │              │
                         ▼              ▼              ▼
Awaitables:        [=====t1=====]  [===t2===]  [======t3======]
                   [========t4========]
```

The workflow code has a single execution point (●), while multiple awaitables may be in-flight externally.

## State Serialization Requirements

### What Must Be Serializable

Since resumption loads state directly (not replaying events), checkpoint state must be fully serializable:

| Data | Serializable? | Notes |
|------|--------------|-------|
| Primitives | Yes | Int, String, Bool, etc. |
| Records/structs | Yes | All fields must be serializable |
| Collections | Yes | Lists, maps with serializable contents |
| Awaitable handles | Special | Reference by ID, not by value |
| Functions/closures | No | Cannot serialize code |
| External resources | No | Must be re-acquired on resume |

### Checkpoint Boundaries

State is captured at explicit checkpoint points:
- After awaitable resolution
- At explicit `checkpoint` statements
- At workflow suspension points

Between checkpoints, execution need not be deterministic — only the checkpointed state matters.

## Resume Considerations

### What Happens on Resume

When resuming from checkpoint:
1. Load serialized state
2. Re-establish external connections (if needed)
3. Continue execution from checkpoint position
4. Pending awaitables may need status check or restart

### Code Evolution

**Current stance: Code must not change during workflow lifetime.**

Checkpoint state is tightly coupled to the code that produced it:
- State reflects code structure (variable names, types, control flow position)
- Changing code risks state incompatibility on resume
- Even "safe-looking" changes can break resume semantics

Future work may address code migration, but for now:
- A workflow runs to completion on the code version that started it
- Schema evolution (if needed) is an explicit future design problem

## Partial Failure Handling

### Failure Modes in Distributed Systems

| Failure | Description | ReLang Handling |
|---------|-------------|-----------------|
| Crash | Process dies | Workflow resumes from checkpoint |
| Timeout | No response in time | `timeout(t, d)` returns `Failure(Timeout)` |
| Network partition | Can't reach service | Action fails, retry possible |
| Byzantine | Malicious/corrupted | Out of scope (trusted environment) |

### At-Most-Once vs At-Least-Once

**Actions** (external calls) have delivery semantics:
- **At-most-once**: Action may not execute (on early timeout)
- **At-least-once**: Action may execute multiple times (on retry)
- **Exactly-once**: Requires idempotency keys

ReLang provides:
- Automatic retry with `retry(t, policy)`
- Idempotency keys via action identity (`t.id`)

### Compensation vs Rollback

ReLang does **not** provide automatic rollback:
- Distributed rollback is generally impossible
- Compensation (undo actions) must be explicit
- Sagas pattern: forward recovery with compensating actions

## Cancellation Semantics

### Cancellation is Advisory

```relang
cancel(t)   // Request cancellation
// t may still succeed if already completed
```

Why advisory:
- Network delay means cancel arrives late
- External system may not support cancellation
- Side effects may have already occurred

### Cancellation Propagation

Default behavior:
- `and`: Cancel remaining on first failure
- `or`: Cancel remaining on first success

Override with `shield`:
```relang
shield(t)   // Block propagated cancellation
```

### Cancel vs Timeout

| Mechanism | Initiator | Semantics |
|-----------|-----------|-----------|
| `cancel(t)` | Explicit code | Advisory request |
| `timeout(t, d)` | Time elapsed | Cancel + Timeout failure |

Timeout implies cancellation but adds failure payload.

## Serialization Constraints

### What Must Be Serializable

All data crossing boundaries:
- Workflow inputs/outputs
- Action arguments/results
- Checkpoint state
- Failure payloads

### Serialization-Safe Types

| Type | Serializable | Notes |
|------|--------------|-------|
| Primitives | Yes | Int, String, Bool, etc. |
| Records/structs | Yes | All fields must be serializable |
| Sealed hierarchies | Yes | Discriminated unions |
| Functions/closures | No | Cannot serialize code |
| References/pointers | No | Cannot serialize addresses |
| Awaitables | No | Handles, not data |

### Schema Evolution

**Inspiration: Protocol Buffers**

Protobuf's field-numbered, additive-only evolution model will guide ReLang's approach:

| Rule | Rationale |
|------|-----------|
| Fields have stable numeric IDs | Wire format independent of field names |
| Add optional fields with defaults | Old readers ignore unknown fields |
| Never reuse field numbers | Prevents type confusion on old data |
| Never remove required fields | Old data must remain readable |
| Never change field types | Binary compatibility |

Forward compatibility (new writer, old reader):
- Old reader ignores unknown field IDs
- New optional fields have sensible defaults

Backward compatibility (old writer, new reader):
- New reader uses defaults for missing optional fields
- Required fields always present in old data

## Timing and Ordering

### Workflow Time vs Wall Time

```relang
workflow.now()      // Consistent within workflow context
System.time()       // May differ on resume (use with caution)
```

Time handling considerations:
- Timers should be relative to workflow logical time
- Wall clock may differ between original execution and resume
- Time-sensitive logic should use workflow-provided time primitives

### Ordering Guarantees

Within a workflow:
- Sequential code executes in order
- `and` operands may complete in any order
- `or` operands may complete in any order

Across workflows:
- No ordering guarantees without explicit coordination
- Use signals/queries for cross-workflow communication

### Temporal Ordering in Failures

For `or` coordination, failures preserve temporal order:

```relang
// If t1 fails, then t2 fails
(t1 or t2).await() → [Failure₁, Failure₂]  // Order preserved
```

## Consistency Considerations

### Eventual Consistency

ReLang workflows operate in eventually consistent environments:
- External systems may have stale data
- Concurrent workflows may conflict
- Coordination provides ordering within a workflow, not across

### Conflict Resolution

For concurrent access:
- Use external locking/coordination
- Design for commutative operations
- Implement merge strategies

## Performance Implications

### Checkpoint Overhead

Each checkpoint:
- Serializes workflow state
- Writes to durable storage
- Enables recovery from that point

Balance: More checkpoints = better recovery, more overhead.

### Action Batching

Individual actions have overhead:
- Network round-trip
- Event logging
- Serialization

Batch when possible:
```relang
let results = and([action(x) for x in items])  // Single coordination
```

### Workflow Size

Large workflows risk:
- Large checkpoint sizes
- Longer serialization/deserialization times
- Memory pressure

Consider: Split into child workflows for isolation.

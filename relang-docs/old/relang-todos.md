## TODOs

1) Scheduling semantics for coordination

Right now, you assume “eagerly started” but do not specify when operands are started for and/or, especially with nesting.

You need a normative statement covering:
•	eager vs lazy start
•	whether or is hedged (start all immediately) vs sequential fallback (start next only after failure/timeout)
•	whether the runtime may reorder starts for optimization

Without this, users cannot reason about latency vs load tradeoffs.

2) Retry model and interaction with failure/cancellation

Relang is an orchestration language. You need a first-class philosophy for retries:
•	where retry policy lives (per action, per scope, per operator)
•	what is retried (activity invocation vs logical task)
•	retry stop conditions (max attempts, deadline)
•	how retries surface in Failure (RetryExhausted, attempts metadata)
•	how cancellation interacts with retries (cancelling stops future retries)

If you do not define this, every action family will reinvent it.

3) Idempotency and deduplication semantics

You already model identity (id) but not what identity means operationally:
•	Is id used for deduplication of action execution?
•	Are repeated replays guaranteed not to re-run the same action?
•	What are the required idempotency properties of actions?
•	Can users provide an explicit idempotency key?

This is core to durable orchestration.

4) Timeout primitives and deadlines as first-class

You referenced timeouts as policy but need a language-level construct:
•	withTimeout(d) { ... } or timeout(t, d)
•	whether timeouts produce Timeout failure vs Cancelled vs both
•	whether timeouts propagate cancellation to children by default
•	deadline inheritance (parent deadline constrains children)

Without explicit deadlines, cancellation and retries become inconsistent.

5) Resource / concurrency limits

You have coordination operators but no way to express:
•	max parallelism for and(list<*T>)
•	backpressure semantics
•	rate limits per action family (HTTP concurrency caps)
•	fairness / scheduling

This matters operationally the moment users coordinate large lists.

6) Determinism boundary and “what is replayed”

You stated “deterministic, replayable” but must define:
•	what parts of evaluation are replayed
•	what is memoized
•	whether reading clocks/random is forbidden outside actions
•	how non-determinism is handled when it occurs

This prevents subtle footguns.

7) Structured error payload stability rules

Now that you use sealed families “by contract”, you need evolution rules:
•	can new variants be added without breaking code?
•	do matches over sealed families require _ => in production?
•	does the compiler warn on non-exhaustive matches?
•	versioning strategy for error families

Otherwise, sealed error sets become a compatibility trap.

8) Task lifecycle introspection semantics

You list methods like isOngoing/isCompleted/isFailed/data/error.
You need to specify:
•	whether these are instantaneous snapshots vs stable after resolution
•	whether calling data() before completion is defined
•	whether they are allowed in deterministic replay (they depend on time)
•	what is guaranteed to be persisted and queryable

If you keep them, they must be part of the deterministic model, not “runtime sugar”.
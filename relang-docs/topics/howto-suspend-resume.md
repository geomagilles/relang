# How to Suspend and Resume Execution

This guide explains suspend/resume behavior in v0.1 terms.

## 1. Understand the Trigger

In v0.1, suspension is runtime-driven around unresolved effect boundaries, especially:

- `await` on unresolved awaitables
- long waits such as `receive<T>()` and non-selected timers

There is no `checkpoint;` surface statement in canonical v0.1 syntax.

## 2. Write Program Logic for Safe Resume

Keep local state serializable and explicit:

```relang
type WorkflowState { orderId: String, approved: Bool }

fn run(orderId: String): WorkflowState | Failure {
  let approval = await receive<Approval>()
  match approval {
    a: Approval -> WorkflowState { orderId: orderId, approved: a.value }
    f: Failure -> f
  }
}
```

## 3. Design for Idempotency

Use stable identifiers when invoking external effects so a resumed execution does not duplicate logical work.

## 4. Handle Versioning Explicitly

Per v0.1 canonical spec:

- resuming with a different `codeVersion` is rejected by default
- migration requires explicit state-version strategy

## 5. Operational Checklist

- Keep `Failure` and domain types serializable
- Avoid hidden mutable process-local state
- Use `await` boundaries intentionally
- Validate runtime version and code version compatibility before resume

## See Also

- [State Format Reference](reference-state-format.md)
- [How Resumability Works](explanation-resumability.md)

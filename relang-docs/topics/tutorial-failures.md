# Handling Failures

This tutorial explains the canonical v0.1 failure contract.

## What You Will Learn

- The `Failure` envelope and `FailureKind`
- Propagation rules (`!`, inline call, `spawn` boundary)
- Aggregation semantics with `or`

## 1. Canonical Failure Shape

```relang
type Failure {
  id: String
  kind: FailureKind
  sourceId: String?
  failedAt: Timestamp
  execution: ExecutionRef
  cause: Failure?
  causes: [Failure]?
}
```

Core rule:

- `cause` and `causes` are mutually exclusive

## 2. Await Returns `T | Failure`

```relang
fn charge(order: Order): *Receipt {
  payments.charge(order)
}

fn pay(order: Order): Receipt | Failure {
  let r = await charge(order)
  match r {
    receipt: Receipt -> receipt
    f: Failure -> f
  }
}
```

## 3. Propagation with `!`

```relang
fn payStrict(order: Order): Receipt | Failure {
  await charge(order)!
}
```

## 4. Inline vs Distributed Propagation

- Inline call `f(...)`: failure propagates as-is in same execution
- Distributed call `await spawn f(...)`: parent observes `FunctionFailed` with child failure as `cause`

## 5. Aggregation with `or`

If all branches fail:

- `or` returns a `Failure` with `kind = AllFailed(...)`
- branch failures are in `causes` in lexical branch order

## 6. Business Errors vs Infrastructure Failures

- Business/domain outcomes: model as normal data (`Declined`, `InsufficientFunds`, ...)
- Infrastructure/execution errors: always `Failure`

## Next Step

Continue with [Controlling Execution](tutorial-execution-control.md).

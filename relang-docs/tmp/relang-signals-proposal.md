# ReLang Signals

*External Workflow Interruption*

---

## 1. Overview

Signals allow external systems to send messages to running workflows. They enable:

- Cancellation requests
- Status updates
- User input during execution
- Workflow coordination

**Core principle**: Signals are received at defined points, not arbitrarily.

---

## 2. Signal Declaration

### 2.1 Defining Signals

```relang
signal Cancel {}

signal UpdatePriority {
    newPriority: Int
}

signal UserInput {
    choice: String
    data: Json?
}
```

### 2.2 Signal Types

| Type | Description |
|------|-------------|
| Unit signal | No payload (`signal Cancel {}`) |
| Data signal | Carries payload |
| Typed signal | Payload matches specific type |

---

## 3. Receiving Signals

### 3.1 Wait for Signal

```relang
fn awaitApproval(requestId: String): ApprovalResult {
    // Do initial work
    let request = prepareRequest(requestId)!

    // Wait for external approval signal
    let approval = receive<Approval>()

    match approval.decision {
        Approved -> processApproved(request)
        Rejected -> processRejected(request, approval.reason)
    }
}
```

### 3.2 Signal with Timeout

```relang
let result = receive<UserInput>(timeout: 24h)

match result {
    input: UserInput -> process(input)
    f: Failure -> match f.error {
        t: Timeout -> useDefault()
        _ -> raise f
    }
}
```

### 3.3 Non-Blocking Check

```relang
let maybeSignal = tryReceive<Cancel>()

match maybeSignal {
    s: Cancel -> handleCancellation()
    none -> continueProcessing()
}
```

---

## 4. Sending Signals

### 4.1 From External Systems

```relang
// External API (not ReLang code)
workflowClient.signal(workflowId, Approval {
    decision: Approved,
    approver: "alice@example.com"
})
```

### 4.2 From Other Workflows

```relang
fn parent(): Result {
    let childId = startWorkflow(childWorkflow, params)

    // Send signal to child
    sendSignal(childId, UpdatePriority { newPriority: 1 })

    // Wait for child
    awaitWorkflow(childId)
}
```

---

## 5. Signal Queuing

### 5.1 Signals Are Queued

Signals sent before `receive` are queued:

```
Timeline:
  Signal sent ──────────┐
                        │ (queued)
  Workflow starts ──────┼─────────────────────
                        │
  receive<Signal>() ────┴────> Signal delivered
```

### 5.2 Multiple Signals

```relang
fn handleMultiple(): Result {
    // Receive first signal of type Cancel
    let sig1 = receive<Cancel>()

    // Receive next signal of type Cancel
    let sig2 = receive<Cancel>()

    // Signals delivered in order
}
```

### 5.3 Signal Selection

```relang
// Wait for any of several signal types
let signal = receiveAny([
    SignalType<Approval>,
    SignalType<Rejection>,
    SignalType<Cancel>
])

match signal {
    a: Approval -> ...
    r: Rejection -> ...
    c: Cancel -> ...
}
```

---

## 6. Signal Channels

### 6.1 Named Channels

```relang
fn multiChannel(): Result {
    // Different channels for different purposes
    let approval = receive<Approval>(channel: "approval")
    let updates = receive<Update>(channel: "updates")
}
```

### 6.2 Channel Semantics

- Signals are delivered to specific channels
- Default channel if not specified
- Independent queues per channel

---

## 7. Signal Handlers

### 7.1 Declarative Handlers

```relang
@onSignal(Cancel)
fn handleCancel(signal: Cancel): Unit {
    // Called when Cancel signal received
    cleanup()
}

fn main(): Result {
    // Cancel handler active throughout
    doWork()
}
```

### 7.2 Scoped Handlers

```relang
fn withHandler(): Result {
    withSignalHandler<Pause> { signal ->
        pauseProcessing()
    } {
        // Handler only active in this block
        doWork()
    }
}
```

---

## 8. Cancellation via Signal

### 8.1 Cancellation Signal

```relang
signal CancelWorkflow {
    reason: String?
}

fn cancellable(): Result {
    let work = longRunningOperation()

    // Check for cancellation
    select {
        result = work -> result
        cancel = receive<CancelWorkflow>() -> {
            cleanup()
            Failure { error: Cancelled { reason: cancel.reason } }
        }
    }
}
```

### 8.2 Graceful Shutdown

```relang
signal Shutdown {
    gracePeriod: Duration
}

fn server(): Unit {
    while true {
        let shutdown = tryReceive<Shutdown>()

        match shutdown {
            s: Shutdown -> {
                drainRequests(s.gracePeriod)
                return
            }
            none -> {
                handleNextRequest()
            }
        }
    }
}
```

---

## 9. Select Statement

### 9.1 Signal or Completion

```relang
select {
    result = operation() -> {
        // Operation completed
        handleResult(result)
    }
    signal = receive<Cancel>() -> {
        // Signal received first
        handleCancel()
    }
}
```

### 9.2 Multiple Options

```relang
select {
    r1 = task1 -> handleFirst(r1)
    r2 = task2 -> handleSecond(r2)
    cancel = receive<Cancel>() -> abort()
    timeout = self.sleep(30s) -> timedOut()
}
```

### 9.3 Select Semantics

- First matching branch wins
- Other branches are cancelled/ignored
- Deterministic on replay

---

## 10. Signal Persistence

### 10.1 Signals Are Durable

Signals survive workflow restarts:

```relang
fn durable(): Result {
    doStep1()!

    // Workflow crashes here

    // After restart, pending signals are still queued
    let signal = receive<Input>()
}
```

### 10.2 Signal History

```relang
self.signals       // List of received signals
self.pendingSignals // Queued but not yet received
```

---

## 11. Patterns

### 11.1 Human-in-the-Loop

```relang
fn orderWithApproval(order: Order): OrderResult {
    let validated = validateOrder(order)!

    if validated.requiresApproval {
        notifyApprover(order)

        let approval = receive<Approval>(timeout: 48h)

        match approval {
            a: Approval -> if a.approved { processOrder(order) } else { rejectOrder(order) }
            _ -> rejectOrder(order)
        }
    } else {
        processOrder(order)
    }
}
```

### 11.2 Long-Running with Updates

```relang
fn processing(jobId: String): JobResult {
    let totalItems = getItemCount(jobId)!
    let processed = 0

    for item in getItems(jobId) {
        // Check for pause/cancel signals
        match tryReceive<JobSignal>() {
            Pause -> {
                receive<Resume>()   // Wait for resume
            }
            Cancel -> {
                return JobResult.cancelled(processed)
            }
            none -> {}
        }

        processItem(item)
        processed = processed + 1

        // Report progress via signal to parent
        sendSignal(self.parentId, Progress {
            completed: processed,
            total: totalItems
        })
    }

    JobResult.completed(processed)
}
```

---

## 12. Summary

| Construct | Description |
|-----------|-------------|
| `signal Name { fields }` | Declare signal type |
| `receive<T>()` | Wait for signal |
| `receive<T>(timeout: d)` | Wait with timeout |
| `tryReceive<T>()` | Non-blocking check |
| `sendSignal(id, signal)` | Send to workflow |
| `select { ... }` | Wait for first of several |
| `@onSignal(T)` | Declarative handler |

**Key principles:**
- Signals are delivered at explicit receive points
- Signals are queued and persistent
- Use select for signal-or-completion patterns
- Cancellation is a signal, not magic

---

*End of proposal*

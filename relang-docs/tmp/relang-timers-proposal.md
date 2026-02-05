# ReLang Timers

*Time-Based Awaitables for Scheduling and Delays*

---

## 1. Overview

Timers are awaitables that resolve after a duration or at a specific time. Like all awaitables, they must be explicitly awaited.

**Core principles:**
- Timers are awaitables (`*Timestamp`)
- Resolve to the timestamp when they fired
- Created immediately, awaited when needed
- Durable across workflow restarts
- Use workflow time (deterministic)

---

## 2. Timer Creation

### 2.1 Duration-Based Timer

```relang
fn delayed(): Result {
    doFirstPart()!

    timer(5s)    // Pause for 5 seconds

    doSecondPart()!
}
```

### 2.2 Absolute Time Timer

```relang
fn scheduledJob(): Result {
    let targetTime = Timestamp.parse("2024-12-01T09:00:00Z")

    timer(targetTime)

    runJob()!
}
```

### 2.3 Timer Type

```relang
timer(d: Duration): *Timestamp    // resolves after duration
timer(t: Timestamp): *Timestamp   // resolves at timestamp
```

The `timer()` function is overloaded: it accepts either a `Duration` or a `Timestamp`. Timers resolve to the `Timestamp` when they fired, or `Failure` (if cancelled).

---

## 3. Timer as Awaitable

### 3.1 Deferred Awaiting

```relang
fn example(): Result {
    // Create timer immediately (starts counting)
    let timer = timer(10s)

    // Do work while timer runs
    let result = doWork()!    // takes ~3s

    // Await remaining time (~7s left)
    timer

    finalize(result)!
}
```

### 3.2 Already Elapsed

```relang
fn example(): Result {
    let timer = timer(5s)

    let result = slowOperation()!   // takes 10s

    timer    // Resolves immediately (5s already passed)

    result
}
```

### 3.3 Using the Returned Timestamp

```relang
fn example(): Result {
    let firedAt = timer(5s)!

    log("Timer fired at: ${firedAt}")

    // Use for next calculation
    let nextDeadline = firedAt + 10s
    timer(nextDeadline)!
}
```

### 3.4 Timer State Introspection

```relang
let timer = timer(5s)

timer.isResolved()      // false initially, true after 5s
timer.remainingTime()   // Duration remaining (0s if elapsed)
```

---

## 4. Duration Units

```relang
timer(100ms)       // milliseconds
timer(30s)         // seconds
timer(5m)          // minutes
timer(2h)          // hours
timer(1d)          // days

// Computed duration
let delay = baseDelay * retryCount
timer(delay)
```

---

## 5. Timers in Coordination

### 5.1 Timeout Pattern

```relang
fn withTimeout(): Result {
    let task = longOperation()
    let timeout = timer(30s)

    select {
        result = task -> result
        _ = timeout -> {
            cancel(task)
            Failure { error: Timeout { duration: 30s } }
        }
    }
}
```

### 5.2 Using `or` for Timeout

```relang
fn withTimeout(): Result {
    let task = longOperation()
    let timeout = timer(30s)

    // First to complete wins
    let result = (task or timeout)

    match result {
        r: Response -> r
        _: Unit -> Failure { error: Timeout { duration: 30s } }
    }
}
```

### 5.3 Timeout Helper

```relang
let result = timeout(longOperation(), 30s)
```

Equivalent to the `or` pattern above.

---

## 6. Periodic Execution

### 6.1 Fixed Delay

```relang
fn monitor(): Unit {
    while true {
        checkHealth()
        timer(1m)    // 1m after each completion
    }
}
```

### 6.2 Fixed Rate

```relang
fn fixedRate(): Unit {
    let interval = 1m
    let nextRun = now()

    while true {
        doWork()

        nextRun = nextRun + interval
        let now = now()

        if nextRun > now {
            nextRun = timer(nextRun)!  // returns when it fired
        }
        // else: we're behind, run immediately
    }
}
```

### 6.3 Ticker Helper

```relang
fn periodic(): Unit {
    let ticker = Ticker(interval: 1m)

    for tick in ticker {
        doWork()
    }
}
```

---

## 7. Scheduled Workflows

### 7.1 Start at Time

```relang
// Schedule workflow to start at specific time
let handle = scheduleWorkflow(
    workflow: processReports,
    args: (reportId,),
    startAt: Timestamp.parse("2024-12-01T00:00:00Z")
)
```

### 7.2 Cron-Style Scheduling

```relang
// Run every day at midnight
@schedule("0 0 * * *")
fn dailyReport(): Unit {
    generateReport()
    sendReport()
}

// Run every Monday at 9am
@schedule("0 9 * * MON")
fn weeklyDigest(): Unit {
    compileDigest()
}
```

### 7.3 Cron Syntax

```
+------------------- minute (0 - 59)
| +----------------- hour (0 - 23)
| | +--------------- day of month (1 - 31)
| | | +------------- month (1 - 12)
| | | | +----------- day of week (0 - 6, SUN-SAT)
| | | | |
* * * * *
```

---

## 8. Deadline Propagation

### 8.1 Workflow Deadline

```relang
@deadline(1h)
fn mustFinish(): Result {
    // Entire workflow must complete within 1 hour
    step1()!
    step2()!
    step3()!
}
```

### 8.2 Inherited Deadlines

```relang
fn parent(): Result {
    let deadline = now() + 30m

    withDeadline(deadline) {
        // All operations inherit this deadline
        let child = childWorkflow()!
        moreWork()!
    }
}
```

### 8.3 Deadline Context

```relang
fn aware(): Result {
    let remaining = self.deadlineRemaining()

    match {
        remaining > 10m -> doFullProcess()
        remaining > 1m -> doQuickProcess()
        _ -> doMinimalProcess()
    }
}
```

---

## 9. Time-Based Patterns

### 9.1 Retry with Backoff

```relang
fn retryWithBackoff(): Result {
    let delays = [1s, 2s, 4s, 8s, 16s]

    for delay in delays {
        let result = tryOperation()

        match result {
            r: Response -> return r
            _: Failure -> timer(delay)
        }
    }

    Failure { error: RetriesExhausted {} }
}
```

### 9.2 Rate Limiting with Time

```relang
fn rateLimited(): [Result] {
    let results: [Result] = []
    let minInterval = 100ms
    let lastCall = now()

    for item in items {
        // Ensure at least 100ms between calls
        let elapsed = now() - lastCall
        if elapsed < minInterval {
            timer(minInterval - elapsed)
        }

        let result = processItem(item)!
        results = results.append(result)
        lastCall = now()
    }

    results
}
```

### 9.3 Expiration

```relang
fn reservation(): ReservationResult {
    let reservation = createReservation()!
    let expiration = timer(15m)

    // Wait for confirmation or expiration
    select {
        confirm = receive<Confirmation>() -> {
            confirmReservation(reservation)
        }
        _ = expiration -> {
            cancelReservation(reservation)
            ReservationResult.expired()
        }
    }
}
```

---

## 10. Timer Durability

### 10.1 Timers Survive Restarts

```relang
fn durable(): Result {
    let timer = timer(24h)

    // Workflow can crash and restart here
    // Timer continues counting from original start

    timer           // Resumes after 24h total
    doWork()!
}
```

### 10.2 Timer State

Timers are checkpointed:
- Start time recorded at creation
- Remaining duration computed on restart
- Resolution is deterministic

---

## 11. Time Functions

### 11.1 Current Time

```relang
let now = now()    // Deterministic workflow time
```

### 11.2 Time Arithmetic

```relang
let later = now() + 1h
let earlier = now() - 30m
let diff: Duration = later - earlier
```

### 11.3 Time Comparison

```relang
if deadline > now() {
    // Still have time
}

if now() >= targetTime {
    // Time reached
}
```

---

## 12. Best Practices

### 12.1 Prefer Timers Over Polling

```relang
// Bad: busy-waiting
while not condition() {
    timer(100ms)
}

// Good: use signals or select
select {
    signal = receive<Ready>() -> proceed()
    _ = timer(timeout) -> abort()
}
```

### 12.2 Use Deadlines for SLAs

```relang
@deadline(30s)
fn apiHandler(request: Request): Response {
    // Automatically fails if not complete in 30s
    processRequest(request)!
}
```

### 12.3 Consider Time Zones

```relang
// Use UTC internally
let utcTime = Timestamp.parse("2024-12-01T00:00:00Z")

// Convert for display only
let localTime = utcTime.inTimeZone("America/New_York")
```

### 12.4 Reuse Timers When Possible

```relang
// Good: create once, use in select
let deadlineTime = now() + 30s
let deadline = timer(deadlineTime)

select {
    r1 = task1 -> ...
    r2 = task2 -> ...
    _ = deadline -> timeout()
}
```

---

## 13. Summary

| Function | Returns | Description |
|----------|---------|-------------|
| `timer(Duration)` | `*Timestamp` | Timer resolving after duration |
| `timer(Timestamp)` | `*Timestamp` | Timer resolving at timestamp |
| `now()` | `Timestamp` | Current deterministic time |
| `timeout(task, d)` | `*T` | Task with timeout |
| `@schedule("cron")` | — | Cron-scheduled workflow |
| `@deadline(d)` | — | Workflow deadline |
| `Ticker(interval)` | `Iterable` | Periodic execution |

**Key principles:**
- Timers are awaitables: create with `timer(Duration)` or `timer(Timestamp)`, resolve with ``
- Use `now()` for deterministic time
- Timers are durable (survive restarts)
- Compose timers with `and`/`or`/`select` for complex patterns

---

*End of proposal*

# ReLang Concurrency and Resource Limits

*Parallelism Caps and Rate Limiting*

---

## 1. Overview

When coordinating many operations, unbounded parallelism can overwhelm external systems. ReLang provides mechanisms to control concurrency.

**Core principles:**
- Explicit concurrency limits
- Per-action-family rate limiting
- Backpressure support
- Deterministic execution

---

## 2. The Problem

### 2.1 Unbounded Parallelism

```relang
// Starts 10,000 concurrent requests!
let tasks = tenThousandUrls.map { url -> http.get(url) }
let results = and(tasks)!
```

### 2.2 Consequences

- Service overload
- Rate limit violations
- Resource exhaustion
- Cascading failures

---

## 3. Bounded Coordination

### 3.1 Max Concurrency Parameter

```relang
let tasks = urls.map { url -> http.get(url) }
let results = and(tasks, maxConcurrency: 10)!
```

**Behavior:**
- Start first 10 tasks
- As each completes, start next
- Maintain at most 10 in-flight
- Preserve result order

### 3.2 With Or Coordination

```relang
let tasks = endpoints.map { ep -> tryEndpoint(ep) }
let result = or(tasks, maxConcurrency: 3)!
```

**Behavior:**
- Start first 3 tasks
- On failure, start next
- On first success, cancel remaining, return result

---

## 4. Rate Limiting

### 4.1 Per-Action-Family Limits

```relang
@rateLimit(requests: 100, per: 1s)
action family api {
    fn call(endpoint: String): *Response
}
```

### 4.2 Workflow-Level Rate Limit

```relang
fn processAll(items: [Item]): [Result] {
    let limiter = RateLimiter {
        maxRequests: 50,
        window: 1s
    }

    let tasks = items.map { item ->
        limiter.acquire()   // blocks if limit reached
        processItem(item)
    }

    and(tasks)!
}
```

### 4.3 Token Bucket

```relang
let bucket = TokenBucket {
    capacity: 100,
    refillRate: 10,     // tokens per second
    refillInterval: 1s
}

for item in items {
    bucket.acquire(1)           // wait for token
    processItem(item)
}
```

---

## 5. Semaphore Pattern

### 5.1 Workflow Semaphore

```relang
fn parallel(items: [Item]): [Result] {
    let sem = Semaphore(permits: 5)

    let tasks = items.map { item ->
        sem.acquire()
        let result = processItem(item)
        sem.release()
        result
    }

    and(tasks)!
}
```

### 5.2 Named Semaphore (Cross-Workflow)

```relang
fn process(item: Item): Result {
    // Shared across all workflow instances
    let sem = Semaphore.named("api-limit", permits: 10)

    sem.acquire()
    let result = callApi(item)
    sem.release()

    result
}
```

---

## 6. Backpressure

### 6.1 Queue with Bounded Capacity

```relang
fn consumer(): Unit {
    let queue = BoundedQueue<Item>(capacity: 100)

    // Producer side
    for item in items {
        queue.put(item)     // blocks if full
    }

    // Consumer side
    while let item = await queue.take() {
        processItem(item)
    }
}
```

### 6.2 Dropping Strategies

```relang
let queue = BoundedQueue<Item> {
    capacity: 100,
    onFull: DropOldest       // or: DropNewest, Block, Reject
}
```

---

## 7. Chunked Processing

### 7.1 Manual Batching

```relang
fn processAll(items: [Item]): [Result] {
    let batches = items.chunked(50)
    let allResults: [Result] = []

    for batch in batches {
        let tasks = batch.map { item -> processItem(item) }
        let results = and(tasks)!
        allResults = allResults + results

        // Optional: add delay between batches
        self.sleep(100ms)
    }

    allResults
}
```

### 7.2 Chunked Coordination

```relang
let results = and(tasks, chunkSize: 50, delayBetweenChunks: 100ms)!
```

---

## 8. Circuit Breaker

### 8.1 Basic Circuit Breaker

```relang
let breaker = CircuitBreaker {
    failureThreshold: 5,
    resetTimeout: 30s
}

fn callService(): Response | Failure {
    if breaker.isOpen() {
        return Failure { error: CircuitOpen {} }
    }

    let result = service.call()

    match result {
        r: Response -> {
            breaker.recordSuccess()
            r
        }
        f: Failure -> {
            breaker.recordFailure()
            f
        }
    }
}
```

### 8.2 Circuit Breaker States

```
Closed ──[failures > threshold]──> Open
   ▲                                 │
   │                         [timeout]
   │                                 ▼
   └────────[success]───────── Half-Open
                                     │
                             [failure]
                                     ▼
                                   Open
```

---

## 9. Resource Pools

### 9.1 Connection Pool

```relang
let pool = ConnectionPool {
    minConnections: 5,
    maxConnections: 20,
    idleTimeout: 5m
}

fn query(sql: String): QueryResult {
    let conn = pool.acquire()
    let result = conn.execute(sql)
    pool.release(conn)
    result
}
```

### 9.2 Worker Pool

```relang
let workers = WorkerPool {
    size: 10,
    queueSize: 100
}

fn processAll(items: [Item]): [Result] {
    let futures = items.map { item ->
        workers.submit { processItem(item) }
    }
    and(futures)!
}
```

---

## 10. Fairness

### 10.1 Fair Semaphore

```relang
let sem = Semaphore(permits: 5, fair: true)
// FIFO ordering for waiting workflows
```

### 10.2 Priority Queue

```relang
let queue = PriorityQueue<Task> {
    comparator: { a, b -> a.priority - b.priority }
}
```

---

## 11. Determinism

### 11.1 Concurrency Control Is Deterministic

All concurrency primitives are deterministic on replay:

```relang
fn example(): [Result] {
    let tasks = items.map { item -> processItem(item) }
    let results = and(tasks, maxConcurrency: 5)!
    results  // Same order, same values on replay
}
```

### 11.2 How It Works

- Concurrency decisions are recorded in workflow history
- Replay follows the same execution order
- Results are memoized, not re-executed

---

## 12. Configuration

### 12.1 Default Limits

```relang
// In workflow or module configuration
@defaults {
    http.maxConcurrency: 100
    db.maxConcurrency: 20
    grpc.maxConcurrency: 50
}
```

### 12.2 Per-Workflow Override

```relang
@maxConcurrency(http: 10, db: 5)
fn conservative(): Result {
    // Uses lower limits
}
```

---

## 13. Summary

| Mechanism | Use Case |
|-----------|----------|
| `maxConcurrency` | Bound parallel operations |
| `RateLimiter` | Requests per time window |
| `TokenBucket` | Smooth rate limiting |
| `Semaphore` | Limit concurrent access |
| `BoundedQueue` | Backpressure |
| `CircuitBreaker` | Fail-fast on errors |
| `ConnectionPool` | Reuse resources |
| `chunked` | Batch processing |

**Key principles:**
- Explicit limits over implicit defaults
- Deterministic execution on replay
- Protect external systems from overload
- Graceful degradation with backpressure

---

*End of proposal*

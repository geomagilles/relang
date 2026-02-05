# ReLang Testing Proposal

*Design exploration for testing support in ReLang*

---

## 1. Testing Challenges Unique to ReLang

ReLang's design creates specific testing requirements:

| Challenge | Why It's Hard | What We Need |
|-----------|---------------|--------------|
| Awaitable resolution | `*T` types require resolution to observe | Control over when/how awaitables resolve |
| Failure simulation | Actions have sealed error families | Inject specific failure payloads |
| Coordination testing | `and`/`or` have subtle edge cases | Control timing and ordering |
| Cancellation verification | Cancellation is best-effort | Observe cancellation requests |
| Timeout testing | Real time makes tests slow/flaky | Virtual/controlled time |
| Checkpoint/resume | State must survive serialization | Test resumption from any checkpoint |
| Determinism | Distributed systems are non-deterministic | Reproducible execution |

---

## 2. Design Principles

### 2.1 Tests as Values

Following ReLang's data-oriented philosophy, test outcomes should be values, not exceptions:

```relang
type TestResult = Pass | Fail { reason: String, location: Location }
```

### 2.2 Explicit Effect Control

Tests should explicitly control effects rather than rely on implicit mocking:

```relang
// Bad: implicit global mock
mock(http.get, fn(_) -> response)
let r = await http.get(url)

// Good: explicit effect binding
let r = await http.get(url) with { http: mockHttp }
```

### 2.3 Composition Over Configuration

Test utilities should be small, composable pieces:

```relang
// Compose test behaviors
let test = scenario
  |> withTimeout(5s)
  |> withFailureInjection(httpFailures)
  |> withVirtualTime
```

### 2.4 First-Class Time Control

Virtual time should be a core testing primitive, not an afterthought.

---

## 3. Proposed Syntax

### 3.1 Test Declaration

```relang
test "descriptive name" {
  // test body
  // implicit: returns TestResult
}

test "grouped tests" {
  test "subtest 1" { ... }
  test "subtest 2" { ... }
}
```

Tests are first-class because:
- They need special runtime support (effect control, time control)
- They benefit from dedicated syntax for clarity
- The compiler can provide better diagnostics

### 3.2 Assertions

Assertions return `TestResult`, allowing composition:

```relang
test "user creation" {
  let user = createUser("alice")

  assert user.name == "alice"
  assert user.id != ""
  assertThat user.createdAt, isRecent(within: 1s)
}
```

Assertion failures include rich context:

```relang
// On failure:
Fail {
  reason: "expected user.name == 'alice', got 'bob'"
  location: Location { file: "user_test.rl", line: 5 }
  context: { user: User { name: "bob", ... } }
}
```

### 3.3 Awaitable Assertions

Special assertions for awaitable-specific properties:

```relang
test "http request succeeds" {
  let t: *Response = http.get(url)

  // Assert on the awaitable itself
  assertResolves t                      // eventually resolves
  assertSucceeds t                      // resolves to success
  assertFails t                         // resolves to Failure
  assertFailsWith t, Timeout            // fails with specific error
  assertCancellable t                   // responds to cancellation

  // Assert on the resolved value
  let r = await t
  assertSuccess r                       // r is T, not Failure
  assertFailure r                       // r is Failure
}
```

---

## 4. Effect Control: The `with` Block

### 4.1 Basic Syntax

The `with` block binds effect implementations for a scope:

```relang
test "handles http failure" {
  let result = with { http: failingHttp(DnsFailure { host: "api.com" }) } {
    await http.get("https://api.com/data")
  }

  assertFailure result
  match result {
    f: Failure -> assert f.error is DnsFailure
  }
}
```

### 4.2 Mock Builders

Fluent API for constructing test doubles:

```relang
let mockHttp = http.mock()
  .on(get: "https://api.com/users")
    .returns(Response { status: 200, body: usersJson })
  .on(get: "https://api.com/orders")
    .failsWith(Timeout { duration: 5s })
  .on(get: _)
    .failsWith(HttpStatusError { status: 404 })
```

### 4.3 Call Verification

```relang
test "retries on failure" {
  let tracker = http.tracking()

  with { http: tracker } {
    await retry(http.get(url), maxAttempts: 3)
  }

  assertThat tracker.calls(get: url), hasCount(3)
}
```

---

## 5. Time Control

### 5.1 Virtual Time

Tests run in virtual time by default:

```relang
test "timeout fires" {
  let t = http.get(url)  // mock that never resolves

  let result = await timeout(t, 5s)

  assertFailsWith result, Timeout
}
// Test completes instantly, no real 5s wait
```

### 5.2 Explicit Time Advancement

For fine-grained control:

```relang
test "retry backoff timing" {
  let clock = virtualClock()

  with { time: clock } {
    let t = retry(http.get(url), exponentialBackoff(initial: 1s))

    // First attempt immediate
    clock.advance(0s)
    assertThat tracker.attempts, equals(1)

    // Second attempt after 1s
    clock.advance(1s)
    assertThat tracker.attempts, equals(2)

    // Third attempt after 2s more
    clock.advance(2s)
    assertThat tracker.attempts, equals(3)
  }
}
```

### 5.3 Real Time (Opt-In)

For integration tests that need real timing:

```relang
test "integration: actual http call" with realTime {
  let r = await timeout(http.get("https://httpbin.org/get"), 10s)
  assertSuccess r
}
```

---

## 6. Coordination Testing

### 6.1 Controlled Resolution Order

```relang
test "and fails fast on first failure" {
  let (t1, resolve1, fail1) = controllable[User]()
  let (t2, resolve2, fail2) = controllable[Orders]()

  let combined = t1 and t2

  // Fail t1
  fail1(DbError { message: "connection lost" })

  // combined should immediately fail
  let r = await combined
  assertFailure r

  // t2 should receive cancellation
  assertCancelled t2
}
```

### 6.2 Race Condition Testing

```relang
test "or returns first success" {
  let (t1, resolve1, _) = controllable[Data]()
  let (t2, resolve2, _) = controllable[Data]()

  let raced = t1 or t2

  // t2 wins the race
  resolve2(Data { value: "from t2" })

  let r = await raced
  match r {
    d: Data -> assert d.value == "from t2"
  }

  // t1 should receive cancellation
  assertCancelled t1
}
```

### 6.3 All Permutations

```relang
test "and works regardless of completion order" {
  forAllPermutations [t1, t2, t3] as order {
    let combined = t1 and t2 and t3

    for t in order {
      resolve(t)
    }

    assertSuccess await combined
  }
}
```

---

## 7. Failure Injection

### 7.1 Deterministic Failures

```relang
test "handles intermittent failures" {
  let flakyHttp = http.mock()
    .on(get: url)
    .sequence([
      failsWith(Timeout { duration: 1s }),
      failsWith(Timeout { duration: 1s }),
      returns(Response { status: 200 })
    ])

  with { http: flakyHttp } {
    let r = await retry(http.get(url), maxAttempts: 3)
    assertSuccess r
  }
}
```

### 7.2 Probabilistic Failures (Deterministic Seed)

```relang
test "resilient to random failures" with seed(12345) {
  let chaosHttp = http.mock()
    .on(get: _)
    .failsRandomly(probability: 0.3, with: Timeout { duration: 1s })

  with { http: chaosHttp } {
    // Test behavior under chaos
  }
}
```

---

## 8. Checkpoint/Resume Testing

### 8.1 Resume from Checkpoint

```relang
test "workflow resumes correctly" {
  let workflow = fn() {
    let user = await db.getUser(id)!
    checkpoint
    let orders = await db.getOrders(user.id)!
    user & orders
  }

  // Run to first checkpoint
  let state = runUntilCheckpoint(workflow)

  // Serialize and deserialize (simulates process restart)
  let restored = deserialize(serialize(state))

  // Resume from checkpoint
  let result = resumeFrom(restored)

  assertSuccess result
}
```

### 8.2 Checkpoint State Assertions

```relang
test "checkpoint captures correct state" {
  let state = runUntilCheckpoint(workflow)

  assertThat state.locals["user"], equals(expectedUser)
  assertThat state.pendingAwaitables, isEmpty
}
```

---

## 9. Property-Based Testing

### 9.1 Awaitable Properties

```relang
property "and is associative" {
  forAll (t1: *A, t2: *B, t3: *C) {
    let left  = await ((t1 and t2) and t3)
    let right = await (t1 and (t2 and t3))

    // Same outcome (modulo tuple nesting)
    equivalent(left, right)
  }
}
```

### 9.2 Failure Properties

```relang
property "or aggregates all failures" {
  forAll (failures: [Failure]) where failures.length >= 2 {
    let awaitables = failures.map(f -> failing(f))
    let result = await or(awaitables)

    match result {
      f: Failure -> match f.error {
        agg: AggregateFailure ->
          assert agg.failures.length == failures.length
      }
    }
  }
}
```

---

## 10. Test Organization

### 10.1 Test Modules

```relang
// user_test.rl
module user_test

import testing { test, assert, ... }
import app.user { createUser, deleteUser }

test "user lifecycle" {
  test "creation" { ... }
  test "update" { ... }
  test "deletion" { ... }
}
```

### 10.2 Setup/Teardown

```relang
test "database operations" {
  setup {
    let db = testDatabase()
    db.migrate()
  }

  teardown {
    db.reset()
  }

  test "insert" { ... }
  test "query" { ... }
}
```

### 10.3 Shared Fixtures

```relang
fixture testUser: User = User {
  id: "test-123"
  name: "Test User"
  email: "test@example.com"
}

test "user display" using testUser {
  assert formatUser(testUser) == "Test User <test@example.com>"
}
```

---

## 11. Running Tests

### 11.1 CLI

```bash
# Run all tests
relang test

# Run specific file
relang test user_test.rl

# Run specific test
relang test "user lifecycle/creation"

# With coverage
relang test --coverage

# With specific seed for reproducibility
relang test --seed 12345
```

### 11.2 Test Output

```
Running tests...

user_test.rl
  user lifecycle
    creation ........................ PASS (2ms)
    update .......................... PASS (1ms)
    deletion ........................ FAIL (3ms)
      Expected: user.deletedAt != null
      Got: user.deletedAt == null
      at user_test.rl:45

http_test.rl
  retry behavior
    retries on timeout .............. PASS (0ms)  [virtual time]
    gives up after max attempts ..... PASS (0ms)  [virtual time]

Results: 4 passed, 1 failed
```

---

## 12. Design Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Test syntax | First-class `test` keyword | Compiler support, clarity |
| Assertions | Return `TestResult` | Data-oriented, composable |
| Effect control | `with` blocks | Explicit, scoped, composable |
| Time model | Virtual by default | Fast, deterministic |
| Mocking | Builder pattern | Fluent, type-safe |
| Checkpoints | First-class test support | Core to ReLang's value |

---

## 13. Open Questions

1. **Should `test` be an expression or statement?**
   - Expression: `let results = test "..." { ... }` enables programmatic test generation
   - Statement: simpler mental model

2. **How do we handle test isolation?**
   - Fresh effect bindings per test (default)
   - Shared fixtures opt-in

3. **Should property-based testing be built-in or a library?**
   - Built-in: better integration with awaitable types
   - Library: smaller core language

4. **How do we test actual distributed deployments?**
   - Separate integration test framework?
   - Same framework with different effect bindings?

5. **Test parallelism?**
   - Parallel by default (isolated effects make this safe)
   - Sequential opt-in for tests with shared state

---

## 14. Comparison with Other Systems

| System | Approach | ReLang Difference |
|--------|----------|-------------------|
| Temporal | Activity mocking, time skipping | More explicit effect control |
| Rust | `#[test]` + panic-based assertions | Data-oriented assertions |
| Go | Table-driven tests | First-class awaitable testing |
| Jest | Mock functions, fake timers | Typed mocks, virtual time built-in |
| QuickCheck | Property-based | Integrated with awaitable semantics |

---

## 15. Next Steps

1. **Validate core concepts** - Are `with` blocks the right abstraction?
2. **Prototype mock builders** - Type-safe mock construction
3. **Design virtual time semantics** - How does it interact with coordination?
4. **Checkpoint testing details** - State serialization format for tests
5. **Property testing for awaitables** - Generation strategies for `*T`

---

*End of proposal*

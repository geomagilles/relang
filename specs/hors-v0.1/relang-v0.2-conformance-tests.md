# ReLang v0.2 - Conformance Test Matrix

Status: Draft test specification  
Related: `relang-spec-v0.2-proposition.md`

## 1. Test format

Each test is specified as:

1. `Given` initial state/program setup,
2. `When` transition/execution step,
3. `Then` expected observable result.

Pass criteria:

1. Language result matches expected value/failure shape,
2. Snapshot/resume behavior matches expected transitions,
3. Runtime-only diagnostics do not alter language-level contracts.

## 2. Transition tests

### CT-001 - create_effect(action)

Given:

1. Program evaluates `http.get(url)`.

When:

1. Runtime performs `create_effect(kind=action, payload=http.get(url))`.

Then:

1. Handle `*T(aid)` is returned,
2. `A[aid] = Pending`,
3. no suspension occurs at creation time.

### CT-002 - create_effect(child)

Given:

1. Program evaluates `spawn childFn(x)`.

When:

1. Runtime performs `create_effect(kind=child, payload=spawn childFn(x))`.

Then:

1. Parent receives handle `*T(aid)`,
2. `A[aid] = Pending` in parent,
3. child execution is created with `childExecutionId`.

### CT-003 - await_resolved success

Given:

1. `A[aid] = Succeeded(v)`,
2. instruction is `await h(aid)`.

When:

1. Interpreter evaluates `await`.

Then:

1. expression result is `v`,
2. no snapshot is produced.

### CT-004 - await_resolved failure

Given:

1. `A[aid] = Failed(f)`,
2. instruction is `await h(aid)`.

When:

1. Interpreter evaluates `await`.

Then:

1. expression result is `f` (`Failure`),
2. no snapshot is produced.

### CT-005 - await_suspend

Given:

1. `A[aid] = Pending`,
2. instruction is `await h(aid)`.

When:

1. Interpreter reaches `await`.

Then:

1. execution returns `Suspended(snapshot, waitingOn=aid)`,
2. snapshot includes stack/env/pc/awaitables metadata required for resume.

### CT-006 - resume

Given:

1. snapshot `S` waiting on `aid`,
2. runtime event resolves `aid -> Succeeded(v)`.

When:

1. runtime resumes from `S`.

Then:

1. machine continues at same `pc`,
2. `await` now yields `v`,
3. no re-dispatch of already resolved effect.

## 3. Root termination tests

### CT-007 - root normal completion

Given:

1. root function returns value `r`.

When:

1. execution reaches end.

Then:

1. terminal status is `Completed(r)`.

### CT-008 - root unhandled failure

Given:

1. root path propagates `Failure f`,
2. no handler consumes `f`.

When:

1. execution reaches root boundary.

Then:

1. terminal status is `Failed(f)`.

## 4. Failure propagation tests

### CT-009 - inline propagation keeps same failure

Given:

1. `inner()` inline returns/propagates `Failure f`.

When:

1. `outer()` calls `inner()` inline.

Then:

1. `outer()` observes same failure id (`f.id` unchanged),
2. no `FunctionFailed` wrapper appears.

### CT-010 - spawn propagation wraps failure

Given:

1. child execution fails with `fChild`,
2. parent does `await spawn child()`.

When:

1. parent observes child completion.

Then:

1. parent gets `fParent.kind = FunctionFailed`,
2. `fParent.cause = fChild`,
3. `fParent.id != fChild.id`.

### CT-011 - nested spawn chain

Given:

1. grandChild fails leaf action,
2. child awaits grandChild via spawn,
3. parent awaits child via spawn.

When:

1. parent receives final failure.

Then:

1. failure chain contains two `FunctionFailed` wrappers,
2. root cause leaf remains accessible via `failure.root()`.

### CT-012 - bang propagation

Given:

1. `(await h)!` where `A[aid] = Failed(f)`.

When:

1. expression is evaluated.

Then:

1. failure is propagated directly in same execution,
2. no additional local wrapper created by `!`.

## 5. Coordination tests

### CT-013 - and fail-fast

Given:

1. `t1 and t2 and t3`,
2. `t2` fails first.

When:

1. `await` on composed awaitable is evaluated.

Then:

1. returned failure is the first observed failure,
2. no `AllFailed` wrapper.

### CT-014 - or first success wins

Given:

1. `t1 or t2 or t3`,
2. `t2` succeeds first.

When:

1. composed awaitable resolves.

Then:

1. language result is `t2` success value,
2. loser failures do not change language result type.

### CT-015 - or all-failed aggregation shape

Given:

1. `t1 or t2 or t3`,
2. all branches fail.

When:

1. composed awaitable resolves.

Then:

1. returned failure kind is `AllFailed`,
2. `causes.length = 3`.

### CT-016 - or all-failed lexical ordering

Given:

1. source order is `(tA or tB or tC)`,
2. completion order is `tC`, then `tA`, then `tB`,
3. all fail.

When:

1. `AllFailed` is built.

Then:

1. `causes` are ordered `[fA, fB, fC]` (lexical source order).

### CT-017 - loser diagnostics are runtime-only

Given:

1. `or` winner found,
2. at least one loser failed.

When:

1. result is returned to language user.

Then:

1. language result is only winner value,
2. loser details are accessible only in runtime diagnostics/reporting.

## 6. Timer and signal tests

### CT-018 - timeout by composition

Given:

1. expression `(task or timer(30s))`,
2. `task` remains pending beyond 30s.

When:

1. timer branch resolves first.

Then:

1. expression resolves with timer branch value,
2. this is accepted as canonical timeout pattern (no `timeout(...)` primitive required).

### CT-019 - receive suspension/resume

Given:

1. `await receive<Approval>()`,
2. no signal available.

When:

1. interpreter reaches await.

Then:

1. execution suspends with snapshot.

And when:

1. signal `Approval` is delivered.

Then:

1. resume continues and returns signal payload.

## 7. Snapshot and replay-safety tests

### CT-020 - snapshot contains referenced awaitables

Given:

1. multiple awaitables created,
2. only subset reachable from current env/frames.

When:

1. suspension snapshot is produced.

Then:

1. referenced awaitables are persisted,
2. non-referenced completed awaitables may be omitted.

### CT-021 - no duplicate effect after resume

Given:

1. action `a1` already resolved before suspension,
2. execution resumes later.

When:

1. runtime continues.

Then:

1. `a1` is not re-dispatched,
2. its memoized resolution is reused.

## 8. Version compatibility tests

### CT-022 - same codeVersion resume allowed

Given:

1. snapshot has `codeVersion = X`,
2. runtime currently runs `codeVersion = X`.

When:

1. resume is requested.

Then:

1. resume is accepted.

### CT-023 - changed codeVersion without migration rejected

Given:

1. snapshot has `codeVersion = X`,
2. runtime currently runs `codeVersion = Y`,
3. no migration declared.

When:

1. resume is requested.

Then:

1. resume is rejected with compatibility error.

### CT-024 - changed codeVersion with migration accepted

Given:

1. snapshot has `codeVersion = X`,
2. runtime currently runs `codeVersion = Y`,
3. valid migration `stateVersion n -> n+1` is declared and succeeds.

When:

1. resume is requested.

Then:

1. migrated state is produced,
2. resume proceeds under new version constraints.

## 9. Compliance profile

A runtime is "ReLang v0.2 transition-compliant" if:

1. CT-001 .. CT-024 all pass,
2. no language-level contract mismatch is observed in failure shape or propagation behavior,
3. ordering invariants (`AllFailed.causes` lexical order) are preserved.


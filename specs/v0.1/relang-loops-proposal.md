# ReLang Loops

*Iteration and Loop Control*

---

## 1. Overview

ReLang provides two loop constructs:

| Construct | Use case |
|-----------|----------|
| `for` | Iterate over collections and ranges |
| `while` | Condition-based looping |

Both are **statements** (return `Unit`), not expressions.

---

## 2. For-In Loop

### 2.1 Basic Form

```relang
for item in collection {
    // body
}
```

Iterates over each element in `collection`.

### 2.2 Over Lists

```relang
let names = ["Alice", "Bob", "Carol"]

for name in names {
    print("Hello, ${name}")
}
```

### 2.3 Over Ranges

```relang
// Exclusive end
for i in 0..5 {
    print(i)    // 0, 1, 2, 3, 4
}

// Inclusive end
for i in 1..=5 {
    print(i)    // 1, 2, 3, 4, 5
}

// With step
for i in (0..10).step(2) {
    print(i)    // 0, 2, 4, 6, 8
}
```

### 2.4 With Index

```relang
for (index & value) in items.enumerate() {
    print("${index}: ${value}")
}
```

### 2.5 Destructuring in For

```relang
// Over pairs
let pairs = [(1 & "a"), (2 & "b"), (3 & "c")]
for (num & letter) in pairs {
    print("${num} -> ${letter}")
}

// Over types
let users = [User { name: "Alice", age: 30 }, User { name: "Bob", age: 25 }]
for User { name, age } in users {
    print("${name} is ${age}")
}
```

---

## 3. While Loop

### 3.1 Basic Form

```relang
while condition {
    // body
}
```

Repeats while `condition` is `true`.

### 3.2 Example

```relang
let items = getMutableQueue()
while not items.isEmpty() {
    let item = items.dequeue()
    process(item)
}
```

### 3.3 Condition Must Be Bool

```relang
while count { ... }         // ✗ Compile error: Int is not Bool
while items { ... }         // ✗ Compile error: List is not Bool
while items.length() > 0 { ... }  // ✓ OK
```

---

## 4. Loop Scope

### 4.1 Fresh Scope Per Iteration

Each iteration of a loop creates a **fresh scope**. Variables declared with `let` inside the loop are local to that iteration only:

```relang
for name in names {
    let greeting = "Hello, ${name}"    // fresh 'greeting' each iteration
    print(greeting)
}
// greeting is out of scope here
```

### 4.2 Interaction with Outer Variables

Reassignment affects outer variables; `let` creates iteration-local variables:

```relang
let count = 0
for name in names {
    count = count + 1        // reassigns outer count (persists across iterations)
    let msg = "Hi ${name}"   // local to this iteration (discarded at end)
}
print(count)                 // total number of iterations
```

### 4.3 Shadowing Does Not Persist

Because each iteration has a fresh scope, shadowing inside a loop does not affect subsequent iterations:

```relang
let i = 0
for name in names {
    i = i + 1            // outer i: 1, 2, 3, ...
    print(i)             // prints the incremented outer i
    let i = name         // local i (String) shadows outer for rest of this iteration
    print(i)             // prints the name
}                        // local i discarded, outer i (Int) still accessible
// After loop: i == len(names)
```

This code is valid but confusing — avoid using the same name for different purposes.

---

## 5. Loop Control

### 5.1 Break

Exit the loop immediately:

```relang
for item in items {
    if item.isTerminal() {
        break
    }
    process(item)
}
```

### 5.2 Continue

Skip to the next iteration:

```relang
for item in items {
    if item.shouldSkip() {
        continue
    }
    process(item)
}
```

### 5.3 No Labeled Breaks

ReLang does not support labeled breaks. Use early return or restructure:

```relang
// Instead of labeled break, use a function
fn findFirst(matrix: [[Int]], target: Int): (Int & Int)? {
    for (i & row) in matrix.enumerate() {
        for (j & value) in row.enumerate() {
            if value == target {
                return (i & j)
            }
        }
    }
    none
}
```

---

## 6. Loop Expressions (None)

Loops are statements, not expressions:

```relang
// ✗ This does NOT work
let result = for x in items { x * 2 }

// ✓ Use functional operations instead
let result = items.map { x -> x * 2 }
```

**Rationale**: Loops are for side effects. Transformations should use `map`, `filter`, `reduce`.

---

## 7. Infinite Loops

### 7.1 While True

```relang
while true {
    let event = waitForEvent()
    if event.isShutdown() {
        break
    }
    handleEvent(event)
}
```

### 7.2 With Await

Loops can contain await expressions:

```relang
while true {
    let task = getNextTask()

    match task {
        t: Task -> processTask(t)
        none -> break
    }
}
```

**Note**: Each `await` is a potential checkpoint. The loop counter/state must be reconstructible.

---

## 8. Functional Alternatives

Prefer functional operations over loops when transforming data:

| Loop pattern | Functional alternative |
|--------------|----------------------|
| Transform each | `list.map { ... }` |
| Keep matching | `list.filter { ... }` |
| Find first | `list.find { ... }` |
| Check existence | `list.any { ... }` |
| Check all | `list.all { ... }` |
| Accumulate | `list.reduce(init) { ... }` |
| Side effects | `list.forEach { ... }` |

```relang
// ✗ Imperative style
let results: [Int] = []
for x in items {
    if x > 0 {
        results = results.append(x * 2)
    }
}

// ✓ Functional style
let results = items.filter { x -> x > 0 }.map { x -> x * 2 }
```

---

## 9. Durability in Loops

### 9.1 Await in Loops

When a loop contains `await`, each iteration is a potential checkpoint:

```relang
for url in urls {
    let response = await http.get(url)!   // checkpoint after each
    process(response)
}
```

On resume from snapshot:
- Completed iterations are restored from persisted resolved results
- Execution resumes at the correct iteration

### 9.2 Loop Invariants

Loop variables must be deterministically reconstructible:

```relang
// ✓ OK: index is deterministic
for i in 0..items.length() {
    let result = processItem(items[i])!
}

// ✓ OK: for-in tracks position
for item in items {
    let result = processItem(item)!
}
```

---

## 10. Performance Considerations

### 10.1 Early Exit

Use `break` or `return` when the answer is found:

```relang
fn contains(list: [Int], target: Int): Bool {
    for x in list {
        if x == target {
            return true
        }
    }
    false
}
```

### 10.2 Avoid Await in Tight Loops

Each blocking `await` creates a checkpoint. For bulk operations:

```relang
// ✗ Slow: many checkpoints
for item in items {
    processItem(item)
}

// ✓ Better: single checkpoint for all
let tasks = items.map { item -> processItem(item) }
(await and(tasks))!
```

---

## 11. Summary

| Syntax | Meaning |
|--------|---------|
| `for x in list { }` | Iterate over list |
| `for i in 0..n { }` | Iterate over range (exclusive) |
| `for i in 0..=n { }` | Iterate over range (inclusive) |
| `for (i & v) in list.enumerate() { }` | Iterate with index |
| `while cond { }` | Condition-based loop |
| `break` | Exit loop |
| `continue` | Skip to next iteration |

**Key principles:**
- Loops are statements (return `Unit`)
- Prefer functional operations for data transformation
- Loops can contain `await` (each is a checkpoint)
- No labeled breaks — use functions for complex control flow

---

*End of proposal*

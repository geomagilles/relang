# ReLang Collections

*Lists and Collection Operations*

---

## 1. Overview

ReLang provides one collection type: **List**. Lists are:

- **Ordered** — Elements maintain their insertion order
- **Typed** — Element type is declared (can be a sum type)
- **Immutable** — Operations return new lists, never mutate
- **Serializable** — Can be checkpointed and resumed

---

## 2. List Type

### 2.1 Type Syntax

`[T]` where `T` is the element type:

```relang
[Int]           // list of integers
[String]        // list of strings
[User]          // list of User types
[[Int]]         // list of lists (nested)
[Int | String]  // list of sum type elements
```

### 2.2 Literal Syntax

```relang
[]                      // empty list (type inferred from context)
[1, 2, 3]               // [Int]
["a", "b", "c"]         // [String]
[true, false]           // [Bool]
```

**Trailing comma** allowed:
```relang
let items = [
    "first",
    "second",
    "third",
]
```

### 2.3 Type Annotation

Explicit type annotation for empty lists or mixed types:

```relang
let empty: [Int] = []
let mixed: [Int | String] = [1, "two", 3]
```

---

## 3. Basic Operations

### 3.1 Length and Empty Check

```relang
[1, 2, 3].length()      // 3
[1, 2, 3].isEmpty()     // false
[].isEmpty()            // true
```

### 3.2 Index Access

```relang
let list = [10, 20, 30]

// Direct access (raises Failure if out of bounds)
list[0]                 // 10
list[2]                 // 30
list[-1]                // 30 (negative index from end)
list[-2]                // 20
list[10]                // raises Failure: index out of bounds

// Safe access (returns T?)
list[0]?                // Some(10)
list[10]?               // None
list[-1]?               // Some(30)
```

### 3.3 First and Last

```relang
[1, 2, 3].first()       // Some(1)
[1, 2, 3].last()        // Some(3)
[].first()              // None
[].last()               // None
```

### 3.4 Slicing

```relang
let list = [0, 1, 2, 3, 4]

list[1..3]              // [1, 2] (exclusive end)
list[1..=3]             // [1, 2, 3] (inclusive end)
list[2..]               // [2, 3, 4] (to end)
list[..3]               // [0, 1, 2] (from start)
list[..]                // [0, 1, 2, 3, 4] (copy)
```

**Negative indices in slices:**
```relang
list[..-1]              // [0, 1, 2, 3] (all but last)
list[-3..]              // [2, 3, 4] (last 3 elements)
```

---

## 4. Adding Elements

All operations return a **new list** — the original is unchanged.

```relang
let list = [1, 2, 3]

// Concatenation
list + [4, 5]           // [1, 2, 3, 4, 5]
[0] + list              // [0, 1, 2, 3]

// Single element
list.append(4)          // [1, 2, 3, 4]
list.prepend(0)         // [0, 1, 2, 3]

// Insert at index
list.insert(1, 99)      // [1, 99, 2, 3]
list.insert(0, 99)      // [99, 1, 2, 3]
list.insert(-1, 99)     // [1, 2, 99, 3] (before last)
```

---

## 5. Removing Elements

```relang
let list = [1, 2, 3, 2, 4]

// By index
list.removeAt(1)        // [1, 3, 2, 4]
list.removeAt(-1)       // [1, 2, 3, 2]

// By value (first occurrence)
list.remove(2)          // [1, 3, 2, 4]

// By value (all occurrences)
list.removeAll(2)       // [1, 3, 4]

// By predicate
list.removeWhere(|x| x > 2)  // [1, 2, 2]
```

---

## 6. Updating Elements

```relang
let list = [1, 2, 3]

// Replace at index
list.set(1, 99)         // [1, 99, 3]
list.set(-1, 99)        // [1, 2, 99]

// Update with function
list.update(1, |x| x * 10)   // [1, 20, 3]
```

---

## 7. Reordering

```relang
let list = [3, 1, 4, 1, 5]

// Sort (ascending)
list.sorted()           // [1, 1, 3, 4, 5]

// Sort with comparator
list.sortedBy(|x| -x)   // [5, 4, 3, 1, 1] (descending)

// Reverse
list.reversed()         // [5, 1, 4, 1, 3]

// Shuffle (deterministic in workflow context)
list.shuffled()         // random order
```

---

## 8. Transformation

### 8.1 Map

Transform each element:

```relang
[1, 2, 3].map(|x| x * 2)                // [2, 4, 6]
["a", "b"].map(|s| s.toUpperCase())     // ["A", "B"]

// With index
[10, 20, 30].mapIndexed(|i, x| i + x)   // [10, 21, 32]
```

### 8.2 Filter

Keep elements matching a predicate:

```relang
[1, 2, 3, 4, 5].filter(|x| x > 2)       // [3, 4, 5]
[1, 2, 3, 4, 5].filter(|x| x % 2 == 0)  // [2, 4]

// Opposite: reject
[1, 2, 3, 4, 5].reject(|x| x > 2)       // [1, 2]
```

### 8.3 Reduce

Combine elements into a single value:

```relang
[1, 2, 3, 4].reduce(0, |acc, x| acc + x)    // 10
[1, 2, 3, 4].reduce(1, |acc, x| acc * x)    // 24

// Without initial value (uses first element)
[1, 2, 3, 4].reduce(|acc, x| acc + x)       // 10
[].reduce(|acc, x| acc + x)                 // raises: empty list
```

### 8.4 Fold

Like reduce but can change type:

```relang
["a", "bb", "ccc"].fold(0, |acc, s| acc + s.length())  // 6
```

### 8.5 FlatMap

Map and flatten one level:

```relang
[[1, 2], [3, 4]].flatMap(|x| x)             // [1, 2, 3, 4]
[1, 2, 3].flatMap(|x| [x, x * 10])          // [1, 10, 2, 20, 3, 30]
```

### 8.6 Flatten

Flatten nested lists:

```relang
[[1, 2], [3, 4]].flatten()                  // [1, 2, 3, 4]
[[[1]], [[2, 3]]].flatten()                 // [[1], [2, 3]] (one level)
```

---

## 9. Taking and Dropping

### 9.1 By Count

```relang
let list = [1, 2, 3, 4, 5]

list.take(3)            // [1, 2, 3]
list.take(10)           // [1, 2, 3, 4, 5] (no error if fewer)
list.takeLast(2)        // [4, 5]

list.drop(2)            // [3, 4, 5]
list.drop(10)           // [] (no error if fewer)
list.dropLast(2)        // [1, 2, 3]
```

### 9.2 By Predicate

```relang
let list = [1, 2, 3, 4, 5]

list.takeWhile(|x| x < 4)   // [1, 2, 3]
list.dropWhile(|x| x < 3)   // [3, 4, 5]

list.takeLast(|x| x > 2)    // [3, 4, 5]
list.dropLast(|x| x > 2)    // [1, 2]
```

---

## 10. Search and Query

### 10.1 Contains

```relang
[1, 2, 3].contains(2)           // true
[1, 2, 3].contains(9)           // false
```

### 10.2 Index Of

```relang
[1, 2, 3, 2].indexOf(2)         // Some(1) (first occurrence)
[1, 2, 3, 2].lastIndexOf(2)     // Some(3)
[1, 2, 3].indexOf(9)            // None
```

### 10.3 Find

```relang
[1, 2, 3, 4].find(|x| x > 2)        // Some(3) (first match)
[1, 2, 3, 4].findLast(|x| x > 2)    // Some(4) (last match)
[1, 2, 3].find(|x| x > 10)          // None

[1, 2, 3, 4].findIndex(|x| x > 2)   // Some(2)
```

### 10.4 Predicates

```relang
[1, 2, 3].any(|x| x > 2)        // true (at least one)
[1, 2, 3].all(|x| x > 0)        // true (every element)
[1, 2, 3].none(|x| x > 5)       // true (no element)
```

### 10.5 Count

```relang
[1, 2, 3, 4, 5].count(|x| x > 2)    // 3
```

---

## 11. Aggregation

### 11.1 Numeric Aggregation

For lists of `Int` or `Float`:

```relang
[1, 2, 3, 4].sum()          // 10
[1, 2, 3, 4].product()      // 24
[1, 2, 3, 4].average()      // 2.5 (returns Float)

[3, 1, 4, 1, 5].min()       // Some(1)
[3, 1, 4, 1, 5].max()       // Some(5)
[].min()                    // None
```

### 11.2 With Selector

```relang
let users = [
    User { name: "Alice", age: 30 },
    User { name: "Bob", age: 25 }
]

users.minBy(|u| u.age)      // Some(User{name: "Bob", age: 25})
users.maxBy(|u| u.age)      // Some(User{name: "Alice", age: 30})

users.sumBy(|u| u.age)      // 55
```

### 11.3 Join

```relang
["a", "b", "c"].join(", ")      // "a, b, c"
["a", "b", "c"].join("")        // "abc"
[1, 2, 3].join("-")             // "1-2-3" (calls toString)
```

---

## 12. Grouping and Partitioning

### 12.1 Partition

Split into two lists by predicate:

```relang
[1, 2, 3, 4, 5].partition(|x| x % 2 == 0)
// ([2, 4], [1, 3, 5])

let (evens, odds) = [1, 2, 3, 4, 5].partition(|x| x % 2 == 0)
```

### 12.2 Chunked

Split into fixed-size sublists:

```relang
[1, 2, 3, 4, 5].chunked(2)      // [[1, 2], [3, 4], [5]]
[1, 2, 3, 4, 5, 6].chunked(3)   // [[1, 2, 3], [4, 5, 6]]
```

### 12.3 Windowed

Sliding window:

```relang
[1, 2, 3, 4, 5].windowed(3)
// [[1, 2, 3], [2, 3, 4], [3, 4, 5]]

[1, 2, 3, 4, 5].windowed(3, step: 2)
// [[1, 2, 3], [3, 4, 5]]
```

### 12.4 GroupBy

Group elements by key function:

```relang
["apple", "apricot", "banana", "blueberry"].groupBy(|s| s.charAt(0))
// {"a": ["apple", "apricot"], "b": ["banana", "blueberry"]}

[1, 2, 3, 4, 5, 6].groupBy(|x| x % 3)
// {0: [3, 6], 1: [1, 4], 2: [2, 5]}
```

Note: Returns a `Json` object (string keys required for Json).

### 12.5 Distinct

Remove duplicates (preserves first occurrence):

```relang
[1, 2, 2, 3, 3, 3].distinct()           // [1, 2, 3]
["a", "b", "a", "c"].distinct()         // ["a", "b", "c"]

// Distinct by key
let users = [User{name: "Alice", dept: "A"}, User{name: "Bob", dept: "A"}]
users.distinctBy(|u| u.dept)            // [User{name: "Alice", dept: "A"}]
```

---

## 13. Combining Lists

### 13.1 Zip

Combine corresponding elements:

```relang
[1, 2, 3].zip(["a", "b", "c"])
// [(1 & "a"), (2 & "b"), (3 & "c")]

// Shorter list determines length
[1, 2].zip(["a", "b", "c"])
// [(1 & "a"), (2 & "b")]
```

### 13.2 ZipWith

Combine with a function:

```relang
[1, 2, 3].zipWith([10, 20, 30], |a, b| a + b)
// [11, 22, 33]
```

### 13.3 Unzip

Separate pairs into two lists:

```relang
[(1 & "a"), (2 & "b"), (3 & "c")].unzip()
// ([1, 2, 3] & ["a", "b", "c"])

let (nums, strs) = pairs.unzip()
```

---

## 14. Iteration

### 14.1 ForEach

Execute side effect for each element:

```relang
[1, 2, 3].forEach(|x| print(x))
// Returns Unit
```

### 14.2 For-In Loop

```relang
for x in [1, 2, 3] {
    print(x)
}
```

### 14.3 Enumerate

Iterate with index:

```relang
for (index, value) in ["a", "b", "c"].enumerate() {
    print("${index}: ${value}")
}
// 0: a
// 1: b
// 2: c
```

### 14.4 Enumerate Method

```relang
["a", "b", "c"].enumerate()
// [(0 & "a"), (1 & "b"), (2 & "c")]
```

---

## 15. Ranges

Create lists from ranges:

```relang
(1..5).toList()         // [1, 2, 3, 4]
(1..=5).toList()        // [1, 2, 3, 4, 5]
(5..1).toList()         // [] (empty, no auto-reverse)
(1..10).step(2).toList() // [1, 3, 5, 7, 9]
```

---

## 16. Method Summary

### Creation and Access

| Method | Description | Returns |
|--------|-------------|---------|
| `length()` | Number of elements | `Int` |
| `isEmpty()` | True if empty | `Bool` |
| `[i]` | Direct access (raises if out of bounds) | `T` |
| `[i]?` | Safe access | `T?` |
| `first()` | First element | `T?` |
| `last()` | Last element | `T?` |

### Adding Elements

| Method | Description | Returns |
|--------|-------------|---------|
| `+` | Concatenate lists | `[T]` |
| `append(x)` | Add to end | `[T]` |
| `prepend(x)` | Add to start | `[T]` |
| `insert(i, x)` | Insert at index | `[T]` |

### Removing Elements

| Method | Description | Returns |
|--------|-------------|---------|
| `removeAt(i)` | Remove at index | `[T]` |
| `remove(x)` | Remove first occurrence | `[T]` |
| `removeAll(x)` | Remove all occurrences | `[T]` |
| `removeWhere(p)` | Remove matching predicate | `[T]` |

### Updating

| Method | Description | Returns |
|--------|-------------|---------|
| `set(i, x)` | Replace at index | `[T]` |
| `update(i, f)` | Transform at index | `[T]` |

### Reordering

| Method | Description | Returns |
|--------|-------------|---------|
| `sorted()` | Sort ascending | `[T]` |
| `sortedBy(f)` | Sort by key function | `[T]` |
| `reversed()` | Reverse order | `[T]` |
| `shuffled()` | Random order | `[T]` |

### Transformation

| Method | Description | Returns |
|--------|-------------|---------|
| `map(f)` | Transform each element | `[U]` |
| `mapIndexed(f)` | Transform with index | `[U]` |
| `filter(p)` | Keep matching | `[T]` |
| `reject(p)` | Remove matching | `[T]` |
| `reduce(init, f)` | Combine into one | `T` |
| `fold(init, f)` | Combine into different type | `U` |
| `flatMap(f)` | Map and flatten | `[U]` |
| `flatten()` | Flatten one level | `[T]` |

### Taking and Dropping

| Method | Description | Returns |
|--------|-------------|---------|
| `take(n)` | First n elements | `[T]` |
| `takeLast(n)` | Last n elements | `[T]` |
| `drop(n)` | Skip first n | `[T]` |
| `dropLast(n)` | Skip last n | `[T]` |
| `takeWhile(p)` | Take while predicate true | `[T]` |
| `dropWhile(p)` | Drop while predicate true | `[T]` |

### Search

| Method | Description | Returns |
|--------|-------------|---------|
| `contains(x)` | Has element | `Bool` |
| `indexOf(x)` | First index of | `Int?` |
| `lastIndexOf(x)` | Last index of | `Int?` |
| `find(p)` | First match | `T?` |
| `findLast(p)` | Last match | `T?` |
| `findIndex(p)` | Index of first match | `Int?` |

### Predicates

| Method | Description | Returns |
|--------|-------------|---------|
| `any(p)` | At least one matches | `Bool` |
| `all(p)` | All match | `Bool` |
| `none(p)` | None match | `Bool` |
| `count(p)` | Count matching | `Int` |

### Aggregation

| Method | Description | Returns |
|--------|-------------|---------|
| `sum()` | Sum of elements | `T` |
| `product()` | Product of elements | `T` |
| `average()` | Mean value | `Float` |
| `min()` | Minimum element | `T?` |
| `max()` | Maximum element | `T?` |
| `minBy(f)` | Min by key function | `T?` |
| `maxBy(f)` | Max by key function | `T?` |
| `join(sep)` | Join to string | `String` |

### Grouping

| Method | Description | Returns |
|--------|-------------|---------|
| `partition(p)` | Split by predicate | `([T], [T])` |
| `chunked(n)` | Split into chunks | `[[T]]` |
| `windowed(n)` | Sliding windows | `[[T]]` |
| `groupBy(f)` | Group by key | `Json` |
| `distinct()` | Remove duplicates | `[T]` |
| `distinctBy(f)` | Distinct by key | `[T]` |

### Combining

| Method | Description | Returns |
|--------|-------------|---------|
| `zip(other)` | Pair with other list | `[(T & U)]` |
| `zipWith(other, f)` | Combine with function | `[V]` |
| `unzip()` | Separate pairs | `([T], [U])` |
| `enumerate()` | Pair with indices | `[(Int & T)]` |

### Iteration

| Method | Description | Returns |
|--------|-------------|---------|
| `forEach(f)` | Execute for each | `Unit` |

---

## 17. Design Notes

### 17.1 Why Immutable?

1. **Checkpoint safety**: Mutation tracking is complex; immutable values are trivially serializable
2. **Predictability**: A list passed to a function cannot change unexpectedly
3. **Functional style**: Transforms over mutations lead to clearer code

### 17.2 Performance

Immutable operations create new lists, but:
- Many operations share structure with the original (structural sharing)
- For large datasets, consider chunked processing
- The compiler may optimize sequential operations

### 17.3 Why No Set or Map?

ReLang keeps collections minimal:
- **Set behavior**: Use `list.distinct()` or `list.contains()`
- **Map behavior**: Use `Json` for dynamic keys, or user-defined types for static keys

The philosophy: if you know the shape, use types; if you don't, use `Json`.

---

*End of specification*

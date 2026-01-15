---
description: Intelligent refactoring with context-aware strategies
---

# Refactor Command

Parse arguments as: /refactor [scope]

## Objectives:

- Extract reusable components from duplicated code
- Simplify complex methods (>30 lines) into smaller, testable units
- Improve naming consistency for classes, methods, and variables
- Consolidate related functionality into cohesive classes
- Add Javadoc documentation when code is not self-explanatory
- Split large files into focused, single-responsibility classes

## Priority: Focus on these improvements IN ORDER:

1. **Critical**: Fix bugs, memory leaks, null pointer issues, security vulnerabilities
2. **Performance**: Optimize hot paths, reduce allocations in execute methods
3. **Structure**: Extract classes, organize imports, follow Truffle conventions
4. **Readability**: Better names, add Javadoc comments, simplify logic

## ReLang-Specific Patterns:

### AST Nodes (in `nodes/` package)
- All nodes extend `ReLangNode` and implement `executeGeneric(VirtualFrame, ResumableState)`
- Use `@Child` for single child nodes, `@Children` for arrays
- Use `@CompilationFinal` for fields that don't change after parsing
- Keep `execute*` methods small and focused
- Resumable nodes must handle state save/restore correctly

### Parser (`ReLangTruffleParser.java`)
- Visitor methods should be thin, delegating to node constructors
- Keep grammar rule handling separate from AST construction
- Handle all ANTLR parse tree to Truffle AST conversion here

### Resumability System
- `ResumableState` should remain immutable
- `FrameState` captures local variables and execution path
- State bubbles up through `ReLangBlockNode` and `ReLangRootNode`
- Resume path uses block indices to skip completed statements

### Common Patterns
- Use `long` for all numeric values (ReLang's number type)
- Use Truffle's `@Specialization` for type-specialized execute methods if needed
- Follow existing naming: `ReLang[Feature]Node`
- Use `ReLangSuspendException` for checkpoint unwinding

## Idiomatic Java for Truffle

When refactoring, apply these Java idioms for GraalVM Truffle:

### Null Safety
- Use `Objects.requireNonNull()` for constructor parameters
- Prefer `Optional<T>` for methods that may not return a value
- Avoid returning null from execute methods

### Truffle Annotations
| Annotation | Use Case |
|------------|----------|
| `@Child` | Single child node field |
| `@Children` | Array of child nodes |
| `@CompilationFinal` | Field set once, then constant |
| `@Specialization` | Type-specialized execute variants |
| `@NodeChild` | Declare child nodes on class |

```java
// PREFER
@Child private ReLangNode leftNode;
@Child private ReLangNode rightNode;

// AVOID
private ReLangNode leftNode;  // Missing @Child annotation
```

### Execute Method Patterns
```java
// PREFER - clear, focused execute method
@Override
public Object executeGeneric(VirtualFrame frame, ResumableState state) {
    long left = (long) leftNode.executeGeneric(frame, state);
    long right = (long) rightNode.executeGeneric(frame, state);
    return left + right;
}

// AVOID - mixing concerns
@Override
public Object executeGeneric(VirtualFrame frame, ResumableState state) {
    // Don't do logging, validation, and execution all mixed together
}
```

### Resumability Patterns
```java
// PREFER - clean state handling
if (state != null && state.shouldSkipTo(currentIndex)) {
    continue; // Skip already-executed statements on resume
}

// PREFER - proper suspension propagation
if (result instanceof SuspendedResult suspended) {
    return suspended.addFrameState(captureFrameState(frame));
}
```

### Anti-Patterns to Fix
| Anti-Pattern | Idiomatic Alternative |
|--------------|----------------------|
| `if (x == null)` everywhere | `Objects.requireNonNull()` in constructor |
| Long execute methods | Extract helper methods |
| Magic numbers | Named constants |
| Instanceof chains | Use Truffle specialization |
| Catching `Exception` | Catch specific exceptions |
| Public fields | Private fields with accessors if needed |
| Complex conditionals | Extract to well-named methods |

## Constraints

- Preserve all existing functionality
- Maintain backward compatibility for existing ReLang programs
- Follow existing code style guidelines (see CLAUDE.md)
- Ensure all tests pass after refactoring
- Keep AST node contracts (executeGeneric signature) unchanged

## When Refactoring Cannot Proceed:

- No test coverage for critical paths → Suggest adding tests first
- Scope too large (>20 files) → Ask to narrow scope
- Conflicting changes in working directory → Suggest commit or stash
- Unknown scope → List available options and ask for clarification

## Verification Checklist

- [ ] No compilation errors: `./gradlew build`
- [ ] All tests pass: `./gradlew test`
- [ ] ANTLR grammar generates correctly: `./gradlew generateGrammarSource`
- [ ] No new warnings in build output
- [ ] Javadoc updated for changed public APIs
- [ ] Changes follow existing patterns in CLAUDE.md

## Output Format:

1. Start with: "Refactoring [scope]..."
2. Show analysis: "Found X issues: [list them]"
3. For complex changes (>5 files or >100 lines):
    - Create plan in `/docs/tmp/refactor-plan-[timestamp].md`
    - Ask for confirmation before proceeding
4. List changes with impact level:
    - Breaking: [change]
    - Major: [change]
    - Safe: [change]
5. End with: "Refactoring complete. [X] improvements made."

## Examples

### Example 1: Refactor Node

```bash
/refactor ReLangBlockNode
```

**Analysis:**
- `ReLangBlockNode.java` has 200+ lines
- Complex resumability logic mixed with execution
- Opportunities: Extract ResumePathTracker, simplify execute method

### Example 2: Refactor by File

```bash
/refactor @src/main/java/com/relang/parser/ReLangTruffleParser.java
```

**Analysis:**
- Large visitor class with many similar methods
- Duplicate code in expression handling
- Extract common patterns to helper methods

### Example 3: Refactor Recent Changes

```bash
/refactor
```

**Analysis:**
- Git diff shows changes in 3 files
- New code duplicates existing pattern in nodes
- Suggest extracting common base class

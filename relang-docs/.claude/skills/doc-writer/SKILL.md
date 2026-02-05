---
name: doc-writer
description: Write technical documentation for ReLang, a durable execution programming language. Use when creating or editing documentation topics, writing tutorials, how-to guides, reference docs, or explanations about ReLang's awaitable types, coordination operators, checkpoints, or resumability features.
---

# Writing ReLang Documentation

Write world-class technical documentation for ReLang following the Diataxis methodology.

## Quick Reference

| Category | Prefix | Purpose | Tone |
|----------|--------|---------|------|
| Tutorial | `tutorial-` | Teach by doing | Encouraging, step-by-step |
| How-to | `howto-` | Solve specific tasks | Direct, practical |
| Reference | `reference-` | Complete technical details | Precise, structured |
| Explanation | `explanation-` | Build understanding | Conceptual, discursive |

See [DIATAXIS.md](DIATAXIS.md) for detailed guidance on each category.

## Language Specification

The authoritative ReLang spec is at `tmp/relang-awaitables-final.md`. Key concepts:

- **Awaitable types** (`*T`): In-flight effects resolved via `.await()`
- **Coordination**: `and` (all succeed, fail-fast), `or` (first wins, fail-last)
- **Data types**: `&` (product/tuple), `|` (sum/union)
- **Failure model**: `await` returns `T | Failure` with action-family payloads
- **Cancellation**: `cancel(t)`, `shield(t)` for propagation control
- **Timeouts/Retries**: `timeout(t, d)`, `retry(t, policy)`

See [LANGUAGE-CONCEPTS.md](LANGUAGE-CONCEPTS.md) for documentation-ready explanations.

## Workflow

Copy this checklist when writing documentation:

```
Documentation Progress:
- [ ] Step 1: Identify category and create file
- [ ] Step 2: Read relevant spec sections
- [ ] Step 3: Draft content using template
- [ ] Step 4: Verify technical accuracy against spec
- [ ] Step 5: Add to rl.tree
- [ ] Step 6: Cross-link related topics
```

### Step 1: Identify category and create file

Determine which Diataxis category fits:
- **Learning a concept?** → Tutorial
- **Accomplishing a task?** → How-to guide
- **Looking up details?** → Reference
- **Understanding why?** → Explanation

Create file in `topics/` with appropriate prefix:
```bash
touch topics/tutorial-new-topic.md
touch topics/howto-new-task.md
touch topics/reference-new-feature.md
touch topics/explanation-new-concept.md
```

### Step 2: Read relevant spec sections

Before writing, read the specification in `tmp/relang-awaitables-final.md`. Note:
- Exact syntax and semantics
- Edge cases and constraints
- Design rationale (for explanations)

### Step 3: Draft content using template

Use the appropriate template for your category:
- [TEMPLATE-TUTORIAL.md](TEMPLATE-TUTORIAL.md) — Learning-oriented, step-by-step
- [TEMPLATE-HOWTO.md](TEMPLATE-HOWTO.md) — Task-oriented, practical
- [TEMPLATE-REFERENCE.md](TEMPLATE-REFERENCE.md) — Technical details, structured
- [TEMPLATE-EXPLANATION.md](TEMPLATE-EXPLANATION.md) — Conceptual, discursive

### Step 4: Verify technical accuracy

Cross-check all:
- Type signatures match spec
- Behavior descriptions are accurate
- Code examples are valid ReLang syntax
- Terminology is consistent

### Step 5: Add to rl.tree

Edit `rl.tree` to include the new topic under the correct category:

```xml
<toc-element toc-title="How-to Guides">
    <toc-element topic="howto-existing.md"/>
    <toc-element topic="howto-new-task.md"/>  <!-- Add here -->
</toc-element>
```

### Step 6: Cross-link related topics

Add "See Also" section linking to:
- Related tutorials (for how-to guides)
- Detailed reference (for tutorials)
- Conceptual explanation (for reference)

## Writing Standards

### Code Examples

Always use fenced code blocks with `relang` language tag:

````markdown
```relang
let t1: *User = db.query("SELECT * FROM users WHERE id = 1")
let t2: *Orders = db.query("SELECT * FROM orders WHERE user_id = 1")

let joined: *(User & Orders) = t1 and t2
match joined.await() {
  (user & orders) => print("${user.name} has ${orders.count} orders")
  err: Failure => print("Failed: ${err}")
}
```
````

### Terminology

Use consistent terms throughout:

| Correct | Avoid |
|---------|-------|
| awaitable | task, future, promise |
| coordinate | combine, join, merge |
| resolve | complete, finish |
| fail-fast | early termination |
| fail-last | all-or-nothing |

### Voice and Tone

- **Tutorials**: "Let's create...", "You'll learn..."
- **How-to**: "To achieve X, do Y", imperative mood
- **Reference**: Third person, declarative
- **Explanation**: "This works because...", exploratory

## Validation Checklist

Before finalizing any topic:

```
Quality Check:
- [ ] Technical accuracy verified against spec
- [ ] Code examples compile (if tooling exists)
- [ ] Consistent terminology used
- [ ] Appropriate for Diataxis category
- [ ] Cross-links to related topics included
- [ ] Added to rl.tree
```

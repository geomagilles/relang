# Diataxis Documentation Categories

## Tutorials

**Purpose**: Learning-oriented. Teach users by doing.

**Characteristics**:
- Step-by-step instructions the reader follows
- Concrete, working examples
- Narrow scope (one learning outcome)
- Assumes no prior knowledge of ReLang
- Shows expected results at each step

**Structure**:
1. Brief intro stating what they'll learn
2. Prerequisites (installation, setup)
3. Numbered steps with code and explanations
4. Verification of success
5. Summary of what was learned
6. Links to next tutorials or how-to guides

**Writing tips**:
- Use "you" and "let's"
- Show every command and expected output
- Don't explain why (that's for Explanation docs)
- Keep focused; resist tangents
- Test every step yourself

**Example opening**:
> In this tutorial, you'll create your first ReLang program that uses checkpoints to suspend and resume execution. By the end, you'll understand how to write durable workflows.

## How-to Guides

**Purpose**: Task-oriented. Help users accomplish specific goals.

**Characteristics**:
- Assumes basic ReLang knowledge
- Focused on solving a real problem
- Practical, not theoretical
- Multiple valid approaches may exist
- Can assume reader knows why they need this

**Structure**:
1. Brief statement of the goal
2. Prerequisites (if any)
3. Steps to achieve the goal
4. Variations or alternatives (if relevant)
5. Troubleshooting common issues
6. Links to related how-to guides

**Writing tips**:
- Use imperative mood: "Create a file", "Run the command"
- Be direct; don't over-explain
- Include error handling patterns
- Show complete, working code
- Address common variations

**Example opening**:
> This guide shows how to coordinate multiple HTTP requests and handle partial failures gracefully using ReLang's `and` and `or` operators.

## Reference

**Purpose**: Information-oriented. Provide accurate, complete technical details.

**Characteristics**:
- Comprehensive coverage of a feature
- Structured for lookup, not reading
- Precise and consistent
- No tutorials or explanations embedded
- Covers all options, parameters, edge cases

**Structure**:
1. Brief definition
2. Syntax (formal grammar if relevant)
3. Parameters/options table
4. Return values/types
5. Examples (minimal, illustrative)
6. Edge cases and constraints
7. Related reference pages

**Writing tips**:
- Use tables for parameters and options
- Be exhaustive; document everything
- Use consistent format across all reference pages
- Link to explanations for "why" questions
- Include type signatures

**Example format**:
```markdown
## timeout

Wraps an awaitable with a deadline.

### Syntax

```relang
timeout(t, d) : *T
```

### Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `t` | `*T` | The awaitable to wrap |
| `d` | `Duration` | Maximum time to wait |

### Returns

Returns `*T`. On timeout, resolves with `Failure` containing `Timeout` error.
```

## Explanation

**Purpose**: Understanding-oriented. Build mental models.

**Characteristics**:
- Discusses concepts, not procedures
- Explains why things work the way they do
- Can be discursive and exploratory
- Connects ideas to broader context
- May discuss design decisions and trade-offs

**Structure**:
1. Introduction to the concept
2. Core explanation with examples
3. Why it matters / design rationale
4. How it relates to other concepts
5. Common misconceptions addressed
6. Further reading

**Writing tips**:
- Use analogies and diagrams
- Discuss alternatives and why they weren't chosen
- Connect to concepts readers may know
- It's okay to be longer and more narrative
- Reference the specification's design rationale

**Example opening**:
> ReLang's failure model treats failures as data, not exceptions. This design choice enables deterministic replay and allows failures to compose predictably through coordination operators. Understanding why requires examining the constraints of durable execution.

## Category Selection Guide

| User is... | They need... | Category |
|------------|--------------|----------|
| Learning ReLang | Step-by-step lesson | Tutorial |
| Trying to do something specific | Practical steps | How-to |
| Looking up syntax/options | Complete details | Reference |
| Wondering why something works | Conceptual understanding | Explanation |

## Anti-patterns

**Don't mix categories**:
- Tutorial with extensive theory → Split into tutorial + explanation
- Reference with embedded tutorial → Link to separate tutorial
- How-to that explains why → Link to explanation doc

**Don't duplicate**:
- Same information in multiple categories
- Instead, link between docs

**Don't assume wrong context**:
- Tutorial assuming prior knowledge
- Reference that's incomplete
- How-to that over-explains basics

# ReLang Documentation

ReLang is a programming language built on GraalVM's Truffle framework with support for **resumable execution** via checkpoints.

## What is ReLang?

ReLang allows you to:
- Write programs that can **suspend** at any point using `checkpoint;`
- **Serialize** the complete execution state (call stack, local variables)
- **Resume** execution later, even on a different machine

This makes ReLang ideal for:
- Long-running computations that need to survive restarts
- Workflow orchestration with durable execution
- Debugging and time-travel scenarios

## Documentation Structure

This documentation follows the [Diataxis](https://diataxis.fr) methodology:

| Section | Purpose | When to use |
|---------|---------|-------------|
| **Tutorials** | Learning-oriented guides | You're new to ReLang |
| **How-to Guides** | Task-oriented instructions | You need to accomplish something specific |
| **Reference** | Technical specifications | You need precise details |
| **Explanation** | Conceptual understanding | You want to understand how things work |

## Quick Example

```
fn countdown(n) {
    while (n > 0) {
        checkpoint;  // Suspend here
        n = n - 1;
    }
    return n;
}

countdown(1000000);
```

Run with state persistence:
```bash
relang countdown.re --state-out state.json
# Suspends, saves state, exits

relang countdown.re --state-in state.json --state-out state.json
# Resumes from where it left off
```

# How to Suspend and Resume Execution

This guide shows you how to use ReLang's checkpoint feature to suspend program execution and resume it later.

## Overview

ReLang programs can suspend at `checkpoint;` statements. When suspended:
1. The complete execution state is captured (call stack, local variables)
2. The state is saved to a file
3. The program exits with a special exit code
4. You can later resume execution from exactly where it stopped

## Basic Usage

### Step 1: Add checkpoints to your program

Create `counter.re`:

```
x = 0;
while (x < 10) {
    x = x + 1;
    checkpoint;
}
return x;
```

### Step 2: Run with state output

```bash
relang counter.re --state-out state.json
```

The program runs until the first `checkpoint;`, then:
- Saves state to `state.json`
- Exits with code `75` (suspended)

### Step 3: Resume execution

```bash
relang counter.re --state-in state.json --state-out state.json
```

This:
- Loads state from `state.json`
- Resumes execution after the checkpoint
- On next checkpoint, overwrites `state.json` with new state

### Step 4: Run to completion

Repeat step 3 until the program completes (exits with code `0`).

## Checking Exit Codes

```bash
relang counter.re --state-out state.json
echo $?  # 75 = suspended, 0 = completed
```

| Exit Code | Meaning |
|-----------|---------|
| `0` | Program completed normally |
| `75` | Program suspended at checkpoint |
| `1` | Error occurred |

## Scripting Resumption

Run a program to completion with automatic resume:

```bash
#!/bin/bash
STATE_FILE="state.json"

while true; do
    relang program.re --state-in "$STATE_FILE" --state-out "$STATE_FILE"
    EXIT_CODE=$?

    if [ $EXIT_CODE -eq 0 ]; then
        echo "Program completed"
        rm -f "$STATE_FILE"
        break
    elif [ $EXIT_CODE -eq 75 ]; then
        echo "Checkpoint reached, resuming..."
    else
        echo "Error occurred"
        exit $EXIT_CODE
    fi
done
```

## Using Different State Formats

### JSON (default, human-readable)

```bash
relang program.re --state-out state.json --state-format json
```

### Protocol Buffers (compact, efficient)

```bash
relang program.re --state-out state.pb --state-format protobuf
```

## Resuming on a Different Machine

The state file is portable. You can:

1. Suspend on machine A:
   ```bash
   # On machine A
   relang compute.re --state-out state.json
   scp state.json compute.re machine-b:
   ```

2. Resume on machine B:
   ```bash
   # On machine B
   relang compute.re --state-in state.json --state-out state.json
   ```

> **Important**: The source code must be identical. ReLang validates a hash of the source code to prevent resuming with modified code.

## Handling Code Changes

If you modify the source code after suspending, ReLang will refuse to resume:

```
Error: Source code has changed since suspension.
Stored hash: a1b2c3d4e5f6...
Current hash: 9f8e7d6c5b4a...
```

To resume with modified code (dangerous!), you must start fresh without `--state-in`.

## Common Patterns

### Periodic checkpointing in loops

```
fn process_items(items) {
    i = 0;
    while (i < items) {
        // Do work...
        i = i + 1;

        // Checkpoint every 100 iterations
        if (i % 100 == 0) {
            checkpoint;
        }
    }
}
```

### Checkpointing at phase boundaries

```
fn pipeline(data) {
    result = phase1(data);
    checkpoint;  // Save after expensive phase 1

    result = phase2(result);
    checkpoint;  // Save after phase 2

    return phase3(result);
}
```

## See Also

- [CLI Reference](reference-cli.md) - Complete command-line options
- [State File Format](reference-state-format.md) - Technical details of state files
- [How Resumability Works](explanation-resumability.md) - Understanding the internals

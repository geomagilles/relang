# State File Format

Technical reference for ReLang execution state files.

## Overview

When a ReLang program suspends at a `checkpoint;` statement, its complete execution state is serialized to a file. This state includes:

- **Source hash**: SHA-256 hash of the source code (for validation)
- **Frame stack**: The call stack at suspension time
- **Local variables**: All variable values in each frame
- **Execution path**: The position within each block/function

## Supported Formats

| Format | Extension | Use Case |
|--------|-----------|----------|
| JSON | `.json` | Human-readable, debugging, small states |
| Protocol Buffers | `.pb` | Compact, efficient, production storage |

## JSON Format

### Structure

```json
{
  "sourceHash": "<sha256-hex-string>",
  "frames": [
    {
      "locals": {
        "<variable-name>": <value>,
        ...
      },
      "executionPath": [<index>, ...]
    },
    ...
  ]
}
```

### Fields

#### Root Object

| Field | Type | Description |
|-------|------|-------------|
| `sourceHash` | string | SHA-256 hash of source code (64 hex characters) |
| `frames` | array | Stack of frame states, bottom to top |

#### Frame Object

| Field | Type | Description |
|-------|------|-------------|
| `locals` | object | Map of variable names to values |
| `executionPath` | array | List of block indices representing position |

#### Value Types

| ReLang Type | JSON Type | Example |
|-------------|-----------|---------|
| `long` | number | `42` |
| `boolean` | boolean | `true` |

### Example

For this program:

```
fn outer(n) {
    x = n * 2;
    return inner(x);
}

fn inner(y) {
    z = y + 1;
    checkpoint;
    return z;
}

outer(10);
```

The state file when suspended in `inner`:

```json
{
  "sourceHash": "a1b2c3d4e5f67890...",
  "frames": [
    {
      "locals": {
        "n": 10,
        "x": 20
      },
      "executionPath": [1]
    },
    {
      "locals": {
        "y": 20,
        "z": 21
      },
      "executionPath": [1]
    }
  ]
}
```

Frame order: `frames[0]` is the outermost caller (`outer`), `frames[1]` is the innermost (`inner`).

## Protocol Buffers Format

### Schema

```protobuf
syntax = "proto3";

message ResumableStateProto {
  string source_hash = 1;
  repeated FrameStateProto frames = 2;
}

message FrameStateProto {
  map<string, LocalValue> locals = 1;
  repeated int32 execution_path = 2;
}

message LocalValue {
  oneof value {
    int64 long_value = 1;
    bool bool_value = 2;
  }
}
```

### Usage

Protocol Buffers format is more compact and faster to parse:

| State Size | JSON | Protobuf | Savings |
|------------|------|----------|---------|
| 10 frames, 5 vars each | ~2 KB | ~500 B | 75% |
| 100 frames, 10 vars each | ~25 KB | ~5 KB | 80% |

Use protobuf for:
- Production systems with frequent checkpoints
- Network transmission of state
- Large call stacks

## Source Hash Validation

The `sourceHash` field prevents resuming with modified code:

1. **On suspend**: Hash of source code is computed and stored
2. **On resume**: Hash of current source is compared to stored hash
3. **On mismatch**: Error is raised, resume is rejected

Hash algorithm: SHA-256

```
sourceHash = SHA256(source_code_bytes_utf8)
```

### Bypassing Validation

There is no CLI option to bypass validation. If you need to resume with modified code:

1. Manually edit the state file
2. Update `sourceHash` to match new code's hash
3. Ensure execution path is still valid (risky!)

## Execution Path

The `executionPath` array tracks the program counter within nested blocks.

For this code:

```
fn example() {
    x = 1;           // index 0
    if (x < 10) {    // index 1
        y = 2;       //   block index 0
        checkpoint;  //   block index 1
        z = 3;       //   block index 2
    }
    return x;        // index 2
}
```

If suspended at `checkpoint;`, the path would be `[1, 1]`:
- `1`: We're in the if-block (statement index 1 in function)
- `1`: We're at statement index 1 within that block

On resume, execution skips to index 1 in the if-block and continues.

## Compatibility

### Forward Compatibility

State files from older ReLang versions may work with newer versions if:
- The source code is unchanged
- No breaking changes to state format

### Backward Compatibility

State files from newer ReLang versions may not work with older versions.

### Version Field

Currently no explicit version field. The `sourceHash` provides implicit versioning by rejecting mismatched code.

## See Also

- [How to Suspend and Resume](howto-suspend-resume.md)
- [How Resumability Works](explanation-resumability.md)
- [CLI Reference](reference-cli.md)

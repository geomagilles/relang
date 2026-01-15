# CLI Reference

Complete reference for the `relang` command-line interface.

## Synopsis

```
relang [OPTIONS] [FILE]
```

## Description

The `relang` command executes ReLang programs. When invoked without a file, it starts an interactive REPL.

## Arguments

| Argument | Description |
|----------|-------------|
| `FILE` | Path to a `.re` file to execute. If omitted, starts the REPL. |

## Options

### General Options

| Option | Description |
|--------|-------------|
| `--help`, `-h` | Display help message and exit |

### State Management Options

| Option | Description |
|--------|-------------|
| `--state-in <FILE>` | Load execution state from FILE before running. Used to resume from a previous checkpoint. |
| `--state-out <FILE>` | Save execution state to FILE when a checkpoint is reached. |
| `--state-format <FORMAT>` | State file format: `json` (default) or `protobuf` |

### Debugging Options

| Option | Description |
|--------|-------------|
| `--inspect` | Enable Chrome DevTools debugger |
| `--inspect.port <PORT>` | Debugger port (default: `4711`) |

### LSP Options

| Option | Description |
|--------|-------------|
| `--lsp` | Start Language Server Protocol server for IDE integration |
| `--lsp.port <PORT>` | LSP server port (default: `8123`) |

## Exit Codes

| Code | Name | Description |
|------|------|-------------|
| `0` | `SUCCESS` | Program completed normally |
| `1` | `ERROR` | An error occurred during execution |
| `75` | `SUSPENDED` | Program suspended at a checkpoint. State saved to `--state-out` file. |

## Examples

### Run a program

```bash
relang program.re
```

### Start the REPL

```bash
relang
```

### Run with checkpointing

```bash
# First run - may suspend
relang program.re --state-out state.json

# Resume from checkpoint
relang program.re --state-in state.json --state-out state.json
```

### Run with debugger

```bash
relang --inspect program.re
# Connect Chrome to chrome://inspect
```

### Use Protocol Buffers for state

```bash
relang program.re --state-out state.pb --state-format protobuf
relang program.re --state-in state.pb --state-out state.pb --state-format protobuf
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `JAVA_HOME` | Path to Java installation (must be Java 21+) |
| `GRAALVM_HOME` | Path to GraalVM installation (recommended) |

## Files

| File | Description |
|------|-------------|
| `*.re` | ReLang source files |
| `state.json` | JSON-formatted execution state |
| `state.pb` | Protocol Buffer-formatted execution state |

## See Also

- [How to Run Programs](howto-run-programs.md)
- [How to Suspend and Resume](howto-suspend-resume.md)
- [State File Format](reference-state-format.md)

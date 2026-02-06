# ReLang LSP

Standalone Language Server Protocol entry point for ReLang.

## Overview

This module starts the GraalVM-based LSP server independently from the interpreter CLI.  
It is intended to be the single LSP process used by IDE integrations.

## Run

```bash
# Default bind: 127.0.0.1:8123
./gradlew :relang-lsp:run

# Custom port
./gradlew :relang-lsp:run --args="--port 8124"

# Custom host and port
./gradlew :relang-lsp:run --args="--host 127.0.0.1 --port 8124"
```

## Build

```bash
./gradlew :relang-lsp:build
```

## Notes

- Requires GraalVM 25+.
- Uses experimental GraalVM LSP options under the hood.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What Is This Project

This is the **Writerside documentation** for ReLang, a programming language designed for **durable execution** with first-class support for checkpoints, awaitables, and resumable workflows. The documentation follows the [Diataxis](https://diataxis.fr/) methodology.

## ReLang Language Design

The current language specification is in `tmp/relang-awaitables-final.md`. Key concepts:

- **Awaitable types** (`*T`): In-flight effects resolved via `.await()`
- **Coordination operators**: `and` (all must succeed, fail-fast), `or` (first success wins)
- **Data operators**: `&` (product/tuple), `|` (sum/union)
- **Failure model**: `await` returns `T | Failure` with action-family-specific error payloads
- **Cancellation**: Request-based, observed only at resolution, with `shield()` for propagation control
- **Timeouts/Retries**: Composable policies via `timeout(t, d)` and `retry(t, policy)`

## Build Commands

```bash
# Build docs via Gradle (requires Writerside plugin)
./gradlew :relang-docs:build

# Docker build
docker run --rm -v $(pwd):/docs jetbrains/writerside-builder:latest
```

Or open in JetBrains Writerside IDE and click "Build".

## Documentation Structure

```
relang-docs/
├── writerside.cfg      # Root configuration (topics dir, images dir, instance)
├── rl.tree             # Table of contents (XML)
├── v.list              # Variables (product name, version)
└── topics/             # Markdown documentation files
```

### Diataxis Categories

| Prefix | Category | Purpose |
|--------|----------|---------|
| `tutorial-*` | Tutorials | Learning-oriented, step-by-step |
| `howto-*` | How-to Guides | Task-oriented, practical recipes |
| `reference-*` | Reference | Technical details, complete specification |
| `explanation-*` | Explanation | Conceptual understanding, "why" |

### Adding New Topics

1. Create file in `topics/` with appropriate prefix
2. Add to `rl.tree` under the matching category section
3. Use standard Markdown with Writerside extensions

## Configuration Files

- **writerside.cfg**: Declares `topics/` and `images/` directories, references `rl.tree`
- **rl.tree**: XML table of contents with `<toc-element>` entries per topic
- **v.list**: Variables like `%product%` (ReLang) and `%version%` (1.0)

## Working Files

Use `tmp/` for all temporary and in-progress files:
- Draft documentation before finalization
- Planning documents and discussion results
- Specification drafts and design notes

Only move content to `topics/` when it's ready for publication.

## Writing Guidelines

- Reference the spec in `tmp/relang-awaitables-final.md` for accurate language semantics
- Use standard Markdown; Writerside does not support HTML comments
- Link related topics in "See Also" sections
- Keep reference docs structured with tables and lists
- Keep explanation docs conceptual and discursive

# Docs Command

Reorganize for Human + AI Consumption the documentation relative to: **$ARGUMENTS**

Examples of placeholder text:

- "resumability system and checkpoint execution"
- "AST nodes and Truffle framework integration"
- "parser pipeline and ANTLR grammar"
- "the entire docs/ directory"

## Objectives

1. **AI Token Efficiency**: No document should exceed 200 lines (target 100-150)
2. **Single Responsibility**: Each file covers ONE focused topic
3. **Self-Contained Guides**: Minimal cross-references, readable standalone
4. **Clear Separation**: Concepts (why) vs Implementation (how)
5. **Strong Index**: README.md for navigation

## Documentation Structure

ReLang uses a flat documentation structure:

```
docs/
├── README.md                    # Index + quick reference
├── architecture.md              # High-level system design
├── nodes.md                     # AST node hierarchy
├── parser.md                    # ANTLR grammar and parsing
├── resumability.md              # Checkpoint/resume system
├── types.md                     # Type system (long, boolean)
└── extending.md                 # Adding new language features
```

**Key principles**:

- **Flat structure**: No nested folders within docs
- **Naming convention**: lowercase with hyphens
- Each doc has a **clear single purpose**
- **README.md** serves as the index with quick reference tables

## Document Guidelines

### README.md (Index)

```markdown
# ReLang Documentation

ReLang is a custom programming language built on GraalVM's Truffle framework with support for resumable execution via checkpoints.

## Documentation Index

| Document | Description | When to Read |
|----------|-------------|--------------|
| [architecture.md](architecture.md) | System overview | Understanding the big picture |
| [nodes.md](nodes.md) | AST node hierarchy | Implementing new nodes |
| [resumability.md](resumability.md) | Checkpoint system | Understanding suspend/resume |

## Quick Reference

### Key Classes

| Class | Location | Purpose |
|-------|----------|---------|
| `ReLang` | `com.relang/` | Language entry point |
| `ReLangContext` | `com.relang/` | Per-context state |
| `ReLangRootNode` | `com.relang/` | Function entry point |
| `ResumableState` | `com.relang.nodes/` | Checkpoint state container |

### Common Tasks

| Task | Documentation |
|------|---------------|
| Add new operator | [nodes.md](nodes.md#operators) |
| Add checkpoint support | [resumability.md](resumability.md) |

## Commands

```bash
./gradlew build
./gradlew test
./gradlew generateGrammarSource
```
```

**Target**: 50-80 lines, **max 100 lines**

### Topic Document

```markdown
# ReLang - {Topic}

This document covers {topic} for the ReLang language.

## Overview

[Brief overview paragraph]

## Key Files

| File | Purpose |
|------|---------|
| `path/File.java` | Brief purpose |

---

## Main Content

[Organized with ## headers for major sections]

---

## Best Practices / Common Patterns

[If applicable]

---

## Troubleshooting

[If applicable - common issues table]

| Issue | Check |
|-------|-------|
| Problem | Solution |
```

**Target**: 80-150 lines, **max 200 lines**

## Specific Instructions

### Step 1: Audit

```bash
# List all docs and line counts
find docs -name "*.md" -exec wc -l {} \; | sort -n

# Find files exceeding 200 lines
find docs -name "*.md" -exec wc -l {} \; | awk '$1 > 200 {print}'

# Check README exists
test -f docs/README.md && echo "README exists" || echo "Missing README"
```

### Step 2: Create New Document

```bash
# Create docs folder if needed
mkdir -p docs

# Create new topic document
touch docs/{topic}.md
```

### Step 3: Move Content

**For splits (1 file -> many files)**:

- Create new files with focused content
- Add comment at top: `<!-- Split from: {original-file}.md -->`
- Update README.md index

### Step 4: Update README.md Index

After adding/moving docs:

- Add entry to Documentation Index table
- Add relevant entries to Quick Reference
- Add to Common Tasks if applicable

### Step 5: Validate

```bash
# No doc exceeds 200 lines
find docs -name "*.md" -exec wc -l {} \; | awk '$1 > 200 {print $2 ": " $1 " lines (EXCEEDS MAX)"}'

# Verify README.md exists
test -f docs/README.md || echo "Missing README.md"
```

## Line Count Limits

| File Type | Target | Max | If Exceeds Max |
|-----------|--------|-----|----------------|
| README.md (index) | 50-80 | 100 | Reduce quick start, link more |
| {topic}.md | 80-150 | 200 | Split into multiple focused docs |

**Philosophy**: If a file exceeds max, split into multiple focused documents.

## When to Use This Command

**Use when**:

- Any single file exceeds 150 lines
- Adding documentation for a new feature
- Content doesn't fit existing documents
- Reorganizing after significant changes

**Don't use when**:

- Docs are already well-organized
- File is <150 lines and focused
- Minor updates to existing docs

---
name: qmd
description: Search ReLang language specs, proposals, documentation, and design docs using QMD - a local hybrid search engine. Combines BM25 keyword search, vector semantic search, and LLM re-ranking. Use when users ask to search specs, find proposals, look up language design decisions, retrieve documentation, or search across ReLang knowledge. Triggers on "search specs", "search docs", "find in specs", "look up", "what does the spec say about", "find the proposal for", "how is X specified".
license: MIT
compatibility: Requires qmd CLI or MCP server. Install via `bun install -g https://github.com/tobi/qmd`.
metadata:
author: tobi
version: "1.1.1"
allowed-tools: Bash(qmd:*), mcp__qmd__*
---

# QMD - ReLang Knowledge Search

QMD is a local, on-device search engine for markdown content. It indexes the ReLang specs, proposals, documentation, governance docs, and sprint plans for fast retrieval.

## QMD Status

!`qmd status 2>/dev/null || echo "Not installed. Run: bun install -g https://github.com/tobi/qmd"`

## When to Use This Skill

- User asks what the spec says about a language feature (types, actions, awaitables, etc.)
- User needs to find a specific proposal or design decision
- User wants to look up how a feature is specified before implementing it
- User asks "how is X specified" or "find the proposal for Y"
- User needs to cross-reference specs, governance, or sprint plans
- User needs semantic search across ReLang documentation (conceptual similarity)

## Search Commands

Choose the right search mode for the task:

| Command | Use When | Speed |
|---------|----------|-------|
| `qmd search` | Exact keyword matches needed | Fast |
| `qmd vsearch` | Keywords aren't working, need conceptual matches | Medium |
| `qmd query` | Best results needed, speed not critical | Slower |

```bash
# Fast keyword search (BM25)
qmd search "your query"

# Semantic vector search (finds conceptually similar content)
qmd vsearch "your query"

# Hybrid search with re-ranking (best quality)
qmd query "your query"
```

## Common Options

```bash
-n <num>                 # Number of results (default: 5)
-c, --collection <name>  # Restrict to specific collection
--all                    # Return all matches
--min-score <num>        # Minimum score threshold (0.0-1.0)
--full                   # Show full document content
--json                   # JSON output for processing
--files                  # List files with scores
--line-numbers           # Add line numbers to output
```

## Document Retrieval

```bash
# Get document by path
qmd get "collection/path/to/doc.md"

# Get document by docid (shown in search results as #abc123)
qmd get "#abc123"

# Get with line numbers for code review
qmd get "docs/api.md" --line-numbers

# Get multiple documents by glob pattern
qmd multi-get "docs/*.md"

# Get multiple documents by list
qmd multi-get "doc1.md, doc2.md, #abc123"
```

## Index Management

```bash
# Check index status and available collections
qmd status

# List all collections
qmd collection list

# List files in a collection
qmd ls <collection-name>

# Update index (re-scan files for changes)
qmd update
```

## Score Interpretation

| Score | Meaning | Action |
|-------|---------|--------|
| 0.8 - 1.0 | Highly relevant | Show to user |
| 0.5 - 0.8 | Moderately relevant | Include if few results |
| 0.2 - 0.5 | Somewhat relevant | Only if user wants more |
| 0.0 - 0.2 | Low relevance | Usually skip |

## Recommended Workflow

1. **Check what's available**: `qmd status`
2. **Start with keyword search**: `qmd search "topic" -n 10`
3. **Try semantic if needed**: `qmd vsearch "describe the concept"`
4. **Use hybrid for best results**: `qmd query "question" --min-score 0.4`
5. **Retrieve full documents**: `qmd get "#docid" --full`

## Example: Finding a Language Feature Spec

```bash
# Search for how awaitables are specified
qmd search "awaitable checkpoint resume" -n 5

# Get semantic matches for a concept
qmd vsearch "how does error handling work in relang"

# Retrieve the full proposal
qmd get "#abc123" --full
```

## Example: Cross-Referencing Specs and Plans

```bash
# Find all mentions of a feature across specs and plans
qmd query "actions HTTP binding" --min-score 0.3 --json

# Get all relevant governance and sprint docs
qmd query "conformance testing" --all --files --min-score 0.4
```

## Example: Looking Up Design Decisions

```bash
# Search for rationale behind a language choice
qmd vsearch "why optional types instead of null"

# Find sprint plan for a feature area
qmd search "sprint types primitives"
```

## MCP Server Integration

This plugin configures the qmd MCP server automatically. When available, prefer MCP tools over Bash for tighter integration:

| MCP Tool | Equivalent CLI | Purpose |
|----------|---------------|---------|
| `qmd_search` | `qmd search` | Fast BM25 keyword search |
| `qmd_vsearch` | `qmd vsearch` | Semantic vector search |
| `qmd_query` | `qmd query` | Hybrid search with reranking |
| `qmd_get` | `qmd get` | Retrieve document by path or docid |
| `qmd_multi_get` | `qmd multi-get` | Retrieve multiple documents |
| `qmd_status` | `qmd status` | Index health and collection info |

For manual MCP setup without the plugin, see [references/mcp-setup.md](references/mcp-setup.md).
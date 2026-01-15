---
description: Generate user-oriented release notes for a new version
---

## Usage

```
/release <version> [--from <tag>] [--draft]
```

## Arguments

- `<version>` - The version number (e.g., `0.5.3`, `1.0.0`)

## Options

- `--from <tag>` - Previous version tag to compare from (default: latest tag)
- `--draft` - Generate a draft template without fetching git history

## Examples

```bash
# Generate release notes for v0.5.3
/release 0.5.3

# Specify the previous version explicitly
/release 1.0.0 --from v0.9.5

# Generate a draft template to fill in manually
/release 0.6.0 --draft
```

## Instructions

When this command is invoked:

1. **Gather git history** to understand what changed
2. **Categorize changes** by type (features, fixes, improvements)
3. **Transform to user-oriented language** focusing on benefits
4. **Generate release notes** following the structure below

---

## Workflow

### Step 1: Gather Changes

```bash
# Get the previous tag if not specified
git describe --tags --abbrev=0

# List commits since last tag
git log <previous-tag>..HEAD --oneline --no-merges

# Get detailed commit messages
git log <previous-tag>..HEAD --pretty=format:"### %s%n%b%n---" --no-merges
```

### Step 2: Categorize by Type

| Commit Pattern | Section |
|----------------|---------|
| `Add`, `Implement` | New Features |
| `Fix` | Bug Fixes |
| `Improve`, `Refactor` | Improvements |
| `BREAKING` | Breaking Changes |
| `docs`, `test` | Evaluate individually |

### Step 3: Transform to User-Oriented Language

Focus on:

- **What changed?** - The feature or fix
- **Why does it matter?** - The benefit to users
- **How do I use it?** - Example code if applicable

**Before (developer):** `Add Protocol Buffers serialization for ResumableState`

**After (user):** `Checkpoint state can now be serialized to Protocol Buffers format for efficient storage and transmission`

### Step 4: Generate Release Notes

```markdown
# ReLang {version} Release Notes

## Summary
<!-- For MINOR/MAJOR: 2-4 sentences on release theme -->

## New Features

### {Feature Name}
{Description}

#### Example
```relang
// Example code using the new feature
```

## Bug Fixes

- **{Area}**: {What was fixed} - {User impact}

## Improvements

- **{Area}**: {What improved} - {Measurable benefit}

## Breaking Changes

None.
<!-- Or list breaking changes with migration steps -->

## Full Changelog

Compare: https://github.com/{org}/relang/compare/{prev-tag}...v{version}
```

### Step 5: Verify Against Checklist

Confirm:

- [ ] All user-facing changes documented
- [ ] Code examples tested and working
- [ ] Breaking changes section present (even if "None")
- [ ] Links are valid
- [ ] Consistent formatting throughout
- [ ] No internal jargon without explanation

---

## ReLang-Specific Categories

### Language Features
- New syntax additions (operators, statements, expressions)
- Type system changes
- Built-in function additions

### Resumability
- Checkpoint/resume improvements
- State serialization changes
- Frame state handling

### Performance
- Execution speed improvements
- Memory usage optimizations
- Truffle compilation improvements

### Tooling
- Grammar changes
- Parser improvements
- Test infrastructure

---

## Output

Save the generated release notes to `/tmp/RELEASE-{version}.md` and present to the user.

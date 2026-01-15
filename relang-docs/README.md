# ReLang Documentation

Technical documentation for ReLang, built with JetBrains Writerside.

## Overview

This module contains the official ReLang documentation, organized following the [Diataxis](https://diataxis.fr/) methodology:

| Category | Purpose | Example Topics |
|----------|---------|----------------|
| **Tutorials** | Learning-oriented, step-by-step lessons | Getting started guide |
| **How-to Guides** | Task-oriented, practical recipes | Suspend and resume, run programs |
| **Reference** | Information-oriented, technical details | CLI options, language syntax, state format |
| **Explanation** | Understanding-oriented, conceptual | How resumability works, Truffle architecture |

## Documentation Structure

```
relang-docs/
├── writerside.cfg          # Writerside configuration
├── v.list                  # Version/variable definitions
├── rl.tree                 # Table of contents
└── topics/
    ├── overview.md
    ├── tutorial-getting-started.md
    ├── howto-suspend-resume.md
    ├── howto-run-programs.md
    ├── reference-cli.md
    ├── reference-state-format.md
    ├── reference-language-syntax.md
    ├── reference-native-builds.md
    ├── explanation-resumability.md
    └── explanation-truffle.md
```

## Building the Documentation

### Prerequisites

- JetBrains Writerside (IDE or plugin for IntelliJ)
- Or: Docker for command-line builds

### Using Writerside IDE

1. Open the `relang-docs` folder in Writerside
2. Click "Build" to generate HTML output
3. Output is in `build/`

### Using Gradle

```bash
# Build docs (requires Writerside Gradle plugin)
./gradlew :relang-docs:build
```

### Using Docker

```bash
cd relang-docs
docker run --rm -v $(pwd):/docs jetbrains/writerside-builder:latest
```

## Topics

### Tutorials

| Topic | Description |
|-------|-------------|
| [Getting Started](topics/tutorial-getting-started.md) | First steps with ReLang |

### How-to Guides

| Topic | Description |
|-------|-------------|
| [Suspend and Resume](topics/howto-suspend-resume.md) | Using checkpoints for resumable execution |
| [Run Programs](topics/howto-run-programs.md) | Different ways to run ReLang programs |

### Reference

| Topic | Description |
|-------|-------------|
| [CLI Reference](topics/reference-cli.md) | Command-line options and exit codes |
| [State Format](topics/reference-state-format.md) | JSON/Protobuf state file specification |
| [Language Syntax](topics/reference-language-syntax.md) | Complete language grammar and syntax |
| [Native Builds](topics/reference-native-builds.md) | Building native interpreter and compiler |

### Explanation

| Topic | Description |
|-------|-------------|
| [How Resumability Works](topics/explanation-resumability.md) | Internal mechanics of checkpoint/resume |
| [Truffle Architecture](topics/explanation-truffle.md) | How ReLang is built on GraalVM Truffle |

## Adding New Topics

### 1. Create the Topic File

Create a new Markdown file in `topics/` with the appropriate prefix:

```bash
# For a tutorial
touch topics/tutorial-new-feature.md

# For a how-to guide
touch topics/howto-do-something.md

# For reference
touch topics/reference-something.md

# For explanation
touch topics/explanation-concept.md
```

### 2. Add Content

Use standard Markdown with Writerside extensions:

```markdown
# Topic Title

Introduction paragraph.

## Section

Content here.

### Subsection

More content.

## See Also

- [Related Topic](related-topic.md)
```

### 3. Add to Tree

Edit `rl.tree` to include the new topic:

```xml
<toc-element toc-title="How-to Guides">
    <toc-element topic="howto-suspend-resume.md"/>
    <toc-element topic="howto-new-topic.md"/>  <!-- Add here -->
</toc-element>
```

## Writerside Configuration

### writerside.cfg

Main configuration file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE ihp SYSTEM "https://resources.jetbrains.com/writerside/1.0/ihp.dtd">
<ihp version="2.0">
    <topics dir="topics"/>
    <images dir="images"/>
    <instance src="rl.tree"/>
</ihp>
```

### rl.tree

Table of contents structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE instance-profile SYSTEM "...">
<instance-profile id="rl" name="ReLang Documentation" start-page="overview.md">
    <toc-element topic="overview.md"/>
    <toc-element toc-title="Tutorials">
        <toc-element topic="tutorial-getting-started.md"/>
    </toc-element>
    <!-- ... -->
</instance-profile>
```

### v.list

Version and variable definitions:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE vars SYSTEM "...">
<vars>
    <var name="product" value="ReLang"/>
</vars>
```

## Diataxis Guidelines

### Tutorials

- Focus on learning by doing
- Provide step-by-step instructions
- Keep a narrow scope
- Assume no prior knowledge
- Show expected results

### How-to Guides

- Focus on achieving a goal
- Provide practical steps
- Can assume basic knowledge
- Be direct and concise

### Reference

- Focus on accuracy and completeness
- Be structured and consistent
- Use tables and lists
- Avoid explanations (link to Explanation docs)

### Explanation

- Focus on understanding
- Discuss concepts and background
- Can be more discursive
- Connect ideas together

## Resources

- [Writerside Documentation](https://www.jetbrains.com/help/writerside/)
- [Diataxis Framework](https://diataxis.fr/)
- [Markdown in Writerside](https://www.jetbrains.com/help/writerside/markdown-syntax.html)

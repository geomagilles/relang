# ReLang Documentation

Technical documentation for ReLang, built with JetBrains Writerside.

## Normative Source

The documentation in this module is aligned with:

- `/Users/gilles/dev/relang/specs/v0.1/relang-spec-v0.1-canonique.md`
- `/Users/gilles/dev/relang/specs/v0.1/relang-failures-proposal.md`

When examples or wording drift, `specs/v0.1` is the source of truth.

## Overview

The docs follow [Diataxis](https://diataxis.fr/):

- Tutorials: step-by-step learning
- How-to Guides: practical task completion
- Reference: precise contracts and syntax
- Explanation: conceptual architecture

## Documentation Structure

```text
relang-docs/
├── writerside.cfg
├── v.list
├── rl.tree
└── topics/
    ├── overview.md
    ├── tutorial-getting-started.md
    ├── tutorial-awaitables.md
    ├── tutorial-coordination.md
    ├── tutorial-failures.md
    ├── tutorial-execution-control.md
    ├── howto-suspend-resume.md
    ├── howto-run-programs.md
    ├── reference-cli.md
    ├── reference-language-syntax.md
    ├── reference-state-format.md
    ├── reference-native-builds.md
    ├── explanation-resumability.md
    └── explanation-truffle.md
```

## Building Documentation

From repository root:

```bash
./gradlew :relang-docs:build
```

Or with Writerside tooling in IDE.

## Notes on Runtime-Specific Details

Some operational details are intentionally runtime-defined (for example CLI flag naming or snapshot wire format). Reference pages in this module describe the semantic contract and point to module READMEs for concrete launcher behavior.

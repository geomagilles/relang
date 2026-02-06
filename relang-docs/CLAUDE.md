# CLAUDE.md

Guidance for agents working in `/Users/gilles/dev/relang/relang-docs`.

## Language

Always write in English.

## Source of Truth

Use `/Users/gilles/dev/relang/specs/v0.1` as canonical reference, especially:

- `relang-spec-v0.1-canonique.md`
- `relang-failures-proposal.md`

Do not treat historical drafts in `old/` as normative.

## v0.1 Key Rules

- `await` is explicit: `await e` or `await e!`
- No `checkpoint;` keyword in canonical surface syntax
- No language-level `retry` construct in core v0.1
- Timeout pattern uses coordination (`task or timer(d)`)
- `Failure` is the single failure contract

## Documentation Expectations

- Keep docs aligned with `specs/v0.1`
- Mark runtime-specific behavior as implementation-defined
- Preserve Diataxis structure in `topics/`
- Update `rl.tree` when adding/removing topics

## Build Command

```bash
./gradlew :relang-docs:build
```

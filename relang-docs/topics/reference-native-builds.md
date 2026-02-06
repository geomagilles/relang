# Native Builds Reference

This page covers native build modules in this repository and their relation to the v0.1 spec.

## Modules

- `/Users/gilles/dev/relang/relang-native`: native interpreter packaging
- `/Users/gilles/dev/relang/relang-compiler`: compiler (`relangc`) for standalone native executables
- `/Users/gilles/dev/relang/relang-core`: language/runtime implementation on Truffle

## Semantic Contract

Native binaries must preserve v0.1 language/runtime semantics from `specs/v0.1`, including:

- explicit await model (`await`)
- awaitable coordination (`and` / `or`)
- canonical failure propagation model
- durable snapshot/resume semantics

## Build Commands (Repository)

From `/Users/gilles/dev/relang`:

```bash
./gradlew :relang-native:build
./gradlew :relang-compiler:build
```

For module-specific packaging/running instructions, consult each module README:

- `/Users/gilles/dev/relang/relang-native/README.md`
- `/Users/gilles/dev/relang/relang-compiler/README.md`

## Runtime Surface Variability

Native launchers can differ in:

- binary name and invocation shape
- operational flags
- logging and diagnostics options

But they must remain semantically aligned with `specs/v0.1`.

## Validation Checklist

When validating native outputs:

1. confirm `await` returns `T | Failure`
2. confirm `spawn` boundary wraps child failures as `FunctionFailed`
3. confirm `or` all-failed behavior produces `AllFailed` with ordered causes
4. confirm resume restores language-visible state across effect boundaries

## See Also

- [CLI Reference](reference-cli.md)
- [State Format Reference](reference-state-format.md)

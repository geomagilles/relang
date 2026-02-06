# How to Run ReLang Programs

This guide describes how to run ReLang programs in this repository while staying aligned with `specs/v0.1`.

## 1. Run from Sources (Development)

From `/Users/gilles/dev/relang`:

```bash
./gradlew :relang-core:run --args="path/to/program.re"
```

If your launcher differs, use the module README in `/Users/gilles/dev/relang/relang-core/README.md`.

## 2. Structure Programs as Declarations

Per v0.1 modules:

- one file is one module
- top-level contains declarations (`type`, `sealed`, `fn`, `import`)
- no executable top-level statements

Use an explicit entry function (for example `main`).

## 3. Write Effectful Code with Explicit Await

```relang
fn main(id: String): User | Failure {
  let result = await http.get("https://api.example.com/users/" + id)
  match result {
    u: User -> u
    f: Failure -> f
  }
}
```

## 4. Understand What Is Runtime-Defined

The language spec defines semantics, not a mandatory operational CLI contract for:

- debug ports
- state file flags
- wire format of persisted snapshots

Treat these as runtime/implementation concerns unless explicitly documented by the active launcher.

## See Also

- [CLI Reference](reference-cli.md)
- [Language Syntax Reference](reference-language-syntax.md)

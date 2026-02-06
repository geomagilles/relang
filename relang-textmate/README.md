# ReLang TextMate Grammar

Shared TextMate grammar for ReLang syntax highlighting, used by both VS Code and IntelliJ extensions.

## Overview

This module provides:

- **TextMate Grammar** (`relang.tmLanguage.json`) - Defines syntax highlighting rules
- **Language Configuration** (`language-configuration.json`) - Defines bracket matching, comments, etc.

## Files

```
relang-textmate/
├── syntaxes/
│   └── relang.tmLanguage.json    # TextMate grammar
├── language-configuration.json    # Editor behavior
└── README.md
```

## Grammar Scope

The grammar uses the scope `source.relang` and defines highlighting for:

### Keywords

| Token | Scope |
|-------|-------|
| `if`, `else`, `while`, `return`, `for`, `in`, `match`, `await`, `break`, `continue` | `keyword.control.relang` |
| `fn` | `keyword.declaration.function.relang` |
| `let` | `keyword.declaration.relang` |
| `type`, `sealed` | `keyword.declaration.type.relang` |
| `checkpoint` | `keyword.other.checkpoint.relang` |

### Type Names

| Token | Scope |
|-------|-------|
| `Int`, `Float`, `Bool`, `String`, `Unit`, `None`, `Bytes`, `Duration`, `Timestamp`, `Json`, `Failure` | `support.type.relang` |

### Literals

| Token | Scope |
|-------|-------|
| Integers (`42`, `1_000`) | `constant.numeric.integer.relang` |
| Floats (`3.14`, `1.0e10`) | `constant.numeric.float.relang` |
| Durations (`5s`, `100ms`, `2h`, `10min`) | `constant.numeric.duration.relang` |
| Strings (`"hello"`) | `string.quoted.double.relang` |
| Bytes (`b"\x00\xff"`) | `string.quoted.other.bytes.relang` |
| String interpolation (`${expr}`) | `meta.interpolation.relang` |
| Escape sequences (`\n`, `\t`, `\"`, `\$`) | `constant.character.escape.relang` |
| `true`, `false` | `constant.language.boolean.relang` |
| `none`, `unit` | `constant.language.relang` |

### Type Declarations

| Pattern | Scope |
|---------|-------|
| `type Name` | type name → `entity.name.type.relang` |
| `sealed Name` | type name → `entity.name.type.relang` |

### Identifiers

| Context | Scope |
|---------|-------|
| Function definition (`fn foo(`) | `entity.name.function.relang` |
| Function call (`foo(`) | `entity.name.function.call.relang` |
| Variable | `variable.other.relang` |

### Operators

| Token | Scope |
|-------|-------|
| `+`, `-`, `*`, `/`, `%` | `keyword.operator.arithmetic.relang` |
| `==`, `!=`, `<`, `<=`, `>`, `>=` | `keyword.operator.comparison.relang` |
| `=` | `keyword.operator.assignment.relang` |
| `and`, `or`, `not` | `keyword.operator.logical.relang` |
| `..`, `..=` | `keyword.operator.range.relang` |
| `->` | `keyword.operator.arrow.relang` |
| `&` | `keyword.operator.product.relang` |
| `\|` | `keyword.operator.union.relang` |

### Punctuation

| Token | Scope |
|-------|-------|
| `(`, `)` | `punctuation.section.parens.relang` |
| `{`, `}` | `punctuation.section.block.relang` |
| `[`, `]` | `punctuation.section.brackets.relang` |
| `,` | `punctuation.separator.comma.relang` |
| `:` | `punctuation.separator.colon.relang` |
| `;` | `punctuation.terminator.statement.relang` |
| `.` | `punctuation.accessor.relang` |
| `_` | `constant.language.wildcard.relang` |

## Language Configuration

The `language-configuration.json` defines:

### Brackets

```json
{
  "brackets": [
    ["{", "}"],
    ["(", ")"]
  ]
}
```

### Auto-Closing Pairs

```json
{
  "autoClosingPairs": [
    { "open": "{", "close": "}" },
    { "open": "(", "close": ")" }
  ]
}
```

### Folding

Code folding is based on brace matching:
```json
{
  "folding": {
    "markers": {
      "start": "^\\s*\\{",
      "end": "^\\s*\\}"
    }
  }
}
```

## Usage

### VS Code

The VS Code extension copies these files during build:

```bash
cd relang-vscode
npm run copy-grammar
```

### IntelliJ

The IntelliJ plugin bundles the grammar via `ReLangTextMateBundleProvider`:

```kotlin
class ReLangTextMateBundleProvider : TextMateBundleProvider {
    override fun getBundles(): List<TextMateBundle> {
        return listOf(
            TextMateBundle(
                "relang",
                "/syntaxes/relang.tmLanguage.json",
                "/language-configuration.json"
            )
        )
    }
}
```

## Testing the Grammar

### VS Code

1. Open VS Code in the `relang-vscode` folder
2. Press F5 to launch Extension Development Host
3. Open a `.re` file
4. Use "Developer: Inspect Editor Tokens and Scopes" to verify scopes

### IntelliJ

1. Run `./gradlew :relang-intellij:runIde`
2. Open a `.re` file
3. Check that keywords, literals, etc. are highlighted

## Example Highlighting

```
type Person {                    // 'type' = keyword, 'Person' = type name
    name: String                 // ':' = colon, 'String' = type name
    age: Int                     // 'Int' = type name
}

fn greet(p: Person): String {    // 'fn' = keyword, 'greet' = function name, ':' = return type
    let msg = "Hello, ${p.name}" // 'let' = keyword, string with interpolation
    return msg
}

let timeout = 5s                 // duration literal
let data = b"\x48\x65\x6c\x6c"  // bytes literal

for i in 0..10 {                 // 'for'/'in' = keywords, '..' = range operator
    match i {                    // 'match' = keyword
        0 -> "zero"             // '->' = match arrow
        _ -> "other"            // '_' = wildcard
    }
}

let result = 3.14 + 2.0         // float literals
let check = true and not false   // 'and'/'not' = logical operators
```

## Extending the Grammar

### Adding a New Keyword

1. Add to the appropriate keyword group in `relang.tmLanguage.json`:
   - Control flow → `keyword.control.relang` pattern
   - Declarations → `keyword.declaration.relang` pattern
   - Logical operators → `keyword.operator.logical.relang` pattern

2. Rebuild extensions:
   ```bash
   cd relang-vscode && npm run copy-grammar
   ./gradlew :relang-intellij:build
   ```

### Pattern Ordering

TextMate matches patterns top-to-bottom — more specific patterns **must** come first. The current order is:

1. Comments
2. Strings (with interpolation)
3. Type declarations (`type Name`, `sealed Name`)
4. Keywords
5. Type names (`Int`, `Float`, etc.)
6. Functions (definitions before calls)
7. Constants (floats before integers, durations, bytes, booleans, none/unit)
8. Operators
9. Punctuation
10. Identifiers (catch-all)

When adding new patterns, place them in the correct position. For example, a new literal type should go in section 7 (constants), and float patterns must precede integer patterns to avoid `3.14` matching as integer `3` followed by `.14`.

## TextMate Grammar Reference

- [TextMate Language Grammars](https://macromates.com/manual/en/language_grammars)
- [VS Code Syntax Highlighting Guide](https://code.visualstudio.com/api/language-extensions/syntax-highlight-guide)
- [Oniguruma Regular Expressions](https://github.com/kkos/oniguruma/blob/master/doc/RE)

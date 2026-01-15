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
| `fn` | `keyword.control.function.relang` |
| `if`, `else` | `keyword.control.conditional.relang` |
| `while` | `keyword.control.loop.relang` |
| `return` | `keyword.control.return.relang` |
| `checkpoint` | `keyword.control.checkpoint.relang` |
| `true`, `false` | `constant.language.boolean.relang` |

### Literals

| Token | Scope |
|-------|-------|
| Numbers (`42`) | `constant.numeric.relang` |
| Booleans | `constant.language.boolean.relang` |

### Identifiers

| Context | Scope |
|---------|-------|
| Function name | `entity.name.function.relang` |
| Function call | `entity.name.function.call.relang` |
| Variable | `variable.other.relang` |

### Operators

| Token | Scope |
|-------|-------|
| `+`, `-`, `*`, `/` | `keyword.operator.arithmetic.relang` |
| `<`, `==` | `keyword.operator.comparison.relang` |
| `=` | `keyword.operator.assignment.relang` |

### Punctuation

| Token | Scope |
|-------|-------|
| `(`, `)` | `punctuation.parenthesis.relang` |
| `{`, `}` | `punctuation.brace.relang` |
| `,` | `punctuation.separator.comma.relang` |
| `;` | `punctuation.terminator.relang` |

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
fn factorial(n) {        // 'fn' = keyword, 'factorial' = function name, 'n' = variable
    if (n < 2) {         // 'if' = keyword, '<' = operator, '2' = number
        return 1;        // 'return' = keyword, '1' = number
    }
    return n * factorial(n - 1);  // '*', '-' = operators, 'factorial' = function call
}

result = factorial(5);   // '=' = assignment, 'result' = variable
checkpoint;              // 'checkpoint' = keyword
```

## Extending the Grammar

### Adding a New Keyword

1. Add to the keywords pattern in `relang.tmLanguage.json`:
   ```json
   {
     "match": "\\b(fn|if|else|while|return|checkpoint|newkeyword)\\b",
     "name": "keyword.control.relang"
   }
   ```

2. Rebuild extensions:
   ```bash
   cd relang-vscode && npm run copy-grammar
   ./gradlew :relang-intellij:build
   ```

### Adding a New Scope

1. Define the pattern:
   ```json
   {
     "match": "your-pattern-here",
     "name": "your.scope.name.relang"
   }
   ```

2. Place it in the appropriate position in the `patterns` array (order matters for precedence)

## TextMate Grammar Reference

- [TextMate Language Grammars](https://macromates.com/manual/en/language_grammars)
- [VS Code Syntax Highlighting Guide](https://code.visualstudio.com/api/language-extensions/syntax-highlight-guide)
- [Oniguruma Regular Expressions](https://github.com/kkos/oniguruma/blob/master/doc/RE)

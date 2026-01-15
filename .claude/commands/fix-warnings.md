# Fix IDE Warnings

Fix all IDE warnings in the specified scope using IntelliJ's code analysis.

## Arguments
- `$ARGUMENTS`: Optional path pattern (e.g., `relang-core/**/*.java`). Defaults to all Java files.

## Instructions

1. **Find all Java files** in the scope using `mcp__jetbrains__find_files_by_glob`
2. **Get problems for each file** using `mcp__jetbrains__get_file_problems` with `errorsOnly: false`
3. **Categorize warnings** by type:
   - Unused code (methods, constructors, variables, imports)
   - Deprecated API usage
   - Missing annotations (`@Serial`, `@Override`, etc.)
   - Field may be final
   - Redundant code
   - Type safety warnings

4. **Fix each warning** based on category:

   | Warning Type | Action |
   |-------------|--------|
   | "is never used" (private) | Remove the unused code |
   | "is never used" (public API) | Keep but note for user review |
   | Deprecated API | Replace with modern alternative |
   | Missing `@Override` | Add the annotation |
   | Missing `@Serial` | Add the annotation |
   | "can be final" | Make field final (except `@Child` fields in Truffle) |
   | Unused import | Remove the import |
   | Redundant cast | Remove the cast |

5. **Special rules for Truffle code**:
   - Never make `@Child` annotated fields final
   - Keep `@CompilationFinal` fields non-final if they use `CompilerDirectives.transferToInterpreterAndInvalidate()`

6. **After fixing**, run `./gradlew build` to verify no compilation errors

7. **Report summary**:
   ```
   Fixed X warnings in Y files:
   - Removed N unused methods
   - Updated M deprecated APIs
   - Added P annotations
   - Skipped Q warnings (public API, requires review)
   ```

## Example Usage

```
/fix-warnings                           # Fix all warnings in project
/fix-warnings relang-core/**/*.java     # Fix warnings in relang-core only
/fix-warnings **/nodes/*.java           # Fix warnings in nodes package
```

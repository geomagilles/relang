# ReLang IntelliJ Plugin

Language support for ReLang, a resumable programming language built on GraalVM Truffle.

## Features

- Syntax highlighting for `.re` files (via TextMate grammar)
- File type recognition with custom icon
- Code folding
- LSP support for code completion, diagnostics, and go-to-definition (via LSP4IJ)

## Quick Start: Test Syntax Highlighting

### Step 1: Build and run the plugin

```bash
# From the project root
./gradlew :relang-intellij:runIde
```

This launches a sandboxed IntelliJ instance with the plugin pre-installed.

### Step 2: Open a sample ReLang file

In the sandbox IDE, open one of the sample files:
- `relang-core/src/test/resources/samples/factorial.re`
- `relang-core/src/test/resources/samples/fibonacci.re`

Or create a new `.re` file with this content:
```
fn factorial(n) {
    if (n < 2) {
        return 1;
    }
    return n * factorial(n - 1);
}

result = factorial(5);
checkpoint;
```

### Step 3: Verify highlighting

You should see:
- `fn`, `if`, `return`, `checkpoint` highlighted as keywords
- `factorial`, `n`, `result` as identifiers
- Numbers highlighted
- Operators and punctuation styled

## Quick Start: Test Code Completion (LSP)

LSP provides code completion, diagnostics, and go-to-definition. This requires GraalVM.

### Step 1: Install LSP4IJ plugin

In the sandbox IDE (or your regular IntelliJ):
1. Go to `Settings` → `Plugins` → `Marketplace`
2. Search for **LSP4IJ** and install it
3. Restart the IDE

### Step 2: Start the LSP server

In a terminal (from the project root):
```bash
./gradlew :relang-core:run --args="--lsp"
```

You should see:
```
Starting ReLang LSP server on port 8123...
[Graal LSP] Starting server and listening on localhost/127.0.0.1:8123
```

Keep this terminal running.

### Step 3: Configure LSP4IJ

In IntelliJ:
1. Go to `Settings` → `Languages & Frameworks` → `Language Servers`
2. Click `+` to add a new server
3. Configure:
   - **Name**: `ReLang`
   - **Server**: Select `TCP` mode
   - **Host**: `localhost`
   - **Port**: `8123`
4. In the **Mappings** tab, add:
   - **File pattern**: `*.re`
5. Click `OK`

### Step 4: Test LSP features

Open a `.re` file. You should now have:
- Error diagnostics (red underlines for syntax errors)
- Code completion (press `Ctrl+Space`)
- Hover documentation

## Prerequisites

- GraalVM 25+ (required for LSP features)
- IntelliJ IDEA 2024.3+ (any edition)

## Building the Plugin

```bash
./gradlew :relang-intellij:build
```

## Installing the Plugin (Production)

### From ZIP file

1. Build the distributable:
   ```bash
   ./gradlew :relang-intellij:buildPlugin
   ```

2. In IntelliJ: `Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk...`

3. Select: `relang-intellij/build/distributions/relang-intellij-*.zip`

4. Restart IntelliJ

## Project Structure

```
relang-intellij/
├── build.gradle                    # Plugin build configuration
├── src/main/
│   ├── kotlin/com/relang/intellij/
│   │   ├── ReLangLanguage.kt       # Language definition
│   │   ├── ReLangFileType.kt       # File type (.re)
│   │   ├── ReLangFile.kt           # PSI file
│   │   ├── ReLangParserDefinition.kt
│   │   └── ReLangTextMateBundleProvider.kt
│   └── resources/
│       ├── META-INF/plugin.xml     # Plugin descriptor
│       └── icons/relang.svg        # File icon
└── build/
    └── distributions/              # Built plugin ZIP
```

## Troubleshooting

**Plugin not loading?**
- Check `Help` → `Show Log in Finder/Explorer` for errors
- Ensure you're using IntelliJ 2024.3+

**Syntax highlighting not working?**
- Verify file has `.re` extension
- Try restarting IntelliJ (the TextMate bundle is extracted on first load)

**LSP not connecting?**
- Verify the LSP server is running: `./gradlew :relang-core:run --args="--lsp"`
- Check LSP4IJ is configured with TCP mode, host `localhost`, port `8123`
- Check LSP4IJ mappings include `*.re`

**Build fails?**
- Ensure GraalVM 25+ is configured
- Run `./gradlew clean :relang-intellij:build`

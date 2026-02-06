# ReLang VS Code Extension

Language support for ReLang, a resumable programming language built on GraalVM Truffle.

## Features

- Syntax highlighting for `.re` files
- Bracket matching and auto-closing
- Code folding
- LSP support (code completion, diagnostics, go-to-definition) when connected to GraalVM LSP server

## Quick Start: Test Syntax Highlighting

### Step 1: Setup the extension

```bash
cd relang-vscode

# Install dependencies
npm install

# Copy the TextMate grammar from the shared module
npm run copy-grammar
```

### Step 2: Launch the extension

1. Open the `relang-vscode` folder in VS Code:
   ```bash
   code relang-vscode
   ```

2. Press `F5` to launch the **Extension Development Host**
   - This opens a new VS Code window with your extension loaded

### Step 3: Open a sample ReLang file

In the Extension Development Host window, open one of the sample files:
- File → Open → navigate to `relang-core/src/test/resources/samples/factorial.re`

Or create a new file `test.re` with this content:
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

### Step 4: Verify highlighting

You should see:
- `fn`, `if`, `return`, `checkpoint` highlighted as keywords
- `factorial`, `n`, `result` as identifiers
- Numbers highlighted
- Operators and punctuation styled

## Quick Start: Test Code Completion (LSP)

LSP provides code completion, diagnostics, and go-to-definition. This requires GraalVM.

### Step 1: Start the LSP server

In a terminal (from the project root):
```bash
./gradlew :relang-lsp:run
```

You should see:
```
Starting ReLang LSP server on port 8123...
[Graal LSP] Starting server and listening on localhost/127.0.0.1:8123
```

Keep this terminal running.

### Step 2: Launch VS Code with the extension

```bash
cd relang-vscode
code .
```

Press `F5` to launch the Extension Development Host.

### Step 3: Test LSP features

Open a `.re` file in the Extension Development Host. You should now have:
- Error diagnostics (red underlines for syntax errors)
- Code completion (press `Ctrl+Space`)
- Hover documentation

Check the Output panel (`View` → `Output` → select "ReLang Language Server") to verify the connection.

## Prerequisites

- Node.js 18+ and npm
- VS Code
- GraalVM 25+ (required for LSP features)

## Building the Extension

```bash
# Compile TypeScript
npm run compile

# Package as .vsix file
npm run package
```

This creates `relang-vscode-0.1.0.vsix` in the current directory.

## Installing the Extension (Production)

```bash
code --install-extension relang-vscode-0.1.0.vsix
```

Or in VS Code: `Extensions` → `...` menu → `Install from VSIX...`

## Configuration

Open VS Code settings (`Cmd+,` / `Ctrl+,`) and search for "relang":

| Setting | Default | Description |
|---------|---------|-------------|
| `relang.lsp.enabled` | `true` | Enable LSP support |
| `relang.lsp.port` | `8123` | LSP server port |

## Project Structure

```
relang-vscode/
├── package.json          # Extension manifest
├── src/
│   └── extension.ts      # Extension entry point (LSP client)
├── syntaxes/             # Copied from relang-textmate
│   └── relang.tmLanguage.json
├── language-configuration.json  # Copied from relang-textmate
└── out/                  # Compiled JavaScript (generated)
```

## Troubleshooting

**Syntax highlighting not working?**
- Ensure you ran `npm run copy-grammar`
- Check the file has `.re` extension

**LSP not connecting?**
- Verify the LSP server is running: `./gradlew :relang-lsp:run`
- Check the port setting matches (default: 8123)
- Look at VS Code's Output panel → "ReLang Language Server"

# Shell Action Family

*Shell command execution*

---

## Overview

The `shell` family executes shell commands and captures their output.

**Scope:**
- Non-interactive command execution only
- Capture stdout, stderr, exit code
- Working directory and environment control
- No interactive sessions

---

## Error Types

```relang
sealed ShellError

type Timeout : ShellError {
    duration: Duration
    partialOutput: ShellOutput?
}

type ExecutionFailed : ShellError {
    exitCode: Int
    stdout: String
    stderr: String
}

type CommandNotFound : ShellError {
    command: String
}

type PermissionDenied : ShellError {
    path: String
}

type Cancelled : ShellError {
    partialOutput: ShellOutput?
}
```

---

## Response Type

```relang
type ShellOutput {
    exitCode: Int
    stdout: String
    stderr: String
    duration: Duration
}
```

**Output mode determines return type:**

| Mode | Return Type | Use Case |
|------|-------------|----------|
| `All` | `ShellOutput` | Need everything (default) |
| `Stdout` | `String` | Just stdout |
| `Stderr` | `String` | Just stderr |
| `Code` | `Int` | Just exit code |
| `None` | `Unit` | Fire and forget |

---

## Core API

### Direct Execution (Recommended)

```relang
// Safe execution - no shell interpretation
shell.run(program: String, args: List<String>): *ShellOutput
shell.run(program: String, args: List<String>, options: ShellOptions): *ShellOutput
```

Executes the program directly without shell interpretation. Arguments are passed as-is, avoiding injection risks.

### Shell Interpretation

```relang
// Shell interpretation - for pipes, redirects, globs
shell.command(command: String): *ShellOutput
shell.command(command: String, options: ShellOptions): *ShellOutput
```

Passes the command string to a shell for interpretation. Use when you need shell features like pipes (`|`), redirects (`>`), globs (`*`), or variable expansion.

---

## Options Type

```relang
type ShellOptions {
    cwd: String?                  // Working directory
    env: Map<String, String>?     // Environment variables
    appendEnv: Bool?              // Merge with parent env (default: true)
    stdin: String?                // Standard input content
    shell: String?                // Shell to use (default: /bin/sh)
    successCodes: List<Int>?      // Exit codes considered success (default: [0])
    output: ShellOutputMode?      // Output selection (default: All)
}

enum ShellOutputMode {
    All       // Full ShellOutput (default)
    Stdout    // String - just stdout
    Stderr    // String - just stderr
    Code      // Int - just exit code
    None      // Unit - fire and forget
}
```

---

## Usage Examples

### Simple Command

```relang
let result = await shell.run("echo", ["hello world"])!
print(result.stdout)  // "hello world\n"
```

### With Options

```relang
let result = await shell.run("npm", ["install"], ShellOptions {
    cwd: "/app",
    env: { "NODE_ENV": "production" }
})!
```

### Git Status Check

```relang
let result = await shell.run("git", ["status", "--porcelain"], ShellOptions {
    cwd: repoPath
})!

if result.stdout != "" {
    print("Repository has uncommitted changes")
}
```

### Shell Interpretation (Pipes and Globs)

```relang
// Use shell.command() for pipes, redirects, globs
let result = await shell.command("find . -name '*.txt' | wc -l")!
let count = parseInt(result.stdout.trim())
```

### Error Handling

```relang
match await shell.run("make", ["build"]) {
    out: ShellOutput -> print("Build succeeded in ${out.duration}")
    f: Failure -> match f.error {
        e: ExecutionFailed -> {
            print("Build failed with exit code ${e.exitCode}")
            print("stderr: ${e.stderr}")
        }
        t: Timeout -> print("Build timed out after ${t.duration}")
        _ -> reportError(f)
    }
}
```

### Multiple Exit Codes as Success

```relang
// grep: 0 = found, 1 = not found (both OK for checking)
let result = await shell.run("grep", ["pattern", "file.txt"], ShellOptions {
    successCodes: [0, 1]
})!
```

### Output Modes

```relang
// Just get stdout as String
let output: String = shell.run("cat", ["data.txt"], ShellOptions {
    output: Stdout
})!

// Just check exit code
let code: Int = shell.run("test", ["-f", path], ShellOptions {
    output: Code
})!

// Fire and forget
shell.run("cleanup", ["--quiet"], ShellOptions {
    output: None
})!
```

### Parallel Commands

```relang
let (build & lint & test) = (
    shell.run("make", ["build"]) and
    shell.run("make", ["lint"]) and
    shell.run("make", ["test"])
)!
```

### With Timeout

```relang
let result = timeout(shell.run("./long-script.sh", []), 5m)!
```

### Stdin Piping

```relang
// Pass data via stdin
let result = await shell.run("wc", ["-l"], ShellOptions {
    stdin: fileContent
})!

// Chain with previous output
let files = await shell.run("find", [".", "-name", "*.txt"])!
let count = await shell.run("wc", ["-l"], ShellOptions {
    stdin: files.stdout
})!
```

---

## Security Considerations

### Command Injection Risk

```relang
// UNSAFE: Command injection risk with shell.command()
let result = await shell.command("cat ${userInput}")!  // DON'T DO THIS
```

### Safe Alternative

```relang
// SAFE: Use shell.run() with explicit arguments
let result = await shell.run("cat", [userInput])!
```

### When Shell Interpretation is Needed

```relang
// If you must use shell.command(), validate input first
if isValidFilename(userInput) {
    let result = await shell.command("cat '${userInput}'")!
}

// Or use shell features with no user input
let result = await shell.command("ls -la | grep '.txt$'")!
```

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Non-interactive only | Orchestrator scope; interactive sessions require complex state management |
| `shell.run()` as primary | Safe by default; explicit arguments prevent injection |
| `shell.command()` for shell features | Named to indicate shell interpretation; use when pipes/globs needed |
| Output modes | Flexibility; common case is just stdout or exit code |
| Exit code semantics | Non-zero as error is conventional; `successCodes` for exceptions |
| Partial output in errors | Debugging aid; shows what happened before failure |
| `stdin` support | Enable data piping between commands |

---

## Comparison with Serverless Workflow

| Feature | Serverless Workflow | ReLang |
|---------|---------------------|--------|
| Shell command | `run.shell.command` | `shell.command(cmd)` |
| Safe execution | N/A | `shell.run(prog, args)` |
| Stdin | `stdin` expression | `stdin` in options |
| Arguments | `arguments` array | Second arg to `shell.run()` |
| Environment | `environment` map | `env` in options |
| Working directory | Via `cd` in command | `cwd` in options |
| Output selection | `return` property | `output` enum |

---

*End of Shell action family*

# How to Run ReLang Programs

This guide covers the different ways to execute ReLang programs.

## Running a File

Execute a `.re` file:

```bash
relang myprogram.re
```

The result of the last expression is printed:

```bash
$ cat add.re
1 + 2

$ relang add.re
Result: 3
```

## Using the REPL

Start an interactive session:

```bash
relang
```

```
ReLang REPL (type 'exit' to quit)
================================
relang> x = 10
=> 10
relang> x * 2
=> 20
relang> fn double(n) { return n * 2; }
relang> double(21)
=> 42
relang> exit
```

## Running with the Debugger

Enable Chrome DevTools debugging:

```bash
relang --inspect myprogram.re
```

```
Debugger listening on port 4711
Connect Chrome DevTools to: chrome://inspect
```

Specify a custom port:

```bash
relang --inspect --inspect.port 9229 myprogram.re
```

## Running with Resumption

See [How to Suspend and Resume Execution](how-to-suspend-resume.md) for details.

```bash
# Run with state persistence
relang program.re --state-out state.json

# Resume from saved state
relang program.re --state-in state.json --state-out state.json
```

## Development vs Production

### Development (JVM mode)

```bash
./gradlew run --args="myprogram.re"
```

- Slower startup (~500ms)
- Faster peak performance (JIT compilation)
- Full debugging support

### Production (Native mode)

```bash
relang myprogram.re
```

- Fast startup (~50ms)
- Lower memory usage
- Self-contained binary

## See Also

- [CLI Reference](reference-cli.md) - Complete command-line options
- [Getting Started](tutorial-getting-started.md) - Installation and setup

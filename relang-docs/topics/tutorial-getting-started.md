# Getting Started with ReLang

This tutorial will guide you through installing ReLang and running your first program.

## Prerequisites

- Java 21 or later (GraalVM recommended)
- Gradle 8.x (included via wrapper)

## Building ReLang

Clone the repository and build:

```bash
git clone https://github.com/your-org/relang.git
cd relang
./gradlew build
```

## Running Your First Program

Create a file `hello.re`:

```
x = 40 + 2;
x
```

Run it:

```bash
./gradlew run --args="hello.re"
```

You should see:
```
Result: 42
```

## Using the REPL

Start the interactive REPL:

```bash
./gradlew run
```

```
ReLang REPL (type 'exit' to quit)
================================
relang> 1 + 2
=> 3
relang> x = 10
=> 10
relang> x * 2
=> 20
relang> exit
```

## Next Steps

- Learn about [suspending and resuming execution](howto-suspend-resume.md)
- Read the [CLI reference](reference-cli.md)
- Understand [how resumability works](explanation-resumability.md)

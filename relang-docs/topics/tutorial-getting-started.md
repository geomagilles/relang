# Getting Started with ReLang

This tutorial walks you through installing ReLang and running your first program.
By the end, you'll have a working environment ready to write durable programs.

## Prerequisites

- Basic familiarity with command-line terminals
- JDK 21 or higher (only required if using the Java JAR version)

## 1. Download ReLang

Choose your platform and download the appropriate binary:

<tabs group="platform">
<tab id="macos" title="macOS (ARM64)" group-key="macos">

```bash
# Download and extract the native binary
curl -L https://github.com/relang/relang/releases/download/v%version%/relang-v%version%-macos-arm64.tar.gz | tar -xz

# Verify the installation
./relang --version
```

</tab>
<tab id="linux" title="Linux (x86_64)" group-key="linux">

```bash
# Download and extract the native binary
curl -L https://github.com/relang/relang/releases/download/v%version%/relang-v%version%-linux-x86_64.tar.gz | tar -xz

# Verify the installation
./relang --version
```

</tab>
<tab id="windows" title="Windows (x86_64)" group-key="windows">

```powershell
# Download and extract the native binary
Invoke-WebRequest -Uri "https://github.com/relang/relang/releases/download/v%version%/relang-v%version%-windows-x86_64.zip" -OutFile "relang.zip"
Expand-Archive -Path "relang.zip" -DestinationPath "."
Remove-Item "relang.zip"

# Verify the installation
.\relang.exe --version
```

</tab>
<tab id="java" title="Java (Any OS)" group-key="java">

Requires JDK 21 or higher.

```bash
# Download the JAR
curl -L https://github.com/relang/relang/releases/download/v%version%/relang-v%version%.jar -o relang.jar

# Verify the installation
java -jar relang.jar --version
```

</tab>
</tabs>

You should see output showing the ReLang version (e.g., `%version%`).

## 2. Write Your First Program

Create a file named `hello.re`:

```relang
let message = "Hello, ReLang!"
print(message)
```

Run it:

<tabs group="platform">
<tab id="macos-run" title="macOS" group-key="macos">

```bash
./relang hello.re
```

</tab>
<tab id="linux-run" title="Linux" group-key="linux">

```bash
./relang hello.re
```

</tab>
<tab id="windows-run" title="Windows" group-key="windows">

```powershell
.\relang.exe hello.re
```

</tab>
<tab id="java-run" title="Java" group-key="java">

```bash
java -jar relang.jar hello.re
```

</tab>
</tabs>

You should see:

```
Hello, ReLang!
```

## 3. Try the REPL

Start the interactive REPL for quick experimentation:

```bash
./relang
```

```
ReLang %version% (type 'exit' to quit)
> 1 + 2
3
> let x = 10
> x * 2
20
> exit
```

## Your Environment is Ready!

You now have:

- ReLang interpreter installed and working
- The ability to run `.re` source files
- An interactive REPL for experimentation

## Next Steps

Continue with the [Working with Awaitables](tutorial-awaitables.md) tutorial to learn ReLang's core abstraction for durable execution.

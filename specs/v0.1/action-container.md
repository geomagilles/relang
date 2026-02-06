# Container Action Family

*Run container workloads*

---

## Overview

The `container` family runs container workloads and captures their output.

**Scope:**
- Run-to-completion model
- Capture stdout, stderr, exit code
- Mount volumes and set environment
- Pull images as needed

**Not supported:**
- Long-running services
- Container networking
- Container exec after start
- Log streaming

**v0.1 profile note**: no core `timeout(...)` primitive. Model timeouts with `timer(...)` + `or`.

---

## Error Types

```relang
sealed ContainerError

type Timeout : ContainerError {
    duration: Duration
    containerId: String?
    partialOutput: ContainerOutput?
}

type ImageNotFound : ContainerError {
    image: String
    registry: String?
}

type ImagePullFailed : ContainerError {
    image: String
    message: String
}

type ContainerFailed : ContainerError {
    containerId: String
    exitCode: Int
    stdout: String
    stderr: String
}

type ResourceExhausted : ContainerError {
    resource: String
    limit: String
    requested: String?
}

type Cancelled : ContainerError {
    containerId: String?
    partialOutput: ContainerOutput?
}
```

---

## Response Type

```relang
type ContainerOutput {
    containerId: String
    exitCode: Int
    stdout: String
    stderr: String
    duration: Duration
}
```

**Output mode determines return type:**

| Mode | Return Type | Use Case |
|------|-------------|----------|
| `All` | `ContainerOutput` | Need everything (default) |
| `Stdout` | `String` | Just stdout |
| `Stderr` | `String` | Just stderr |
| `Code` | `Int` | Just exit code |
| `None` | `Unit` | Fire and forget |

---

## Core API

### Container Execution

```relang
container.run(image: String): *ContainerOutput
container.run(image: String, options: ContainerOptions): *ContainerOutput

container.run(image: String, command: List<String>): *ContainerOutput
container.run(image: String, command: List<String>, options: ContainerOptions): *ContainerOutput
```

### Options Type

```relang
type ContainerOptions {
    // Execution
    command: List<String>?
    args: List<String>?
    workdir: String?
    user: String?
    stdin: String?               // Data to pipe to stdin

    // Environment
    env: Json?
    envFrom: List<EnvSource>?

    // Storage
    mounts: List<Mount>?

    // Resources
    memory: String?              // e.g., "512Mi"
    cpu: String?                 // e.g., "500m"

    // Image
    pullPolicy: PullPolicy?      // Default: IfNotPresent
    pullSecret: String?

    // Networking
    network: String?

    // Output
    output: ContainerOutputMode? // Default: All
}

enum ContainerOutputMode {
    All       // Full ContainerOutput (default)
    Stdout    // String - just stdout
    Stderr    // String - just stderr
    Code      // Int - just exit code
    None      // Unit - fire and forget
}

type Mount {
    source: String
    target: String
    readOnly: Bool?
}

type EnvSource =
    | SecretRef { name: String, key: String? }
    | ConfigMapRef { name: String, key: String? }

enum PullPolicy {
    Always
    IfNotPresent
    Never
}
```

---

## Usage Examples

### Simple Container Run

```relang
let result = await container.run("alpine:latest", ["echo", "hello"])!
print(result.stdout)  // "hello\n"
```

### Run with Options

```relang
let result = await container.run("python:3.11", ["python", "script.py"], ContainerOptions {
    mounts: [Mount { source: "./scripts", target: "/app", readOnly: true }],
    workdir: "/app",
    env: { "PYTHONUNBUFFERED": "1" },
    memory: "256Mi",
    cpu: "500m"
})!
```

### Build Process

```relang
let build = await container.run("node:18", ["npm", "run", "build"], ContainerOptions {
    mounts: [
        Mount { source: "./src", target: "/app/src", readOnly: true },
        Mount { source: "./dist", target: "/app/dist" }
    ],
    workdir: "/app",
    memory: "1Gi"
})!
```

### Error Handling

```relang
match await container.run("tests:latest") {
    out: ContainerOutput -> print("Tests passed in ${out.duration}")
    f: Failure -> match f.kind {
        af: ActionFailed -> match af.error {
            e: ContainerFailed -> {
                print("Tests failed with exit code ${e.exitCode}")
                print("stderr: ${e.stderr}")
            }
            i: ImageNotFound -> print("Image not found: ${i.image}")
            t: Timeout -> print("Tests timed out")
            _ -> reportError(f)
        }
        _ -> reportError(f)
    }
}
```

### Parallel Container Runs

```relang
let (build & lint & test) = (await (
    container.run("builder:latest", ["make", "build"]) and
    container.run("linter:latest", ["make", "lint"]) and
    container.run("tester:latest", ["make", "test"])
))!
```

### With Timeout

```relang
let winner = await (
    container.run("heavy-job:latest") or
    timer(10m)
)
match winner {
    out: ContainerOutput -> handle(out)
    _ -> timeoutExceeded()
}
```

### Environment from Secrets

```relang
let result = await container.run("app:latest", ContainerOptions {
    envFrom: [
        SecretRef { name: "db-credentials" },
        ConfigMapRef { name: "app-config" }
    ]
})!
```

### Data Processing Pipeline

```relang
// Process data in isolated containers
let extracted = await container.run("extractor:v1", ["extract", inputPath], ContainerOptions {
    mounts: [Mount { source: dataDir, target: "/data" }]
})!

let transformed = await container.run("transformer:v1", ["transform"], ContainerOptions {
    mounts: [Mount { source: dataDir, target: "/data" }]
})!

let loaded = await container.run("loader:v1", ["load", outputPath], ContainerOptions {
    mounts: [Mount { source: dataDir, target: "/data" }]
})!
```

### Output Modes

```relang
// Just get stdout
let output: String = container.run("processor:v1", ContainerOptions {
    output: Stdout
})!

// Fire and forget
container.run("cleanup:v1", ContainerOptions {
    output: None
})!
```

### Stdin Piping

```relang
// Pass data to container via stdin
let result = await container.run("jq:latest", [".items[]"], ContainerOptions {
    stdin: jsonData
})!
```

---

## Kubernetes Integration

For Kubernetes environments:

```relang
type ContainerOptions {
    // ... previous fields ...

    // Kubernetes-specific
    namespace: String?
    serviceAccount: String?
    nodeSelector: Json?
    tolerations: List<Toleration>?
    labels: Json?
    annotations: Json?
}
```

### Kubernetes Example

```relang
let result = await container.run("gpu-job:latest", ContainerOptions {
    namespace: "ml-jobs",
    serviceAccount: "gpu-runner",
    nodeSelector: { "gpu": "true" },
    memory: "16Gi",
    labels: { "job-type": "training" }
})!
```

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Run-to-completion only | Orchestrator scope; services are complex lifecycle |
| Output modes | Flexibility; often only need stdout or exit code |
| Exit code in error | Non-zero typically means failure |
| Partial output in Timeout | Debugging aid |
| Explicit mounts | Security; no implicit host access |
| Resource limits | Prevent runaway containers |
| `stdin` support | Enable data piping into containers |

---

## Comparison with Serverless Workflow

| Feature | Serverless Workflow | ReLang |
|---------|---------------------|--------|
| Image | `image` | `image` |
| Command | `command` (string) | `command` (list) |
| Arguments | `arguments` | `args` |
| Environment | `environment` | `env` |
| Volumes | `volumes` | `mounts` |
| Pull policy | `pullPolicy` | `pullPolicy` |
| Stdin | `stdin` expression | `stdin` in options |
| Output selection | `return` property | `output` enum |
| Lifecycle | `lifetime` config | Run-to-completion only |

---

*End of Container action family*

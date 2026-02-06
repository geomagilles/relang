# gRPC Action Family

*Typed RPC calls to gRPC services*

---

## Overview

The `grpc` family provides typed RPC calls to gRPC services.

**Scope:**
- Unary calls only (request-response)
- No streaming (client, server, bidirectional)
- Metadata support (headers/trailers)
- Generated clients from protobuf

**v0.1 profile note**: `timeout(...)`/`retry(...)` are not language primitives. Timeout patterns use `timer(...)` + `or`; retries are configured at runtime/policy level.

---

## Error Types

```relang
sealed GrpcError

type Timeout : GrpcError {
    duration: Duration
}

type ConnectionFailed : GrpcError {
    target: String
    message: String?
}

type Unavailable : GrpcError {
    target: String
    message: String?
}

type DeadlineExceeded : GrpcError {
    deadline: Duration
}

type GrpcStatus : GrpcError {
    code: GrpcCode
    message: String
    details: Bytes?
    trailers: Json?
}

type Cancelled : GrpcError {}
```

### Status Codes

```relang
enum GrpcCode {
    OK = 0
    CANCELLED = 1
    UNKNOWN = 2
    INVALID_ARGUMENT = 3
    DEADLINE_EXCEEDED = 4
    NOT_FOUND = 5
    ALREADY_EXISTS = 6
    PERMISSION_DENIED = 7
    RESOURCE_EXHAUSTED = 8
    FAILED_PRECONDITION = 9
    ABORTED = 10
    OUT_OF_RANGE = 11
    UNIMPLEMENTED = 12
    INTERNAL = 13
    UNAVAILABLE = 14
    DATA_LOSS = 15
    UNAUTHENTICATED = 16
}
```

---

## Service Definition Pattern

gRPC services are defined via protobuf and generate typed clients.

### Protobuf Definition

```protobuf
// users.proto
service UserService {
    rpc GetUser(GetUserRequest) returns (User);
    rpc CreateUser(CreateUserRequest) returns (User);
    rpc ListUsers(ListUsersRequest) returns (ListUsersResponse);
}
```

### Generated ReLang Client

```relang
// Auto-generated from users.proto
action family userService : grpc {
    errors: GrpcError
    target: "users.example.com:443"

    fn getUser(request: GetUserRequest): *User
    fn createUser(request: CreateUserRequest): *User
    fn listUsers(request: ListUsersRequest): *ListUsersResponse
}
```

---

## Core API

### Generated Client Usage

```relang
let user = await userService.getUser(GetUserRequest { userId: "123" })!
```

### With Options

```relang
let user = await userService.getUser(
    GetUserRequest { userId: "123" },
    GrpcOptions {
        metadata: { "x-request-id": requestId },
        deadline: 5s
    }
)!
```

### Options Type

```relang
type GrpcOptions {
    metadata: Json?
    deadline: Duration?
    target: String?
    authority: String?
    credentials: GrpcCredentials?
}

type GrpcCredentials =
    | Insecure {}
    | ServerTls { ca: Bytes? }
    | MutualTls { ca: Bytes?, cert: Bytes, key: Bytes }
    | Token { token: String }
```

### Response with Metadata

```relang
type GrpcResponse<T> {
    data: T
    metadata: Json
    trailers: Json
}

// When metadata is needed
let resp: GrpcResponse<User> = userService.getUser.withMetadata(request)!
let user = resp.data
let traceId = resp.metadata["x-trace-id"]
```

### Dynamic Calls

For services without generated clients:

```relang
let response: Bytes = grpc.call(
    target: "users.example.com:443",
    service: "users.UserService",
    method: "GetUser",
    request: Bytes,
    options: GrpcOptions?
)!
```

**Recommendation**: Use generated clients whenever possible.

---

## Usage Examples

### Simple Unary Call

```relang
let user = await userService.getUser(GetUserRequest { userId: id })!
```

### Timeout Pattern (v0.1 Core)

```relang
let winner = await (userService.getUser(request) or timer(5s))
match winner {
    user: User -> processUser(user)
    _ -> timeoutExceeded()
}
```

### Parallel Calls

```relang
let (user & orders) = (await (
    userService.getUser(GetUserRequest { userId: id }) and
    orderService.listOrders(ListOrdersRequest { userId: id })
))!
```

### Error Handling

```relang
match await userService.getUser(request) {
    user: User -> processUser(user)
    f: Failure -> match f.kind {
        af: ActionFailed -> match af.error {
            s: GrpcStatus -> match s.code {
                NOT_FOUND -> createNewUser()
                PERMISSION_DENIED -> reportAccessDenied()
                _ -> reportError(s.message)
            }
            t: Timeout -> retryLater()
            _ -> reportError(f)
        }
        _ -> reportError(f)
    }
}
```

### Multiple Services

```relang
// Coordinate across services
let (user & account & preferences) = (await (
    userService.getUser(userReq) and
    accountService.getAccount(accountReq) and
    preferencesService.get(prefReq)
))!
```

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Unary only | Streaming requires different execution model; out of orchestrator scope |
| Generated clients | Type safety, IDE support, compile-time validation |
| `GrpcStatus` includes code enum | Type-safe status handling |
| Separate `metadata` from response | Most calls don't need it; `.withMetadata()` opt-in |
| Dynamic `grpc.call()` escape hatch | Flexibility for untyped scenarios |

---

## Comparison with Serverless Workflow

| Feature | Serverless Workflow | ReLang |
|---------|---------------------|--------|
| Proto definition | `proto.endpoint` (runtime) | Compile-time code generation |
| Service reference | `service.name`, `service.host` | Generated client with default target |
| Method call | `method` + `arguments` | Typed method with request object |
| Authentication | `service.authentication` | `GrpcCredentials` in options |
| Metadata | N/A | `metadata` in options |
| Target override | Inline in call | `target` in options |

**Key difference**: ReLang uses compile-time code generation for full type safety, while Serverless Workflow uses runtime proto loading.

---

*End of gRPC action family*

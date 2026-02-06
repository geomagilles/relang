# HTTP Action Family

*Request-response HTTP operations*

---

## Overview

The `http` family provides HTTP/1.1 and HTTP/2 request-response operations.

**Scope:**
- Request-response only
- No streaming (SSE, chunked, WebSocket)
- Full header and body control
- Authentication helpers

**v0.1 profile note**: `timeout(...)`/`retry(...)` are not language primitives. Timeout patterns use `timer(...)` + coordination (`or`), while retries are runtime policy concerns.

---

## Error Types

```relang
sealed HttpError

type Timeout : HttpError {
    duration: Duration
}

type DnsFailure : HttpError {
    host: String
    message: String?
}

type ConnectionRefused : HttpError {
    host: String
    port: Int
}

type ConnectionReset : HttpError {
    host: String
}

type TlsError : HttpError {
    host: String
    reason: String
}

type HttpStatus : HttpError {
    status: Int
    statusText: String              // e.g., "Not Found"
    body: Bytes?
    headers: Json                   // Multi-value headers preserved
}

type RequestTooLarge : HttpError {
    maxSize: Int
    actualSize: Int
}

type ResponseTooLarge : HttpError {
    maxSize: Int
    actualSize: Int?
}

type InvalidResponse : HttpError {
    message: String
    partialData: Bytes?
}

type ProtocolError : HttpError {
    protocol: String                // "HTTP/1.1" or "HTTP/2"
    code: String?                   // e.g., HTTP/2 error code
    message: String
}

type Cancelled : HttpError {}
```

**Design note**: `HttpStatus` is an error for non-2xx responses. This keeps the success path clean. For APIs where any status is acceptable, use `http.request()`.

---

## Response Types

```relang
// Full response (when output: Response)
type HttpResponse {
    status: Int
    statusText: String
    headers: Json                   // Case-insensitive keys, values are strings or arrays
    body: Bytes

    // Body parsing
    fn text(): String
    fn json<T>(): T | JsonError

    // Header access (case-insensitive)
    fn header(name: String): String?           // First value
    fn headerAll(name: String): List<String>?  // All values

    // Cache-related (RFC 9111)
    fn etag(): String?
    fn lastModified(): Timestamp?
    fn cacheControl(): CacheDirectives?
}

// Content only (when output: Content, default)
// Returns parsed body directly based on Content-Type

// Raw bytes (when output: Raw)
// Returns Bytes directly
```

**Output mode determines return type:**

| Mode | Return Type | Use Case |
|------|-------------|----------|
| `Content` | Parsed body (`T`) | Most API calls |
| `Response` | `HttpResponse` | Need headers/status |
| `Raw` | `Bytes` | Binary data, no parsing |

---

## Core API

### Basic Requests

```relang
// Safe methods (no body by default)
http.get(url: String): *HttpResponse
http.get(url: String, options: HttpOptions): *HttpResponse

http.head(url: String): *HttpResponse
http.head(url: String, options: HttpOptions): *HttpResponse

http.options(url: String): *HttpResponse
http.options(url: String, options: HttpOptions): *HttpResponse

// Methods with body
http.post(url: String, body: HttpBody): *HttpResponse
http.post(url: String, body: HttpBody, options: HttpOptions): *HttpResponse

http.put(url: String, body: HttpBody): *HttpResponse
http.put(url: String, body: HttpBody, options: HttpOptions): *HttpResponse

http.patch(url: String, body: HttpBody): *HttpResponse
http.patch(url: String, body: HttpBody, options: HttpOptions): *HttpResponse

http.delete(url: String): *HttpResponse
http.delete(url: String, body: HttpBody, options: HttpOptions): *HttpResponse

// Generic request (any method, any status is success)
http.request(method: HttpMethod, url: String, options: HttpRequestOptions): *HttpResponse

enum HttpMethod {
    GET, HEAD, POST, PUT, DELETE, PATCH, OPTIONS
    // TRACE and CONNECT intentionally omitted (security/tunneling)
}
```

### Options Type

```relang
type HttpOptions {
    // Headers (case-insensitive keys)
    headers: Json?                  // { "Content-Type": "application/json", ... }

    // URL
    query: Json?                    // { "page": "1", "limit": "10" }

    // Auth
    auth: HttpAuth?

    // Redirects (RFC 9110 §15.4)
    followRedirects: Bool?          // Default: true
    maxRedirects: Int?              // Default: 10
    redirectMode: RedirectMode?     // Default: Follow

    // Conditional requests (RFC 9110 §13)
    ifNoneMatch: String?            // ETag for cache validation
    ifModifiedSince: Timestamp?     // Date-based cache validation
    ifMatch: String?                // ETag for optimistic concurrency

    // Caching (RFC 9111)
    cacheMode: CacheMode?           // Default: Default

    // Output
    output: HttpOutput?             // Default: Content

    // Idempotency
    idempotencyKey: String?         // For safe retries
}

enum HttpOutput {
    Raw         // Bytes only, no parsing
    Content     // Parsed body (default)
    Response    // Full response with headers, status, body
}

enum RedirectMode {
    Follow      // Follow all redirects (default)
    Manual      // Return 3xx as success, don't follow
    Error       // Treat 3xx as error
}

enum CacheMode {
    Default     // Normal caching behavior
    NoStore     // Don't cache, don't use cache
    NoCache     // Validate with server before using cache
    ForceCache  // Use cache even if stale
    OnlyCache   // Only use cache, fail if not cached
}
```

### Body Types

```relang
type HttpBody =
    | JsonBody { value: Json }
    | Form { fields: Json }         // { "field1": "value1", ... }
    | Multipart { parts: List<Part> }
    | Raw { data: Bytes, contentType: String }
    | Text { data: String }
    | None {}

type Part {
    name: String
    filename: String?
    contentType: String?
    data: Bytes
}
```

### Authentication (RFC 7235)

```relang
type HttpAuth =
    | Bearer { token: String }
    | Basic { username: String, password: String }
    | Digest { username: String, password: String }
    | ApiKey { header: String, value: String }
    | OAuth2 { token: String, tokenType: String? }
    | Custom { headers: Json }
```

### Content Negotiation (RFC 9110 §12)

```relang
// Convenience options for content negotiation
type HttpOptions {
    // ... other fields ...

    // Content negotiation
    accept: String?                 // e.g., "application/json"
    acceptLanguage: String?         // e.g., "en-US,en;q=0.9"
    acceptEncoding: String?         // e.g., "gzip, deflate" (auto-managed by default)
}
```

---

## Usage Examples

### Simple GET

```relang
let resp = (await http.get("https://api.example.com/users"))!
```

### GET with Headers and Auth

```relang
let resp = (await http.get("https://api.example.com/users", HttpOptions {
    headers: { "Accept": "application/json" },
    auth: Bearer { token: apiToken }
}))!
```

### POST JSON

```relang
let resp = (await http.post("https://api.example.com/users",
    JsonBody { value: { name: "Alice", email: "alice@example.com" } }
))!
```

### Parse JSON Response

```relang
let users: List<User> = resp.json()!
```

### Error Handling

```relang
match await http.get(url) {
    resp: HttpResponse -> process(resp)
    f: Failure -> match f.kind {
        af: ActionFailed -> match af.error {
            t: Timeout -> retryLater()
            s: HttpStatus -> match s.status {
                404 -> notFound()
                429 -> rateLimited()
                _ -> serverError(s.status)
            }
            _ -> reportError(f)
        }
        _ -> reportError(f)
    }
}
```

### Parallel Requests

```relang
let (users & orders & products) = (await (
    http.get(usersUrl) and
    http.get(ordersUrl) and
    http.get(productsUrl)
))!
```

### Racing Endpoints

```relang
let data = (await (
    http.get(primaryUrl) or
    http.get(fallbackUrl)
))!
```

### Timeout Pattern (v0.1 Core)

```relang
let winner = await (http.get(url) or timer(5s))
match winner {
    resp: HttpResponse -> process(resp)
    _ -> timeoutExceeded()
}
```

### Idempotency Key

```relang
let resp = (await http.post(url, body, HttpOptions {
    idempotencyKey: "order-${orderId}"
}))!
```

### Conditional Requests (Caching)

```relang
// Cache validation with ETag
let resp = await http.get(url, HttpOptions {
    ifNoneMatch: cachedEtag
})

match resp {
    r: HttpResponse -> updateCache(r)
    f: Failure -> match f.kind {
        af: ActionFailed -> match af.error {
            s: HttpStatus -> if s.status == 304 { useCachedVersion() } else { handleError(f) }
            _ -> handleError(f)
        }
        _ -> handleError(f)
    }
}

// Optimistic concurrency with If-Match
let resp = await http.put(url, updatedData, HttpOptions {
    ifMatch: knownEtag
})

match resp {
    r: HttpResponse -> success()
    f: Failure -> match f.kind {
        af: ActionFailed -> match af.error {
            s: HttpStatus -> if s.status == 412 { conflictDetected() } else { handleError(f) }
            _ -> handleError(f)
        }
        _ -> handleError(f)
    }
}
```

### OPTIONS Request (CORS)

```relang
// Check allowed methods
let resp = (await http.options(url))!
let allowedMethods = resp.header("Allow")
```

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Non-2xx as `HttpStatus` error | Workflows typically handle failures explicitly; success path stays clean |
| `http.request()` for raw access | Escape hatch when any status is acceptable |
| Structured `HttpBody` | Type-safe, serializable, no stringly-typed content-type |
| `HttpAuth` as explicit type | Prevents credential leakage via raw headers |
| Output modes | Flexibility; choose detail level needed |
| No streaming | Orchestrator scope; streaming belongs in action executors |
| Case-insensitive headers | RFC 9110 compliance; prevents subtle bugs |
| Multi-value response headers | RFC 9110 compliance; Set-Cookie, etc. |
| Conditional request support | Efficient caching, optimistic concurrency |
| TRACE/CONNECT omitted | Security (TRACE), tunneling complexity (CONNECT) |

---

## RFC Compliance Notes

| RFC | Feature | Status |
|-----|---------|--------|
| RFC 9110 | HTTP Semantics | ✅ Methods, status codes, headers |
| RFC 9110 §5.1 | Case-insensitive headers | ✅ HttpHeaders type |
| RFC 9110 §5.3 | Multi-value headers | ✅ `List<String>` in response |
| RFC 9110 §12 | Content negotiation | ✅ Accept options |
| RFC 9110 §13 | Conditional requests | ✅ If-None-Match, If-Match |
| RFC 9110 §15.4 | Redirects | ✅ RedirectMode enum |
| RFC 9111 | Caching | ✅ CacheMode enum |
| RFC 7235 | Authentication | ✅ HttpAuth types |

**Intentionally omitted:**
- TRACE method (security risk, rarely used)
- CONNECT method (tunneling, out of orchestrator scope)
- Streaming responses (SSE, chunked) — out of scope

---

## Comparison with Serverless Workflow

| Feature | Serverless Workflow | ReLang |
|---------|---------------------|--------|
| Method | `method` property | Verb methods (`get`, `post`, etc.) |
| URL | `endpoint` | First argument |
| Headers | `headers` map | `headers` in options |
| Query params | `query` map | `query` in options |
| Body | `body` any | Typed `HttpBody` |
| Auth | `endpoint.authentication` | `auth` in options |
| Output modes | `output: raw/content/response` | `output: Raw/Content/Response` |
| Redirect | `redirect: boolean` | `followRedirects` in options |
| Non-2xx handling | Error by default | Error by default |

---

*End of HTTP action family*

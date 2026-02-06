# OpenAPI Action Family

*Generated typed clients from OpenAPI specifications*

---

## Overview

The `openapi` family generates typed clients from OpenAPI (Swagger) specifications at compile time.

**Scope:**
- Full type safety from spec
- Named operations become methods
- Request/response types generated
- Validation based on spec constraints

**v0.1 profile note**: `timeout(...)`/`retry(...)` are not language primitives. Timeout patterns use `timer(...)` + `or`; retries are runtime policy concerns.

---

## Error Types

```relang
sealed OpenApiError

type Timeout : OpenApiError {
    duration: Duration
}

type ConnectionFailed : OpenApiError {
    url: String
    message: String?
}

type ValidationError : OpenApiError {
    field: String
    message: String
    value: any?
}

type ApiError : OpenApiError {
    status: Int
    code: String?
    message: String?
    details: any?
}

type Cancelled : OpenApiError {}
```

---

## Service Definition Pattern

OpenAPI specs generate typed action families.

### OpenAPI Specification

```yaml
# petstore.yaml
openapi: 3.0.0
info:
  title: Petstore API
  version: 1.0.0
paths:
  /pets:
    get:
      operationId: listPets
      parameters:
        - name: limit
          in: query
          schema:
            type: integer
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PetList'
    post:
      operationId: createPet
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/NewPet'
      responses:
        '201':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Pet'
  /pets/{petId}:
    get:
      operationId: getPet
      parameters:
        - name: petId
          in: path
          required: true
          schema:
            type: string
```

### Generated ReLang Client

```relang
// Auto-generated from petstore.yaml
action family petstore : openapi {
    errors: OpenApiError
    baseUrl: "https://api.petstore.com/v1"

    fn listPets(limit: Int?): *PetList
    fn createPet(body: NewPet): *Pet
    fn getPet(petId: String): *Pet
}

// Generated types
type Pet {
    id: String
    name: String
    status: PetStatus
}

type NewPet {
    name: String
    status: PetStatus?
}

type PetList {
    pets: List<Pet>
    nextCursor: String?
}

enum PetStatus {
    AVAILABLE
    PENDING
    SOLD
}
```

---

## Core API

### Generated Client Usage

```relang
let pets = await petstore.listPets(limit: 10)!
let pet = await petstore.getPet(petId: "abc123")!
let newPet = await petstore.createPet(NewPet { name: "Fluffy" })!
```

### Options Type

```relang
type OpenApiOptions {
    baseUrl: String?
    headers: Json?
    auth: HttpAuth?
    output: HttpOutput?           // Raw, Content, or Response
    followRedirects: Bool?        // Default: true
}

enum HttpOutput {
    Raw         // Bytes only
    Content     // Parsed body (default)
    Response    // Full response with headers
}

// With options
let pet = await petstore.getPet(
    petId: "abc123",
    options: OpenApiOptions {
        auth: Bearer { token: apiToken }
    }
)!
```

---

## Project Configuration

### Multiple Specs

```yaml
# relang.yaml
openapi:
  petstore: "./specs/petstore.yaml"
  payments: "./specs/payments.yaml"
  inventory: "./specs/inventory.yaml"
```

### Usage

```relang
let pet = await petstore.getPet(petId)!
let charge = await payments.createCharge(amount: 1000)!
let stock = await inventory.checkStock(productId)!
```

---

## Usage Examples

### Simple Calls

```relang
let pets = await petstore.listPets(limit: 10)!

for pet in pets.pets {
    print("${pet.name}: ${pet.status}")
}
```

### Create Resource

```relang
let newPet = await petstore.createPet(NewPet {
    name: "Fluffy",
    status: PetStatus.AVAILABLE
})!

print("Created pet with id: ${newPet.id}")
```

### Error Handling

```relang
match await petstore.getPet(petId: "unknown") {
    pet: Pet -> processPet(pet)
    f: Failure -> match f.kind {
        af: ActionFailed -> match af.error {
            e: ApiError -> match e.status {
                404 -> petNotFound()
                403 -> accessDenied()
                _ -> serverError(e)
            }
            v: ValidationError -> badRequest(v.field, v.message)
            _ -> reportError(f)
        }
        _ -> reportError(f)
    }
}
```

### Parallel Calls

```relang
let (pet & owner & vaccinations) = (await (
    petstore.getPet(petId: id) and
    petstore.getOwner(petId: id) and
    petstore.getVaccinations(petId: id)
))!
```

### Timeout Pattern (v0.1 Core)

```relang
let winner = await (petstore.getPet(petId: id) or timer(5s))
match winner {
    pet: Pet -> processPet(pet)
    _ -> timeoutExceeded()
}
```

### Cross-Service Coordination

```relang
// Coordinate multiple OpenAPI services
let (pet & inventory & shipping) = (await (
    petstore.getPet(petId: id) and
    inventory.checkStock(productId: petFoodId) and
    shipping.estimateDelivery(address: customerAddress)
))!
```

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Compile-time generation | Full type safety, IDE support, catch errors early |
| Operation IDs as method names | Natural API, matches spec convention |
| `ApiError` for non-2xx | Structured error body available |
| `ValidationError` separate | Client-side validation distinct from server errors |
| Spec-defined types generated | No manual type maintenance |

---

## Comparison with Serverless Workflow

| Feature | Serverless Workflow | ReLang |
|---------|---------------------|--------|
| Spec loading | `document.endpoint` (runtime) | Compile-time code generation |
| Operation call | `operationId` + `parameters` | Typed method with parameters |
| Authentication | `authentication` property | `auth` in options |
| Output modes | `output: raw/content/response` | Output modes (similar) |
| Redirect handling | `redirect: boolean` | `followRedirects` in options |

**Key difference**: ReLang uses compile-time code generation from OpenAPI spec, providing full type safety and IDE support. Serverless Workflow loads specs at runtime.

---

*End of OpenAPI action family*

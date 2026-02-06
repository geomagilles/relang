# ReLang Primitive Values (v0.1 profile)

*Types de base et litteraux pour workflows durables*

---

## 1. Overview

Les primitives v0.1 sont des valeurs:

1. immuables,
2. serializables,
3. compatibles snapshot/resume.

Types noyau:

- `Int`
- `Float`
- `Bool`
- `String`
- `Bytes`
- `Duration`
- `Timestamp`
- `Json`
- `Unit`
- `Failure`

Les optionnels sont traites a part via `T?` + `none` (surface langage).

---

## 2. Numeric types

### 2.1 `Int`

Entier signe 64-bit.

```relang
let a: Int = 42
let b: Int = -17
let c: Int = 1_000_000
```

Operations usuelles: `+`, `-`, `*`, `/`, `%`, comparaisons.

### 2.2 `Float`

Flottant IEEE 754 64-bit.

```relang
let x: Float = 3.14
let y: Float = -0.5
let z: Float = 1.0e6
```

Implicit widening: `Int` is promoted to `Float` in mixed-type operations (result is `Float`). Narrowing `Float` -> `Int` requires explicit `as`.

---

## 3. Scalars

### 3.1 `Bool`

```relang
let ok: Bool = true
let ko: Bool = false
```

### 3.2 `String`

```relang
let s: String = "hello"
let msg: String = "order-${orderId}"
```

### 3.3 `Bytes`

```relang
let payload: Bytes = b"\x01\x02\x03"
```

---

## 4. Time primitives

### 4.1 `Duration`

```relang
let d1: Duration = 5s
let d2: Duration = 200ms
let d3: Duration = 2h
```

### 4.2 `Timestamp`

```relang
let t: Timestamp = now()
let deadline: Timestamp = now() + 30s
```

v0.1:

1. `now()` lit l'horloge UTC courante de la machine d'execution,
2. les valeurs capturees avant checkpoint sont restaurees apres resume.

---

## 5. `Json`

`Json` represente des donnees dynamiques serializables.

```relang
let obj: Json = { "id": "u-1", "active": true }
let arr: Json = [1, 2, 3]
```

Usage recommande:

1. interfaces externes (HTTP/OpenAPI),
2. metadata flexible,
3. eviter d'en faire un substitut systematique aux types domaine.

---

## 6. `Unit`, optionnels et absence

### 6.1 `Unit`

`Unit` represente "pas de valeur metier utile".

```relang
fn log(msg: String): Unit {
    print(msg)
}
```

### 6.2 Optionnels en surface

L'absence se modelise avec `T?` et `none`:

```relang
let email: String? = none
```

`Some/None` ne sont pas exposes en surface langage v0.1.

---

## 7. `Failure` (type noyau)

`Failure` est le canal d'echec standard des effets awaites.

```relang
match await http.get(url) {
    r: HttpResponse -> use(r)
    f: Failure -> handleFailure(f)
}
```

Le detail structurel de `Failure` est defini dans:

1. `relang-failures-proposal.md`
2. `relang-spec-v0.1-canonique.md`

---

## 8. Durabilite

Tous les primitifs ci-dessus sont snapshot-safe.

- les valeurs sont capturees dans les checkpoints,
- restaurees a l'identique au resume,
- sans replay applicatif explicite du code deja franchi.

---

## 9. Resume

- Primitives v0.1: `Int`, `Float`, `Bool`, `String`, `Bytes`, `Duration`, `Timestamp`, `Json`, `Unit`, `Failure`.
- Optionnels: `T?` + `none`.
- Horloge `now()`: naturelle (wall-clock UTC), compatible snapshots.

---

*End of v0.1 profile*

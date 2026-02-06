# ReLang Signals (v0.1 profile)

*Messages externes vers une execution durable*

---

## 1. Overview

Les signaux permettent d'injecter des evenements externes dans une execution.

Exemples d'usage:

1. validation humaine,
2. mise a jour d'etat,
3. interruption metier.

En v0.1, la reception d'un signal est un effet explicite:

```relang
receive<T>(): *T
```

---

## 2. Declaration

```relang
signal Approval {
    approved: Bool
    reviewer: String
}

signal Cancel {}
```

---

## 3. Reception

### 3.1 Wait d'un type de signal

```relang
let approvalTask = receive<Approval>()
let approval = (await approvalTask)!
```

### 3.2 Handling

```relang
match await receive<Approval>() {
    a: Approval -> {
        if a.approved { continueFlow() } else { rejectFlow() }
    }
    f: Failure -> handleFailure(f)
}
```

---

## 4. Timeout pattern v0.1

Pas de `receive(..., timeout: d)` en primitive noyau.

Pattern canonique:

```relang
let winner = await (receive<Approval>() or timer(24h))

match winner {
    a: Approval -> onApproval(a)
    _ -> onApprovalTimeout()
}
```

---

## 5. Propagation des failures

1. signal awaite inline: propagation normale de la `Failure`,
2. frontiere `spawn`: parent observe `FunctionFailed(cause=...)`.

Le matching detaille suit `Failure.kind`.

---

## 6. Durabilite

1. `receive<T>()` retourne un awaitable durable,
2. suspension sur `await receive` -> checkpoint,
3. reprise depuis snapshot sans casser le contrat `T | Failure`.

---

## 7. Resume

- signaux = messages externes modelises comme `*T`
- reception explicite avec `await`
- timeout via `timer(...)` + `or`

---

*End of v0.1 profile*

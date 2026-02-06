# ReLang Awaitables (v0.1 profile)

*Effets, coordination, et resolution explicite*

---

## 1. Overview

Un awaitable represente un effet en cours.

- Type awaitable: `*T`
- Resolution explicite: `await e`
- Typage de base: `await e : T | Failure`

Ce document est aligne avec:

1. `relang-spec-v0.1-canonique.md`
2. `relang-failures-proposal.md`

---

## 2. Model de base

### 2.1 Creation

Les effets externes produisent des awaitables:

```relang
let a: *HttpResponse = http.get(url)
let b: *User = spawn loadUser(userId)
let c: *Timestamp = timer(5s)
let d: *Approval = receive<Approval>()
```

Semantique v0.1:

1. demarrage logique eager a la creation,
2. le runtime peut differer le demarrage physique (quotas/scheduling),
3. la forme observable reste identique pour le programme.

### 2.2 Resolution

```relang
let r: HttpResponse | Failure = await a
```

Sucre de propagation:

```relang
let r: HttpResponse = (await a)!
```

`(await x)!` retourne la valeur en succes, sinon propage la `Failure`.

---

## 3. Identite

Awaitables primitifs (action/timer/receive/spawn) exposent une identite stable:

```relang
let t = http.get(url)
let id = t.id
let createdAt = t.createdAt
```

Awaitables composes via `and`/`or` n'ont pas d'identite propre.

---

## 4. Coordination

### 4.1 `and`

```relang
*A and *B : *(A & B)
```

Semantique:

1. tous doivent reussir,
2. echec rapide au premier `Failure`,
3. annulation best-effort des branches restantes.

Exemple:

```relang
let joined = t1 and t2 and t3
let (a & b & c) = (await joined)!
```

### 4.2 `or`

```relang
*A or *B : *(A | B)
```

Semantique:

1. premier succes gagne,
2. si toutes les branches echouent: `Failure.kind = AllFailed`,
3. annulation best-effort des perdants.

Exemple:

```relang
let raced = primary or fallback
let result = await raced

match result {
    v: Data -> use(v)
    f: Failure -> match f.kind {
        all: AllFailed -> handleAllFailed(all)
        _ -> handleFailure(f)
    }
}
```

---

## 5. Timeout et retry en v0.1

### 5.1 Timeout

`timeout(...)` n'est pas une primitive langage v0.1.

Pattern canonique:

```relang
let winner = await (http.get(url) or timer(5s))
match winner {
    r: HttpResponse -> use(r)
    _ -> onTimeout()
}
```

### 5.2 Retry

`retry(...)` n'est pas une primitive langage v0.1.

Les retries relevent d'une politique runtime/action family.

---

## 6. Failures

Le contrat langage est `Failure` (enveloppe causale):

1. classification via `Failure.kind`,
2. cause unique via `cause`,
3. causes multiples via `causes`.

Exemple sur action:

```relang
match await http.get(url) {
    r: HttpResponse -> ok(r)
    f: Failure -> match f.kind {
        af: ActionFailed -> match af.error {
            s: HttpStatus -> handleStatus(s.status)
            _ -> handleFailure(f)
        }
        _ -> handleFailure(f)
    }
}
```

Propagation:

1. inline `f(...)`: meme `Failure` propagee,
2. distribue `await spawn f(...)`: wrapper `FunctionFailed(cause=...)`.

---

## 7. Durabilite et snapshots

1. Un `await` qui suspend cree un checkpoint.
2. Apres reprise, les effets deja resolus sont reutilises (pas de re-emission).
3. Les variables locales et la table des awaitables sont restaurees depuis snapshot.

---

## 8. Resume

- `*T` = handle d'effet
- `await` = frontiere explicite de resolution
- `and`/`or` = coordination typed
- `timeout` langage: non (composition via `timer`)
- `retry` langage: non (policy runtime)

---

*End of v0.1 profile*

# ReLang Awaitable Lifecycle (v0.1 profile)

*Introspection minimale des awaitables dans un modele snapshot-first*

---

## 1. Overview

Un awaitable passe par des etats de cycle de vie observables.

Objectif v0.1:

1. introspection simple et stable,
2. pas de changement du contrat de typage (`await : *T -> T | Failure`),
3. compatibilite resume/snapshot.

---

## 2. Etats

Etats conceptuels:

1. `Pending` (en cours),
2. `Succeeded` (resolu en succes),
3. `Failed` (resolu en echec).

`Succeeded` et `Failed` sont terminaux.

---

## 3. API d'introspection (lecture seule)

```relang
t.isResolved(): Bool
t.isSucceeded(): Bool
t.isFailed(): Bool

t.id: String
t.createdAt: Timestamp

t.resolvedAt(): Timestamp?
t.data(): T?
t.failure(): Failure?
```

Regles:

1. API en lecture seule,
2. aucun effet externe,
3. aucune mutation d'etat par introspection.

---

## 4. Semantique

### 4.1 Coherence locale

- avant resolution: `isResolved() == false`
- apres succes: `isResolved() == true`, `isSucceeded() == true`, `data() != none`
- apres echec: `isResolved() == true`, `isFailed() == true`, `failure() != none`

### 4.2 Relation avec `await`

`await t` reste la frontiere canonique pour consommer le resultat.

```relang
let r = await t
match r {
    v: User -> use(v)
    f: Failure -> handleFailure(f)
}
```

---

## 5. Resume et snapshots

1. l'etat de resolution est persiste dans les snapshots,
2. apres reprise, les awaitables deja resolus restent resolus,
3. `data()` / `failure()` restent coherents avec l'etat restaure.

---

## 6. Exemples

### 6.1 Polling non bloquant

```relang
let t = http.get(url)

if t.isResolved() {
    match await t {
        r: HttpResponse -> use(r)
        f: Failure -> handleFailure(f)
    }
} else {
    doOtherWork()
}
```

### 6.2 Diagnostic simple

```relang
let t = spawn processOrder(orderId)

if t.isFailed() {
    let f = t.failure()
    // correlation via failure.id et observabilite runtime
}
```

---

## 7. Frontiere langage/runtime

- contrat langage: `Failure` (portable)
- details machine (host, worker, pod, trace): runtime observability reliee par `failure.id`

---

## 8. Resume

- lifecycle = introspection minimale lecture seule
- `await` reste l'API principale de consommation
- etats et resultats restores via snapshots

---

*End of v0.1 profile*

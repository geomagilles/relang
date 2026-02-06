# ReLang Timers (v0.1 profile)

*Awaitables temporels pour delais et echeances*

---

## 1. Overview

Les timers sont des awaitables temporels:

```relang
timer(d: Duration): *Timestamp
timer(at: Timestamp): *Timestamp
```

- creation explicite,
- resolution via `await`,
- durables via snapshots.

---

## 2. Semantique

### 2.1 Horloge

v0.1 utilise une horloge naturelle:

1. `now()` lit le temps UTC courant de la machine d'execution,
2. pas de modele replay-first base sur une horloge virtuelle,
3. les valeurs capturees avant checkpoint sont restaurees telles quelles.

### 2.2 Echeance persistee

A la creation d'un timer, le runtime persiste une echeance absolue (`fireAt`).

- `timer(5s)` -> `fireAt = now() + 5s`
- `timer(at)` -> `fireAt = at`

Au resume, le runtime reprend a partir de `fireAt` persiste.

---

## 3. Usage

### 3.1 Delai simple

```relang
let t = timer(5s)
let firedAt = (await t)!
```

### 3.2 Echeance absolue

```relang
let deadline = now() + 1h
(await timer(deadline))!
```

### 3.3 Parallelisme avec travail

```relang
let pause = timer(30s)
let work = http.get(url)

let winner = await (work or pause)
match winner {
    r: HttpResponse -> use(r)
    _ -> onTimeout()
}
```

---

## 4. Timeout pattern v0.1

`timeout(task, d)` n'est pas un primitif noyau.

Pattern canonique:

```relang
let winner = await (task or timer(d))
```

La branche gagnante donne le resultat de `task` ou l'evenement de timer.

---

## 5. Failures et cancellation

Un timer peut echouer/cancel selon runtime.

Gestion canonique:

```relang
match await timer(10s) {
    ts: Timestamp -> onFire(ts)
    f: Failure -> handleFailure(f)
}
```

La forme `Failure` suit le modele causal v0.1.

---

## 6. Durabilite

1. creation du timer: handle awaitable durable,
2. suspension sur `await` non resolu: checkpoint,
3. resume: reprise depuis snapshot + `fireAt` persiste.

---

## 7. Resume

- timers = effets temporels `*Timestamp`
- horloge `now()` naturelle
- pas de primitive `timeout(...)`
- timeout par composition `task or timer(...)`

---

*End of v0.1 profile*

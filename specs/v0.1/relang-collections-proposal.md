# ReLang Collections (v0.1 profile)

*Listes immuables et operations de base pour workflows durables*

---

## 1. Overview

v0.1 definit une collection principale: la liste homogene `[T]`.

Proprietes:

1. ordonnee,
2. immutable,
3. serializable,
4. compatible snapshot/resume.

---

## 2. Syntaxe

### 2.1 Types

```relang
[Int]
[String]
[User]
[Int | String]
```

### 2.2 Literaux

```relang
[]
[1, 2, 3]
["a", "b"]
```

---

## 3. Operations de base

### 3.1 Taille et vide

```relang
items.length()
items.isEmpty()
```

### 3.2 Acces par index

```relang
let first = items[0]
let last = items[-1]
```

Le comportement hors borne est runtime-defined; en pratique, preferer des patterns defensifs (`isEmpty`, bornes explicites).

### 3.3 Construction immutable

```relang
let xs = [1, 2, 3]
let ys = xs.append(4)     // xs intacte, ys nouvelle liste
let zs = ys.concat([5, 6])
```

---

## 4. Transformations

```relang
let doubled = xs.map { x -> x * 2 }
let positives = xs.filter { x -> x > 0 }
let total = xs.reduce(0) { acc, x -> acc + x }
let exists = xs.any { x -> x == target }
let allOk = xs.all { x -> x >= 0 }
```

Les lambdas restent inline-only (pas de function type first-class en v0.1).

---

## 5. Recherche

```relang
let i: Int? = xs.indexOf(target)
let found: Int? = xs.find { x -> x > 10 }
```

Convention v0.1:

1. resultats potentiellement absents exposes en `T?`,
2. absence en surface = `none`.

---

## 6. Tri et ordre

```relang
let sorted = xs.sort()
let reversed = xs.reverse()
```

Pour des types complexes:

```relang
let byAge = users.sortBy { u -> u.age }
```

---

## 7. Destructuration et boucle

```relang
for x in xs {
    process(x)
}

for (i & x) in xs.enumerate() {
    log("${i}: ${x}")
}
```

Les loops restent des statements; toute suspension (`await` bloquant) cree un checkpoint.

---

## 8. Collections et awaitables

Pattern courant:

```relang
let tasks = urls.map { u -> http.get(u) }   // [*HttpResponse]
let results = (await and(tasks))!
```

- coordination explicite via `and`/`or`,
- pas de primitive `retry(...)`/`timeout(...)` dans le noyau.

---

## 9. Durabilite

Les listes et leurs elements serializables sont captures dans les snapshots.

Apres resume:

1. contenu restaure a l'identique,
2. etapes deja resolues reutilisees,
3. l'etat des variables liste reste coherent.

---

## 10. Resume

- collection noyau: `[T]`
- operations immuables (`map`, `filter`, `reduce`, etc.)
- absence en surface: `T?` + `none`
- compatible execution snapshot-first

---

*End of v0.1 profile*

# ReLang Variables and Bindings (v0.1 profile)

*Declarations locales, reassignment, et immutabilite des valeurs*

---

## 1. Overview

En v0.1:

1. les variables locales se declarent avec `let`,
2. le nom peut etre reassigne (`x = ...`) si le type reste compatible,
3. les valeurs de types utilisateur sont immuables (pas de mutation de champ).

---

## 2. Declarations

### 2.1 Declaration simple

```relang
let x = 10
let name: String = "Alice"
```

### 2.2 Declaration avec optionnel

```relang
let email: String? = none
```

---

## 3. Reassignment

La reassignment locale est autorisee:

```relang
let count = 0
count = count + 1
```

Contrainte:

1. le type statique de la variable ne change pas.

```relang
let n: Int = 1
n = 2        // OK
// n = "x"  // erreur de type
```

---

## 4. Shadowing

Un nouveau `let` dans un scope interne cree une nouvelle variable:

```relang
let x = 10

if x > 0 {
    let x = x + 1   // shadowing
    print(x)        // 11
}

print(x)            // 10
```

---

## 5. Valeurs immuables

Les types utilisateur sont immuables en v0.1.

```relang
type User {
    id: String
    name: String
}

let u = User { id: "u1", name: "Alice" }
// u.name = "Bob"   // interdit en v0.1
```

Mise a jour par reconstruction:

```relang
let u2 = User { id: u.id, name: "Bob" }
```

---

## 6. Patterns avec `let`

`let` accepte des patterns de decomposition.

```relang
let (a & b) = (1 & "ok")
```

Sur unions, utiliser `match` pour brancher de maniere sure.

---

## 7. Variables et await

Les resultats d'effets attendus sont typiquement lies a des variables:

```relang
let task = http.get(url)
let result = await task

match result {
    r: HttpResponse -> use(r)
    f: Failure -> handleFailure(f)
}
```

Sucre courant:

```relang
let resp = (await http.get(url))!
```

---

## 8. Scope et resume

1. le scope lexical determine la visibilite des noms,
2. les variables locales serializables sont capturees au checkpoint,
3. apres reprise, elles sont restaurees depuis snapshot.

---

## 9. Bonnes pratiques

1. preferer des noms stables et explicites,
2. limiter le shadowing aux blocs courts,
3. reconstruire les valeurs plutot que simuler de la mutation d'objet,
4. garder des variables snapshot-safe (serializables).

---

## 10. Resume

- `let` declare des variables locales,
- reassignment locale autorisee (type stable),
- pas de mutation de champs en v0.1,
- variables restaurees par snapshot/resume.

---

*End of v0.1 profile*

# ReLang Type System (v0.1 profile)

*Types utilisateur immuables pour execution durable*

---

## 1. Overview

Le systeme de types v0.1 vise:

1. serialisation fiable des etats,
2. simplicite de matching et de propagation,
3. compatibilite snapshot/resume.

Principes:

- types utilisateur immuables,
- pas d'heritage general,
- `sealed` pour ensembles exhaustifs,
- optionnels en surface: `T?` + `none`.

---

## 2. Types utilisateur

### 2.1 Records nominaux (`type`)

```relang
type User {
    id: String
    email: String?
}
```

Regles v0.1:

1. fields typees,
2. valeurs serializables,
3. pas de mutation de champ (`x.field = ...` interdit).

### 2.2 Groupes scelles (`sealed`)

```relang
sealed PaymentStatus

type Authorized : PaymentStatus { authId: String }
type Declined : PaymentStatus { reason: String }
```

`sealed` sert a l'exhaustivite de `match`.

---

## 3. Optionnels

Surface langage:

```relang
let email: String? = none
```

- `T?` = presence ou absence,
- valeur d'absence en surface: `none`,
- `Some/None` ne sont pas exposes en surface v0.1.

---

## 4. Produits et unions

```relang
// Produit
type Pair = Int & String
let p = (42 & "ok")

// Union
let v: Int | String = 42
```

- produit: composition conjointe,
- union: alternatives de valeur.

---

## 5. Equality structurale

Les types utilisateur se comparent structurellement (par contenu):

```relang
type Point { x: Int, y: Int }

Point { x: 1, y: 2 } == Point { x: 1, y: 2 }   // true
Point { x: 1, y: 2 } == Point { x: 1, y: 3 }   // false
```

---

## 6. Types et Failure

`Failure` est un type noyau specifique (enveloppe causale) documente dans:

1. `relang-failures-proposal.md`
2. `relang-spec-v0.1-canonique.md`

Les erreurs metier restent des types domaine (`Declined`, etc.), distinctes de `Failure`.

---

## 7. Contraintes de serialisation

Toute valeur qui traverse un checkpoint doit etre serializable.

Implications:

1. pas de references non serializables,
2. pas de fermeture/fonction first-class en v0.1,
3. structures de donnees explicites et stables.

---

## 8. Evolution de schema

Pour evoluer des types sans casser le resume:

1. conserver compatibilite de champs captures,
2. utiliser version d'etat/migration cote runtime quand necessaire.

---

## 9. Resume

- `type` immutable, data-first
- `sealed` pour alternatives exhaustives
- optionnels: `T?` + `none`
- separation stricte erreurs metier vs `Failure`

---

*End of v0.1 profile*

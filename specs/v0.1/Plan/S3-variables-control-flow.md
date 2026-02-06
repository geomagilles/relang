# S3 - Variables, scope, controle de flux de base

## Objectif

Stabiliser les regles de binding/portee et le controle de flux necessaire au code metier.

## Perimetre

IN:

- `let`, reassignment locale type-safe, shadowing.
- `if` expression, `match` minimal.
- Boucles de base (`for`, `while`) sans features avancees.

OUT:

- Exhaustivite complexe des sealed.
- Smart casts avances.
- Awaitables dans boucles (arrive plus tard).

## References spec

- relang-variables-proposal.md
- relang-conditionals-proposal.md
- relang-loops-proposal.md

## Sequence d'implementation

1. Mapper les variables vers `FrameDescriptor` / slots Truffle.
2. Implementer scopes lexicaux imbriques.
3. Implementer reassignment avec verification de type statique stable.
4. Implementer shadowing explicite par scope.
5. Implementer `if` comme expression:
   - branches compatibles,
   - branche else implicite type `Unit` si usage statement.
6. Implementer `match` minimal:
   - literals,
   - wildcard `_`,
   - `none`.
7. Implementer boucles:
   - `for ... in ...`,
   - `while Bool`,
   - `break`, `continue`.
8. Ajouter diagnostics:
   - variable inconnue,
   - out-of-scope,
   - type mismatch sur reassignment.

## Livrables

- Binding engine stable.
- Noeuds AST de controle de flux.
- Tests comportementaux sur scope.

## Tests obligatoires

- shadowing ne fuit pas hors bloc.
- reassignment respecte type initial.
- `match` avec `_` fonctionne.
- `while` refuse condition non bool.
- `break/continue` hors boucle -> erreur compile.

## Risques et garde-fous

Risque: etats de frame incoherents en boucles.

Garde-fou:

- tests avec boucles imbriquees et shadowing meme nom.

Risque: `match` ambigu.

Garde-fou:

- priorite d'evaluation d'armes documentee et testee.

## Definition of Done

- Scope/variables deterministes.
- Controle de flux de base stable.
- Erreurs compile-time precises.


## Gate governance de fin de sprint

Obligatoire avant cloture:

- Rapport spec-delta: `Governance/04-spec-delta-review.md`.
- Mise a jour conformance matrix (statut des requirements touchees).
- Validation des risques ouverts (acceptes/replanifies/corriges).

## Gate supplementaire S3 (conformance matrix)

- Creer `conformance-matrix-v0.1.csv` selon `Governance/02-conformance-matrix.md`.
- Relier chaque requirement implementee a au moins un test.
- Integrer un check CI qui echoue si un `MUST` touche n'a pas de statut.

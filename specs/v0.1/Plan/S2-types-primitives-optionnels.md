# S2 - Types primitifs et optionnels

## Objectif

Livrer un systeme de types de base fiable pour eviter les erreurs structurelles sur les sprints suivants.

## Perimetre

IN:

- `Int`, `Float`, `Bool`, `String`, `Bytes`, `Duration`, `Timestamp`, `Json`, `Unit`.
- `T?` et `none`.
- Operateurs primitifs de base.
- Erreurs de typage fondamentales.

OUT:

- Unions/produits avances.
- Type narrowing complet.
- Awaitables.

## References spec

- relang-spec-v0.1-canonique.md (section types).
- relang-primitives-proposal.md.
- relang-null-safety-proposal.md.

## Sequence d'implementation

1. Definir le modele interne des types (`RelangType`).
2. Ajouter `OptionalType(innerType)`.
3. Encoder `none` comme valeur langage distincte.
4. Implementer verifications:
   - assignation stricte,
   - pas de coercion implicite `Int <-> Float`.
5. Implementer operateurs:
   - arithmetique base,
   - comparaison meme type,
   - erreurs cross-type.
6. Implementer checker `if` condition must be `Bool`.
7. Ajouter diagnostics detailles (message + position + suggestion).
8. Ajouter tests de non-regression sur null safety.

## Livrables

- Type checker primitif stable.
- Runtime values pour primitives et `none`.
- Messages d'erreur exploitables.

## Tests obligatoires

Positifs:

- declarations primitives valides.
- `String?` avec `none`.
- `??` (si active en S2) ou placeholder bloque explicitement.

Negatifs:

- assigner `none` a type non-optionnel.
- comparaison cross-type sans conversion.
- condition `if` non bool.

## Risques et garde-fous

Risque: confusion `Unit` vs `none`.

Garde-fou:

- tests dedies sur semantics distinctes.

Risque: conversions implicites introduites accidentellement.

Garde-fou:

- tests stricts de refus de coercion implicite.

## Definition of Done

- Tous les cas primitifs spec S2 passes.
- Erreurs de type stables et lisibles.
- Aucun comportement implicite non specifie.


## Gate governance de fin de sprint

Obligatoire avant cloture:

- Rapport spec-delta: `Governance/04-spec-delta-review.md`.
- Mise a jour conformance matrix (statut des requirements touchees).
- Validation des risques ouverts (acceptes/replanifies/corriges).

## Gate supplementaire S2 (runtime-contract)

- Publier `runtime-contract-v1` selon `Governance/01-runtime-contract.md`.
- Ajouter tests de compatibilite de schema de base (lecture/ecriture).
- Bloquer toute evolution contractuelle sans version explicite.

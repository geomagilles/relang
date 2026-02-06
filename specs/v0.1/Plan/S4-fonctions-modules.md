# S4 - Fonctions, appels inline et modules

## Objectif

Permettre la composition de code reelle: fonctions top-level, appels inline, imports/modules et contraintes de portabilite.

## Perimetre

IN:

- `fn` top-level.
- Appel inline `f(...)`.
- Parametres nommes + valeurs par defaut.
- Modules/imports.
- Interdiction capture de scope par fonctions nommees.

OUT:

- `spawn`.
- lambdas avancees.
- fonction first-class (hors scope v0.1).

## References spec

- relang-functions.md
- relang-modules-proposal.md
- relang-spec-v0.1-canonique.md (sections fonctions/modules)

## Sequence d'implementation

1. Implementer AST declarations `FnDecl` top-level.
2. Implementer resolution de symboles de fonction par module.
3. Implementer appels inline + evaluation arguments:
   - positionnels,
   - nommes,
   - defaults.
4. Interdire:
   - fonctions imbriquees,
   - capture de variable externe.
5. Implementer imports:
   - import item,
   - import namespace,
   - alias.
6. Implementer detection cycles inter-modules.
7. Ajouter regles de visibilite:
   - `private` module-local.
8. Ajouter hoisting de fonctions dans module.

## Livrables

- Moteur de fonctions inline stable.
- Resolution de modules/imports.
- Contrainte self-contained enforcee.

## Tests obligatoires

- appel avant declaration (hoisting) valide.
- capture de variable externe -> erreur compile.
- cycle module A<->B detecte.
- parametres nommes melanges invalides rejetes.
- fonction nested rejetee.

## Risques et garde-fous

Risque: resolution de symboles non deterministe.

Garde-fou:

- ordre de resolution specifie:
  1. local,
  2. imports explicites,
  3. namespace.

Risque: dette de compatibilite pour `spawn`.

Garde-fou:

- API d'appel preparee pour mode inline/distribue.

## Definition of Done

- Fonctions inline production-ready.
- Modules/imports robustes.
- Regles v0.1 de portabilite respectees.


## Gate governance de fin de sprint

Obligatoire avant cloture:

- Rapport spec-delta: `Governance/04-spec-delta-review.md`.
- Mise a jour conformance matrix (statut des requirements touchees).
- Validation des risques ouverts (acceptes/replanifies/corriges).

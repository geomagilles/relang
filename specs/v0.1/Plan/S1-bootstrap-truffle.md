# S1 - Bootstrap Truffle et pipeline minimal

## Objectif

Poser un socle executable et testable pour le langage, sans dette de structure qui bloquera les phases suivantes.

## Perimetre

IN:

- `TruffleLanguage` + `Context`.
- Parse minimal d'un module.
- `RootNode` executable.
- Harness de tests.
- CI locale basique (build + test).

OUT:

- Typage avance.
- Awaitables.
- Snapshots.
- Actions externes.

## Pre-requis

- JDK/GraalVM version verrouillee.
- Build system configure (Gradle/Maven).
- Convention package et dossiers fixee.

## Sequence d'implementation (ordre strict)

1. Creer le module runtime langage.
2. Implementer la classe `RelangLanguage extends TruffleLanguage<RelangContext>`.
3. Implementer `RelangContext` (config minimale, services mockables).
4. Definir `RelangRootNode` et un `EvalRootNode` de test.
5. Ajouter un parser temporaire minimal (retourne AST constant).
6. Brancher `parse(...)` -> `CallTarget`.
7. Ajouter CLI dev minimale: `run/check`.
8. Ajouter tests smoke:
   - parse module vide,
   - execution d'une expression constante,
   - erreurs parse minimales.
9. Ajouter scripts CI locales (build + test + lint si present).

## Livrables

- Language bootstrap compilable.
- Suite smoke automatisable.
- Document d'architecture courte (1 page) sur separation parser/typer/runtime.

## Tests obligatoires

- `empty_module_executes`.
- `constant_expression_executes`.
- `invalid_source_returns_parse_error`.
- `context_created_once_per_execution`.

## Risques et garde-fous

Risque: melanger parse, typer et runtime trop tot.

Garde-fou:

- interfaces separees:
  - `Parser` -> AST brut,
  - `TypeChecker` -> AST annote,
  - `Executor` -> noeuds runtime.

Risque: tests fragiles relies a des details d'implementation.

Garde-fou:

- tester les comportements observables, pas les classes internes.

## Definition of Done

- Projet compile proprement.
- 100% tests S1 verts.
- `run` execute un programme trivial.
- Structure des modules validee par revue tech.


## Gate governance de fin de sprint

Obligatoire avant cloture:

- Rapport spec-delta: `Governance/04-spec-delta-review.md`.
- Mise a jour conformance matrix (statut des requirements touchees).
- Validation des risques ouverts (acceptes/replanifies/corriges).

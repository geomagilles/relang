# S7 - `spawn`, frontiere distribuee, versioning resume

## Objectif

Introduire la frontiere d'execution distribuee sans casser le contrat de durabilite.

## Perimetre

IN:

- `spawn f(...)` -> `*T`.
- Execution enfant avec `self.id`, `self.parentId`.
- Wrapping `FunctionFailed(cause=...)` cote parent.
- Politique `codeVersion` / `stateVersion` minimale.

OUT:

- scheduler production complexe multi-region.
- migration etendue multi-versions.

## References spec

- relang-spec-v0.1-canonique.md (spawn + versioning)
- relang-execution-model.md
- relang-failures-proposal.md

## Sequence d'implementation

1. Implementer plan de lancement enfant:
   - new execution id,
   - parentId renseigne,
   - args serializes.
2. Implementer scheduler local puis abstraction scheduler distribue.
3. Implementer `await spawn`:
   - succes enfant -> valeur,
   - echec enfant -> `FunctionFailed` avec `cause`.
4. Impl. cancellation best-effort des enfants losers dans `or`.
5. Ajouter metadata version:
   - `codeVersion` attachee a execution,
   - `stateVersion` snapshot.
6. Au resume:
   - meme `codeVersion` -> OK,
   - sinon refus explicite (tant que migration absente).
7. Ajouter hooks migration state (stubs) pour S8+.

## Livrables

- Pipeline parent/enfant execute et observable.
- Propagation cross-boundary conforme.
- Gate versioning active.

## Tests obligatoires

- parent spawn enfant succes.
- parent spawn enfant echec -> wrapper `FunctionFailed`.
- chain parent->child->grandchild preserve cause.
- resume refuse si `codeVersion` incompatible.

## Risques et garde-fous

Risque: confusion inline vs distribue.

Garde-fou:

- tests mirroirs:
  - `f(...)` inline,
  - `await spawn f(...)`,
  comparer shape des failures.

Risque: coupling fort au scheduler.

Garde-fou:

- interface scheduler injectable + fake testable.

## Definition of Done

- `spawn` stable et conforme.
- Frontiere d'execution observable et testee.

## Gate governance de fin de sprint

Obligatoire avant cloture:

- Rapport spec-delta: `Governance/04-spec-delta-review.md`.
- Mise a jour conformance matrix (statut des requirements touchees).
- Validation des risques ouverts (acceptes/replanifies/corriges).

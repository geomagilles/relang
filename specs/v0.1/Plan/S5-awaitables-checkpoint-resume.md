# S5 - Awaitables v1 et checkpoint/resume precoce

## Objectif

Introduire tot le coeur durable pour valider la faisabilite de snapshot/resume avant la complexite distribuee.

## Perimetre

IN:

- Type `*T`.
- `await`.
- Table d'awaitables runtime.
- Checkpoint sur suspension.
- Resume depuis dernier snapshot.

OUT:

- `spawn` distribue complet.
- familles d'actions completes.
- coordination `and/or` avancee (arrive S6).

## References spec

- relang-spec-v0.1-canonique.md (execution + checkpoints)
- relang-execution-model.md
- relang-awaitables-proposal.md

## Sequence d'implementation

1. Definir `AwaitableHandle` interne:
   - id,
   - createdAt,
   - status,
   - result/failure.
2. Implementer type checker `await : *T -> T | Failure`.
3. Implementer suspension runtime:
   - si non resolu, checkpoint,
   - restitution sur reprise.
4. Definir schema snapshot v1:
   - execution metadata,
   - locals serializes,
   - PC,
   - awaitable table.
5. Implementer persistance snapshot (backend choisi).
6. Implementer resume:
   - recharge snapshot,
   - restauration variables,
   - continuation au bon PC.
7. Assurer non-reexecution des effets deja resolus.
8. Ajouter tooling test:
   - injecter crash apres checkpoint N,
   - reprendre et verifier resultat final identique.

## Livrables

- Runtime durable minimal operationnel.
- Format snapshot versionne (v1).
- Test harness crash/resume.

## Tests obligatoires

- `await` suspend puis resume correctement.
- resultats deja resolus non rejoues.
- locals restaurees a l'identique.
- corruption snapshot detectee avec erreur claire.

## Risques et garde-fous

Risque: vouloir serialiser des objets runtime Truffle non serializables.

Garde-fou:

- separer strictement:
  - etat langage serializable,
  - objets runtime ephemeres reconstruits au resume.

Risque: divergences de comportement apres resume.

Garde-fou:

- golden tests "run complet" vs "run avec crash".

## Definition of Done

- Demonstration stable du cycle:
  - start,
  - suspend/checkpoint,
  - crash,
  - resume,
  - complete.


## Gate governance de fin de sprint

Obligatoire avant cloture:

- Rapport spec-delta: `Governance/04-spec-delta-review.md`.
- Mise a jour conformance matrix (statut des requirements touchees).
- Validation des risques ouverts (acceptes/replanifies/corriges).

## Gate supplementaire S5 (golden resume tests)

- Mettre en place le harness de crash injection.
- Creer des golden traces sur scenarios `await` simples et en chaine.
- Faire echouer la CI si run nominal vs run resume divergent.

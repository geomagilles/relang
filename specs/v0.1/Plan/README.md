# Plan d'implementation ReLang v0.1 sur Truffle

Ce dossier contient un plan d'execution S1 a S11, orientee features, avec un niveau de detail suffisant pour deleguer l'implementation a une equipe.

## Hypotheses d'architecture (verrouillees)

- Runtime: Truffle AST classique (pas Bytecode DSL au depart).
- Execution model: snapshot-first, sans replay applicatif complet.
- Durabilite: introduite tot (S5) et etendue ensuite.
- Contrat normatif prioritaire:
  - relang-spec-v0.1-canonique.md
  - relang-failures-proposal.md

## Regles de pilotage transverses

- Aucune feature n'est "Done" sans:
  - parser + typer + runtime + tests.
- Toute feature liee a `await` doit inclure:
  - test checkpoint/resume,
  - test propagation `Failure`.
- Toute evolution de schema runtime doit inclure:
  - version explicite,
  - test migration ou test de refus de resume.
- Gate obligatoire de fin de sprint:
  - revue `spec-delta`,
  - mise a jour conformance matrix,
  - validation des risques ouverts.

## Convention de backlog

Chaque phase Sx inclut:

1. Objectif.
2. Perimetre IN / OUT.
3. Sequence d'implementation (ordre strict).
4. Livrables attendus.
5. Tests obligatoires.
6. Risques et garde-fous.
7. Definition of Done (DoD).

## Milestones critiques

- M1 (fin S4): langage synchrone stable (sans awaitables complets).
- M2 (fin S6): coeur durable (`await`, checkpoint/resume, failures, coordination, timer/signal).
- M3 (fin S7): frontiere distribuee `spawn` stable.
- M4 (fin S10): familles d'actions principales + observabilite + hardening.
- M5 (fin S11): diagnostics and developer experience at world-class quality bar.

## Idees pour augmenter les chances de succes

1. Crer un "Conformance Suite" des le S2 et l'alimenter a chaque sprint.
2. Mettre un "golden trace" runtime en S5 pour comparer les transitions d'etat.
3. Ajouter une revue "spec-delta" en fin de sprint:
   - ce qui est conforme,
   - ce qui diverge,
   - decision explicite (acceptation ou correction).
4. Isoler un module `runtime-contract` (types snapshots, failure envelope, execution refs) fige tot.
5. Interdire les refactors structurels sur snapshots apres S6 sans RFC interne.

## Pack Governance (a appliquer)

- `Governance/01-runtime-contract.md`
- `Governance/02-conformance-matrix.md`
- `Governance/03-golden-resume-tests.md`
- `Governance/04-spec-delta-review.md`
- `Governance/05-snapshot-rfc-policy.md`
- `Governance/06-diagnostic-quality-playbook.md`

## Integration des ameliorations dans le planning

- S2: `runtime-contract` versionne et fige.
- S3: conformance matrix activee en gate.
- S5: golden resume tests obligatoires.
- S6+: snapshot RFC policy obligatoire.
- S1-S10: spec-delta review obligatoire en fin de sprint.
- S11: diagnostic quality playbook + diagnostic snapshots as merge gates.

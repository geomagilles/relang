# S10 - Shell, Container, observabilite, hardening

## Objectif

Terminer le perimetre v0.1 avec les actions systeme et verrouiller la qualite operationnelle.

## Perimetre

IN:

- Action family shell.
- Action family container.
- Instrumentation Truffle (tags standard).
- Hardening perf + fiabilite + conformance complete.

OUT:

- extensions v0.2 (streaming, retry DSL, first-class functions).

## References spec

- action-shell.md
- action-container.md
- relang-actions-proposal.md
- relang-spec-v0.1-canonique.md

## Sequence d'implementation

1. Implementer family shell:
   - `run(program,args)` safe,
   - `command(...)` interprete,
   - output modes,
   - successCodes,
   - erreurs dediees.
2. Implementer family container:
   - run-to-completion,
   - options ressources/mount/env,
   - output modes,
   - erreurs dediees.
3. Integrer controles de securite shell:
   - docs et warnings sur injection.
4. Ajouter instrumentation Truffle:
   - `StatementTag`, `ExpressionTag`, `CallTag`, etc.
5. Exposer observabilite runtime:
   - correlation executionId/failure.id,
   - traces de checkpoint.
6. Lancer campagne hardening:
   - perf micro/macro,
   - chaos crash/resume,
   - tests endurance.
7. Executer conformance suite finale v0.1.

## Livrables

- Toutes families v0.1 livrees.
- Observabilite exploitable en operation.
- Rapport de readiness v0.1.

## Tests obligatoires

- shell success/error/timeout/cancel.
- container success/error/timeout/cancel.
- resume sous charge avec actions mixtes.
- compatibilite snapshots sur jeux de donnees reels.
- conformance matrix 100% sur MUST v0.1.

## Risques et garde-fous

Risque: actions shell/container introduisent fragilite environnementale.

Garde-fou:

- tests hermetiques,
- runners dedies,
- fixtures reproductibles.

Risque: manque de visibilite en incident.

Garde-fou:

- dashboards minimaux + logs structures + correlation IDs.

## Definition of Done

- Suite complete v0.1 verte.
- Rapport final:
  - conformite spec,
  - ecarts eventuels,
  - plan de suivi v0.2.


## Gate governance de fin de sprint

Obligatoire avant cloture:

- Rapport spec-delta: `Governance/04-spec-delta-review.md`.
- Mise a jour conformance matrix (statut des requirements touchees).
- Validation des risques ouverts (acceptes/replanifies/corriges).

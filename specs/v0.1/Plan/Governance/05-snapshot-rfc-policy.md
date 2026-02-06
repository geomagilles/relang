# Snapshot Schema RFC Policy (obligatoire des S6)

## Objectif

Eviter les cassures de resume dues a des changements de schema non controles.

## Quand une RFC est obligatoire

- Ajout/suppression/renommage d'un champ persiste.
- Changement de type d'un champ persiste.
- Changement d'ordre/encodage impactant lecture.
- Changement des invariants de `Failure` ou awaitables persistes.

## Contenu minimum RFC

1. Contexte et motivation.
2. Changement de schema precis.
3. Plan de compatibilite:
   - migration,
   - fallback,
   - ou refus de resume explicite.
4. Plan de test:
   - backward,
   - forward,
   - crash/resume.
5. Rollout et rollback.

## Gate

- Pas de merge sans RFC validee pour les changements concernes.
- Chaque RFC doit referencer des tests concrets passes.


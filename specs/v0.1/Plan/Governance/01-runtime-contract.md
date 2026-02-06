# Runtime Contract (obligatoire des S2)

## Objectif

Figer tres tot les structures runtime critiques pour reduire les regressions sur snapshot/resume et propagation des failures.

## Contenu contractuel minimal

1. `ExecutionRef`:
   - `executionId`
   - `functionName`
   - `parentExecutionId`
2. `Failure` envelope:
   - `id`
   - `kind`
   - `sourceId`
   - `failedAt`
   - `execution`
   - `cause`
   - `causes`
3. Snapshot metadata:
   - `snapshotVersion`
   - `codeVersion`
   - `stateVersion`
   - `capturedAt`
4. Awaitable persisted shape:
   - `awaitableId`
   - `createdAt`
   - `status`
   - `resolvedAt`
   - `resultRef` ou `failureRef`

## Regles

- Toute modification de shape apres S2 exige:
  - RFC courte,
  - tests de migration ou test de refus explicite.
- Les champs contractuels ne doivent pas dependre d'objets Truffle non serializables.
- Le contrat doit etre publie en JSON schema (ou equivalent typed schema).

## Livrables obligatoires

- Fichier schema versionne (ex: `runtime-contract-v1.json`).
- Guide mapping type system -> format persiste.
- Suite tests compatibilite backward/forward de base.


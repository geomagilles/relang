# Conformance Matrix (obligatoire des S3)

## Objectif

Tracer explicitement la conformite aux exigences normatives v0.1 et empecher les derives silencieuses.

## Format recommande

Colonnes:

- `ReqID` (ex: SPEC-6.2-AWAIT-TYPING)
- `SourceDoc`
- `RequirementText`
- `Priority` (`MUST`, `SHOULD`)
- `ImplementationRef` (module/code)
- `TestRef` (test(s) couvrants)
- `Status` (`todo`, `in-progress`, `done`, `waived`)
- `Comment`

## Regles de gate

- Aucun sprint ne ferme avec un `MUST` sans statut (`done` ou `waived` motive).
- Toute requirement `done` doit pointer au moins un test.
- Toute requirement `waived` doit pointer une decision formelle (date, owner, rationale).

## Livrables obligatoires

- `conformance-matrix-v0.1.csv` versionnee dans le repo.
- Rapport de couverture en fin de sprint.


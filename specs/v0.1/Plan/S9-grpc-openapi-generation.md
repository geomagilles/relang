# S9 - Familles gRPC et OpenAPI avec generation

## Objectif

Ajouter les integrations typees compile-time pour gRPC et OpenAPI afin de reduire les erreurs d'integration.

## Perimetre

IN:

- Action family gRPC.
- Generation clients depuis proto.
- Action family OpenAPI.
- Generation clients depuis specs OpenAPI.

OUT:

- streaming natif (hors scope v0.1).

## References spec

- action-grpc.md
- action-openapi.md
- relang-actions-proposal.md

## Sequence d'implementation

1. Definir pipeline de generation code:
   - input spec,
   - validation,
   - generation stubs ReLang.
2. Implementer family gRPC:
   - unary calls,
   - metadata,
   - credentials,
   - map erreurs `GrpcError`.
3. Implementer family OpenAPI:
   - operationId -> methodes,
   - types request/response,
   - map erreurs `OpenApiError`.
4. Integrer generation dans build.
5. Ajouter cache generation et invalidation par hash spec.
6. Ajouter diagnostics de generation:
   - spec invalide,
   - operationId duplique,
   - schema non supporte.

## Livrables

- Tooling generation stable.
- Families gRPC/OpenAPI exploitables.

## Tests obligatoires

- generation proto simple.
- generation openapi simple.
- appel gRPC succes + erreur status.
- appel OpenAPI succes + ApiError.
- build echec propre sur spec invalide.

## Risques et garde-fous

Risque: specs reelles tres heterogenes.

Garde-fou:

- mode strict + rapport des limitations.

Risque: dette maintenance generateur.

Garde-fou:

- separation claire:
  - parser spec,
  - modele intermediaire,
  - renderer code.

## Definition of Done

- generation reproductible.
- families gRPC/OpenAPI conformes v0.1.

## Gate governance de fin de sprint

Obligatoire avant cloture:

- Rapport spec-delta: `Governance/04-spec-delta-review.md`.
- Mise a jour conformance matrix (statut des requirements touchees).
- Validation des risques ouverts (acceptes/replanifies/corriges).

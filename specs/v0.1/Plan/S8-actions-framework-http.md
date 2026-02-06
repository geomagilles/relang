# S8 - Framework d'actions + famille HTTP

## Objectif

Fournir la premiere famille d'effets externes complete sur une base d'action families robuste.

## Perimetre

IN:

- Infra commune action family.
- Contrat erreurs de famille.
- HTTP action family complete v0.1.
- Idempotency key.
- Cancellation best-effort.

OUT:

- gRPC/OpenAPI/shell/container (S9-S10).

## References spec

- relang-actions-proposal.md
- action-http.md
- relang-failures-proposal.md

## Sequence d'implementation

1. Definir interface interne `ActionExecutor`:
   - validate input,
   - execute,
   - map error,
   - produce awaitable result.
2. Definir registre de familles d'actions.
3. Implementer mapping erreur famille -> `ActionError` scelle.
4. Implementer HTTP API:
   - methods,
   - options,
   - auth,
   - output modes.
5. Implementer mapping erreurs HTTP:
   - timeout,
   - dns,
   - status,
   - protocol.
6. Integrer idempotency key.
7. Integrer cancellation best-effort (etat observables).
8. Ajouter instrumentation de latence + attempts.

## Livrables

- Action framework extensible.
- HTTP usable en production pilote.

## Tests obligatoires

- HTTP 2xx succes.
- HTTP non-2xx -> `ActionFailed`/`HttpStatus`.
- timeout pattern via `or timer(...)`.
- idempotency key meme resultat.
- cancellation avant completion.

## Risques et garde-fous

Risque: rendre HTTP special-case et casser extensibilite.

Garde-fou:

- imposer contrat commun family d'abord, HTTP ensuite.

Risque: erreurs pauvres en diagnostic.

Garde-fou:

- enrichir `attempts` et conserver details runtime relies a `failure.id`.

## Definition of Done

- HTTP family complete selon spec v0.1.
- infra actions reutilisable pour S9/S10.


## Gate governance de fin de sprint

Obligatoire avant cloture:

- Rapport spec-delta: `Governance/04-spec-delta-review.md`.
- Mise a jour conformance matrix (statut des requirements touchees).
- Validation des risques ouverts (acceptes/replanifies/corriges).

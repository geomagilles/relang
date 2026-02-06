# S6 - Failure canonique, coordination, timers et signals

## Objectif

Stabiliser la semantique d'echec et de coordination, indispensable pour une orchestration durable correcte.

## Perimetre

IN:

- `Failure` envelope canonique.
- Propagation inline.
- `and`/`or` awaitables.
- `AllFailed` (ordre lexical).
- `timer(...)`, `receive<T>()`.

OUT:

- wrapping `FunctionFailed` via `spawn` (S7).
- toutes familles d'actions externes (S8+).

## References spec

- relang-failures-proposal.md
- relang-awaitables-proposal.md
- relang-timers-proposal.md
- relang-signals-proposal.md

## Sequence d'implementation

1. Implementer types runtime `Failure`, `FailureKind`, `ExecutionRef`.
2. Verifier invariants:
   - `cause` xor `causes`,
   - contraintes par kind.
3. Implementer propagation `x!` (meme failure en inline).
4. Implementer coordination:
   - `and` fail-fast,
   - `or` first-success,
   - `AllFailed` si tout echoue.
5. Garantir ordre lexical dans `AllFailed.causes`.
6. Implementer `timer(Duration|Timestamp)`:
   - persister `fireAt`.
7. Implementer `receive<T>()`:
   - awaitable durable,
   - reprise sur snapshot.
8. Ajouter API introspection awaitables read-only.

## Livrables

- Semantique failure conforme v0.1.
- Coordination stable.
- Timers/signals durables.

## Tests obligatoires

- `and` retourne premier echec observe.
- `or` retourne premier succes.
- `or` all fail -> `AllFailed` ordonne lexicalement.
- timer timeout pattern avec `or`.
- signal receive + resume.

## Risques et garde-fous

Risque: confusion entre erreurs metier et `Failure` runtime.

Garde-fou:

- tests explicites domaine vs infra.

Risque: ordre non deterministe des causes en concurrence.

Garde-fou:

- tri final selon ordre source, pas ordre d'arrivee runtime.

## Definition of Done

- Failure model et coordination validables par conformance tests.
- timers/signals operationnels en resume.


## Gate governance de fin de sprint

Obligatoire avant cloture:

- Rapport spec-delta: `Governance/04-spec-delta-review.md`.
- Mise a jour conformance matrix (statut des requirements touchees).
- Validation des risques ouverts (acceptes/replanifies/corriges).

## Gate supplementaire S6 (snapshot RFC policy)

- Activer la policy `Governance/05-snapshot-rfc-policy.md`.
- Exiger RFC pour tout changement schema persiste.
- Ajouter un controle CI (ou checklist PR) qui bloque sans reference RFC.

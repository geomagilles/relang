# ReLang v0.1 minimal

## Introduction

ReLang est un langage d'orchestration durable concu pour des executions distribuees.

Idee centrale:

1. un programme avance par etapes,
2. les effets externes (HTTP, gRPC, shell, container, signaux, timers) sont explicites,
3. l'etat d'execution est persiste regulierement en snapshot,
4. le runtime reprend depuis le dernier etat stable, sans imposer un replay applicatif complet.

Pourquoi ce design:

1. fiabilite: une execution peut survivre aux crashes/redeploys/migrations,
2. clarte: les frontieres d'effet et de suspension sont visibles dans le code (`await`),
3. scalabilite: les fonctions peuvent s'executer inline ou en distribue (`spawn`),
4. portabilite: le contrat langage reste stable (`Failure`, types serializables), les details machine restent cote runtime.

Ce dossier v0.1 formalise un noyau implementable, pragmatique, et coherent avec ce modele.

## Use cases (rapide)

ReLang est adapte en priorite a:

1. orchestrations metier longues (commande, paiement, fulfillment),
2. coordination de plusieurs services externes avec retries/policies runtime,
3. workflows humains-in-the-loop (attente de signal d'approbation),
4. traitements batch distribues avec fan-out/fan-in (`spawn`, `and`, `or`),
5. pipelines robustes ou une reprise fiable apres incident est obligatoire.

Perimetre minimal v0.1 (normatif):

1. `relang-spec-v0.1-canonique.md`:
   1. noyau langage/runtime,
   2. transitions principales,
   3. awaitables, coordination, snapshots, propagation.
2. `relang-failures-proposal.md`:
   1. model de donnees `Failure`,
   2. causalite (`cause`/`causes`),
   3. difference inline vs distribue,
   4. separation contrat langage vs diagnostics runtime.

Documents detailles (informatifs, alignes v0.1):

1. `relang-execution-model.md`
2. `relang-actions-proposal.md`
3. `action-http.md`
4. `action-grpc.md`
5. `action-openapi.md`
6. `action-shell.md`
7. `action-container.md`
8. `relang-functions.md`
9. `relang-lambdas-proposal.md`
10. `relang-modules-proposal.md`
11. `relang-null-safety-proposal.md`
12. `relang-conditionals-proposal.md`
13. `relang-operators-proposal.md`
14. `relang-equality-proposal.md`
15. `relang-loops-proposal.md`
16. `relang-awaitables-proposal.md`
17. `relang-timers-proposal.md`
18. `relang-signals-proposal.md`
19. `relang-types-proposal.md`
20. `relang-primitives-proposal.md`
21. `relang-variables-proposal.md`
22. `relang-lifecycle-proposal.md`
23. `relang-collections-proposal.md`

Hors perimetre v0.1:

1. fonctions first-class,
2. DSL policy inline,
3. streaming natif,
4. spec conformance v0.2.

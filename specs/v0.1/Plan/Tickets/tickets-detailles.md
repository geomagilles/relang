# Tickets detailles S1-S10

## S1

### RL-S1-001 - Creer le socle `TruffleLanguage` et `RelangContext`
- Description: Implementer la classe langage, initialisation de contexte, options de base, cycle create/dispose.
- Steps:
  1. Creer `RelangLanguage`.
  2. Creer `RelangContext`.
  3. Brancher creation contexte par execution.
  4. Exposer config minimale injectable.
- Acceptance criteria:
  - Un contexte est cree par execution racine.
  - Le contexte est detruit proprement.
  - Aucune reference globale mutable non controlee.
- Dependances: aucune.
- Estimation: 1.5 j.

### RL-S1-002 - Implementer parse minimal et `RootNode` executable
- Description: Ajouter parser stub minimal et construction d'un `CallTarget` runnable.
- Steps:
  1. Parser source vide/constant.
  2. Construire AST minimal.
  3. Brancher `parse(...)` vers `RootNode`.
  4. Retourner `CallTarget` executable.
- Acceptance criteria:
  - `run` execute une expression constante.
  - Source invalide retourne erreur de parse positionnee.
- Dependances: RL-S1-001.
- Estimation: 1.5 j.

### RL-S1-003 - Creer harness de tests smoke + CLI dev
- Description: Mettre en place `run/check/test` local et tests smoke automatiques.
- Steps:
  1. Ajouter commande `run`.
  2. Ajouter commande `check`.
  3. Ecrire tests smoke parse/exec.
  4. Ajouter scripts standardises.
- Acceptance criteria:
  - `run/check/test` fonctionnent en local.
  - 3 tests smoke minimum verts.
- Dependances: RL-S1-002.
- Estimation: 1.0 j.

### RL-S1-004 - Ecrire note architecture initiale + pipeline CI baseline
- Description: Documenter separation parser/typer/runtime et configurer CI build+test.
- Steps:
  1. Rediger doc architecture (1 page).
  2. Configurer pipeline CI baseline.
  3. Echouer CI si tests non verts.
- Acceptance criteria:
  - CI execute build+test sur PR.
  - Doc architecture versionnee.
- Dependances: RL-S1-003.
- Estimation: 1.0 j.

## S2

### RL-S2-101 - Implementer modele de types primitifs
- Description: Introduire `RelangType` et les primitives v0.1 + `Unit`.
- Steps:
  1. Definir hierarchie de types.
  2. Ajouter primitives spec.
  3. Ajouter operations de comparaison de compatibilite.
- Acceptance criteria:
  - Types primitives reconnus parser+checker.
  - Refus de types inconnus.
- Dependances: RL-S1-004.
- Estimation: 1.5 j.

### RL-S2-102 - Implementer regles de typage des operateurs primitifs
- Description: Gerer arithmetique/comparaison/logique primitive sans coercion implicite.
- Steps:
  1. Regles `+ - * / %`.
  2. Regles comparaison meme type.
  3. Erreurs cross-type explicites.
- Acceptance criteria:
  - Pas de conversion implicite `Int<->Float`.
  - Messages d'erreur indiquent type attendu/obtenu.
- Dependances: RL-S2-101.
- Estimation: 1.5 j.

### RL-S2-103 - Implementer optionnels `T?` et valeur `none`
- Description: Support complet syntaxe/types/runtime de l'absence de valeur.
- Steps:
  1. Ajouter `OptionalType`.
  2. Ajouter literal `none`.
  3. Regles d'assignation optionnelle.
- Acceptance criteria:
  - `none` interdit sur type non optionnel.
  - `String?` accepte `none` et `String`.
- Dependances: RL-S2-101.
- Estimation: 1.0 j.

### RL-S2-104 - Publier `runtime-contract-v1` et tests schema
- Description: Creer contrat runtime versionne selon governance `01-runtime-contract`.
- Steps:
  1. Definir schema contractuel v1.
  2. Ajouter tests lecture/ecriture schema.
  3. Versionner artefact schema.
- Acceptance criteria:
  - Schema present en repo.
  - Tests de compatibilite schema verts.
- Dependances: RL-S2-101.
- Estimation: 1.0 j.

## S3

### RL-S3-201 - Implementer binding variables et scopes lexicaux
- Description: Mapper `let` et scopes sur frames Truffle avec shadowing correct.
- Steps:
  1. Frame slots par scope.
  2. Resolution locale->parent.
  3. Shadowing local.
- Acceptance criteria:
  - Shadowing ne fuit pas hors bloc.
  - Variable out-of-scope detectee en compile.
- Dependances: RL-S2-104.
- Estimation: 1.5 j.

### RL-S3-202 - Implementer `if` expression et `match` minimal
- Description: Ajouter controle conditionnel de base avec typing strict.
- Steps:
  1. `if` bool only.
  2. `if` expression typing.
  3. `match` literals/none/wildcard.
- Acceptance criteria:
  - `if` non bool rejete.
  - `match` sans branche valide rejete.
- Dependances: RL-S3-201.
- Estimation: 1.5 j.

### RL-S3-203 - Implementer boucles de base et controle de boucle
- Description: Ajouter `for`, `while`, `break`, `continue`.
- Steps:
  1. `for` sur listes/ranges.
  2. `while` bool.
  3. Exceptions internes de controle `break/continue`.
- Acceptance criteria:
  - `break/continue` hors boucle rejete.
  - Boucles imbriquees comportement correct.
- Dependances: RL-S3-201.
- Estimation: 1.0 j.

### RL-S3-204 - Initialiser conformance matrix + gate CI
- Description: Creer la matrice de conformite et bloquer les `MUST` non traces.
- Steps:
  1. Creer fichier matrix CSV.
  2. Lier requirements couvrees par tests.
  3. Ajouter check CI de completude minimale.
- Acceptance criteria:
  - Matrix versionnee.
  - CI echoue si requirement `MUST` touchee sans statut/test.
- Dependances: RL-S3-202.
- Estimation: 1.0 j.

## S4

### RL-S4-301 - Implementer declarations `fn` top-level et appels inline
- Description: Supporter fonctions nommees, appels inline et retours implicites.
- Steps:
  1. AST `FnDecl`.
  2. Enregistrement symboles fonctions.
  3. Execution inline.
- Acceptance criteria:
  - Fonctions top-level executables.
  - Appel inline retourne valeur attendue.
- Dependances: RL-S3-204.
- Estimation: 1.5 j.

### RL-S4-302 - Parametres nommes/defaults et hoisting
- Description: Ajouter semantics des parametres et appel avant declaration.
- Steps:
  1. Parsing args nommes.
  2. Resolution defaults.
  3. Hoisting intra-module.
- Acceptance criteria:
  - Appels mixes invalides rejetes.
  - Hoisting valide couvre recursion mutuelle simple.
- Dependances: RL-S4-301.
- Estimation: 1.5 j.

### RL-S4-303 - Implementer modules/imports/private et cycles
- Description: Resoudre imports, visibilite et blocage des cycles.
- Steps:
  1. Resolution imports explicites/alias/namespace.
  2. Application `private`.
  3. Detection cycles.
- Acceptance criteria:
  - Cycle module detecte avec message clair.
  - Symboles `private` inaccessibles hors module.
- Dependances: RL-S4-301.
- Estimation: 1.5 j.

### RL-S4-304 - Interdire capture scope des fonctions + abstraction call mode
- Description: Enforcer self-contained et preparer mode inline/spawn.
- Steps:
  1. Analyse references libres.
  2. Erreur compile si capture detectee.
  3. Introduire abstraction interne `CallMode`.
- Acceptance criteria:
  - Capture scope toujours rejetee.
  - Aucune regression sur appels inline.
- Dependances: RL-S4-302.
- Estimation: 1.0 j.

## S5

### RL-S5-401 - Implementer type `*T` et `AwaitableHandle`
- Description: Ajouter type awaitable avec identite stable et statut.
- Steps:
  1. Type checker `*T`.
  2. Runtime handle id/createdAt/status.
  3. Liaison expression effet -> handle.
- Acceptance criteria:
  - `await : *T -> T | Failure` valide.
  - Awaitable expose id et timestamps.
- Dependances: RL-S4-304.
- Estimation: 1.5 j.

### RL-S5-402 - Implementer checkpoints sur suspension
- Description: Persister snapshot lors d'un `await` non resolu.
- Steps:
  1. Capturer locals/PC/awaitables.
  2. Persist snapshot v1.
  3. Marquer etat execution suspendu.
- Acceptance criteria:
  - Snapshot cree a suspension.
  - Schema snapshot conforme v1.
- Dependances: RL-S5-401.
- Estimation: 1.5 j.

### RL-S5-403 - Implementer resume et non-rejeu des effets resolus
- Description: Restaurer execution depuis snapshot sans re-emission d'effets resolus.
- Steps:
  1. Charger snapshot.
  2. Restaurer locals/PC.
  3. Reutiliser resolutions deja persistees.
- Acceptance criteria:
  - Resume reprend au bon point.
  - Effets resolus non rejoues.
- Dependances: RL-S5-402.
- Estimation: 1.5 j.

### RL-S5-404 - Mettre en place golden crash/resume harness
- Description: Introduire tests differenciels run nominal vs crash/resume.
- Steps:
  1. Injecter crash apres checkpoint N.
  2. Produire traces run nominal/resume.
  3. Comparer oracles resultat/etat.
- Acceptance criteria:
  - Au moins 3 scenarios golden.
  - CI echoue si divergence.
- Dependances: RL-S5-403.
- Estimation: 1.0 j.

## S6

### RL-S6-501 - Implementer `Failure` envelope et invariants
- Description: Introduire modele `Failure` canonique, verifications invariantes et serialisation.
- Steps:
  1. Types `Failure`, `FailureKind`, `ExecutionRef`.
  2. Validateurs invariants.
  3. Serialisation stable.
- Acceptance criteria:
  - Invariants violes detectes.
  - Round-trip serialisation stable.
- Dependances: RL-S5-404.
- Estimation: 1.5 j.

### RL-S6-502 - Implementer coordination awaitables `and/or`
- Description: Semantique `and` fail-fast et `or` first-success avec `AllFailed` lexical.
- Steps:
  1. Composition typed awaitables.
  2. Regles fail-fast/first-success.
  3. Ordonnancement lexical `causes`.
- Acceptance criteria:
  - `or` all fail -> `AllFailed` ordonne.
  - `and` propage premier echec observe.
- Dependances: RL-S6-501.
- Estimation: 1.5 j.

### RL-S6-503 - Implementer timers et signaux durables
- Description: Ajouter `timer(...)` et `receive<T>()` avec persistence resume-safe.
- Steps:
  1. Timer `Duration/Timestamp` avec `fireAt`.
  2. Receive durable.
  3. Resume coherent de ces awaitables.
- Acceptance criteria:
  - Timeout pattern via `or timer(...)` passe.
  - receive resume correctement apres crash.
- Dependances: RL-S5-403.
- Estimation: 1.5 j.

### RL-S6-504 - Activer policy RFC snapshots + check PR
- Description: Mettre la policy de changement schema en pratique des S6.
- Steps:
  1. Ajouter checklist PR RFC.
  2. Bloquer merge sans reference RFC si schema touche.
  3. Lier RFC a tests.
- Acceptance criteria:
  - Merges schema-blockes sans RFC.
  - Exemple RFC validee dans sprint.
- Dependances: RL-S6-501.
- Estimation: 0.5 j.

## S7

### RL-S7-601 - Implementer `spawn` et abstraction scheduler
- Description: Lancer execution enfant avec identite propre et parentId.
- Steps:
  1. API `spawn`.
  2. Scheduler local injectable.
  3. Passage args serialises.
- Acceptance criteria:
  - Enfant cree avec `self.id` unique.
  - `parentId` renseigne correctement.
- Dependances: RL-S6-503.
- Estimation: 1.5 j.

### RL-S7-602 - Implementer wrapping `FunctionFailed` sur frontiere spawn
- Description: Assurer difference inline/distribue dans propagation failures.
- Steps:
  1. Mapper echec enfant -> wrapper parent.
  2. Preserver causalite via `cause`.
  3. Conserver invariants envelope.
- Acceptance criteria:
  - `await spawn` echec retourne `FunctionFailed`.
  - `f(...)` inline ne wrap pas.
- Dependances: RL-S7-601.
- Estimation: 1.0 j.

### RL-S7-603 - Implementer gate `codeVersion/stateVersion` au resume
- Description: Controler compatibilite code/state pour reprise fiable.
- Steps:
  1. Attacher `codeVersion` execution.
  2. Attacher `stateVersion` snapshots.
  3. Refuser resume incompatible.
- Acceptance criteria:
  - Resume refuse avec erreur explicite si incompatibilite.
  - Resume accepte si versions compatibles.
- Dependances: RL-S7-601.
- Estimation: 1.0 j.

### RL-S7-604 - Tests distribues parent/enfant/grandchild + causalite
- Description: Valider chaines d'execution distribuees et resumes associes.
- Steps:
  1. Scenario succes multi-niveaux.
  2. Scenario echec multi-niveaux.
  3. Scenario crash/resume inter-executions.
- Acceptance criteria:
  - Causal chain complete et correcte.
  - Aucun rejeu d'effets resolves apres resume.
- Dependances: RL-S7-602, RL-S7-603.
- Estimation: 1.5 j.

## S8

### RL-S8-701 - Implementer framework action family generique
- Description: Base commune extensible pour toutes families d'actions.
- Steps:
  1. Interface `ActionExecutor`.
  2. Registry families.
  3. Mapping erreurs -> `ActionError`.
- Acceptance criteria:
  - Nouvelle famille injectable sans changer coeur runtime.
  - Contrat erreurs unifie.
- Dependances: RL-S7-604.
- Estimation: 1.5 j.

### RL-S8-702 - Implementer HTTP family core API
- Description: Ajouter methods/options/output modes conformes v0.1.
- Steps:
  1. Methods GET/POST/PUT/... .
  2. Options headers/auth/query/output.
  3. Parsing outputs.
- Acceptance criteria:
  - Appels HTTP simples fonctionnels.
  - Output modes `Content/Response/Raw` valides.
- Dependances: RL-S8-701.
- Estimation: 1.5 j.

### RL-S8-703 - Implementer erreurs HTTP, idempotency, cancellation
- Description: Mapper erreurs reseau/status/timeout et features d'exploitation.
- Steps:
  1. Map `HttpError` complet.
  2. Idempotency key handling.
  3. Cancellation best-effort.
- Acceptance criteria:
  - Non-2xx mappe proprement en `ActionFailed`.
  - Idempotency key renvoie meme resultat.
- Dependances: RL-S8-702.
- Estimation: 1.5 j.

### RL-S8-704 - Ajouter observabilite attempts et mise a jour matrix
- Description: Capturer attempts runtime et mettre a jour conformite action layer.
- Steps:
  1. Capturer `Attempt` metadata.
  2. Exposer correlation `failure.id`.
  3. MAJ conformance entries.
- Acceptance criteria:
  - Attempts visibles en diagnostic.
  - Matrix couvre nouvelles requirements HTTP.
- Dependances: RL-S8-703.
- Estimation: 1.0 j.

## S9

### RL-S9-801 - Implementer pipeline de generation (modele intermediaire)
- Description: Construire moteur de generation commun proto/openapi -> IR -> code.
- Steps:
  1. Parser specs.
  2. Generer modele intermediaire stable.
  3. Generer stubs ReLang.
- Acceptance criteria:
  - Pipeline genere artefacts reproductibles.
  - Erreurs de spec remontees clairement.
- Dependances: RL-S8-704.
- Estimation: 1.5 j.

### RL-S9-802 - Implementer famille gRPC + stubs generes
- Description: Support unary, metadata, credentials, mapping `GrpcError`.
- Steps:
  1. Runtime calls unary.
  2. Metadata/trailers.
  3. Mapping erreurs gRPC.
- Acceptance criteria:
  - Appel gRPC success + status error passes.
  - Stubs generes exploitables dans code workflow.
- Dependances: RL-S9-801.
- Estimation: 1.5 j.

### RL-S9-803 - Implementer famille OpenAPI + stubs generes
- Description: Generer methods operationId, types requests/reponses, map erreurs API.
- Steps:
  1. Mapper operationId->fn.
  2. Types schema -> types ReLang.
  3. Gestion `ApiError/ValidationError`.
- Acceptance criteria:
  - Appel OpenAPI success + ApiError passes.
  - Specs invalides echouent proprement.
- Dependances: RL-S9-801.
- Estimation: 1.5 j.

### RL-S9-804 - Integrer generation build + cache hash + mode strict
- Description: Integrer generateur dans build pour CI fiable.
- Steps:
  1. Hook build.
  2. Cache par hash spec.
  3. Mode strict + rapport limitations.
- Acceptance criteria:
  - Build deterministic.
  - Regeneration uniquement si spec modifiee.
- Dependances: RL-S9-802, RL-S9-803.
- Estimation: 1.0 j.

## S10

### RL-S10-901 - Implementer family shell (safe-first)
- Description: Ajouter `shell.run` et `shell.command` avec contraintes securite.
- Steps:
  1. `run(program,args)`.
  2. `command(...)` interprete.
  3. output modes + successCodes.
- Acceptance criteria:
  - Success/error/timeout/cancel couverts.
  - Documentation risques injection incluse.
- Dependances: RL-S9-804.
- Estimation: 1.5 j.

### RL-S10-902 - Implementer family container run-to-completion
- Description: Ajouter execution container avec options ressources/mount/env.
- Steps:
  1. API `container.run`.
  2. Mapping erreurs `ContainerError`.
  3. output modes + timeout patterns.
- Acceptance criteria:
  - Success/error/timeout/cancel couverts.
  - Execution run-to-completion conforme.
- Dependances: RL-S10-901.
- Estimation: 1.5 j.

### RL-S10-903 - Instrumentation Truffle + observabilite runtime
- Description: Exposer tags standard et correlation IDs pour debug prod.
- Steps:
  1. Ajouter StandardTags.
  2. Logs structures checkpoints/actions.
  3. Correlation execution/failure.
- Acceptance criteria:
  - Debug stepping possible.
  - Correlation IDs presents sur incidents.
- Dependances: RL-S10-902.
- Estimation: 1.0 j.

### RL-S10-904 - Campagne hardening + rapport readiness final
- Description: Lancer endurance, chaos, conformance finale et produire rapport go/no-go.
- Steps:
  1. Tests charge + crash/resume massifs.
  2. Validation conformance matrix MUST=done.
  3. Rapport final risques restants.
- Acceptance criteria:
  - Suite v0.1 verte.
  - Rapport readiness versionne.
- Dependances: RL-S10-903.
- Estimation: 1.5 j.


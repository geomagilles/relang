# ReLang v0.2 - Proposition d'Amelioration

Status: Draft proposal  
Base: v0.1 canonique (`relang-spec-v0.1-canonique.md`)

## 1. Objectif de v0.2

Consolider ReLang pour une implementation robuste en production, sans perdre la simplicite du modele snapshot-first.

Priorites v0.2:

1. Semantique formelle complete (moins d'ambiguites runtime),
2. Contrat d'etat/versioning explicite (reprises apres evolutions de code),
3. Separation stricte langage/runtime/observabilite,
4. Conformance tests officiels.

## 2. Principes conserves

Ces decisions v0.1 restent valides:

1. Snapshot-first, pas replay-first,
2. `await` explicite, `*T` pour les effets,
3. `timer(...)` primitif; timeout via composition `(task or timer(d))`,
4. Inline vs `spawn` distingues pour propagation des failures,
5. Erreurs machine dans le runtime, pas dans le type langage `Failure`.

## 3. Propositions v0.2 (recommandees)

### 3.1 Semantique operationnelle "small-step"

Ajouter une section normative formelle:

1. Etat machine `M = (stack, env, pc, awaitables, snapshotMeta)`,
2. Regles de transition:
   1. `create_effect` (unifie),
   2. `await_resolved`,
   3. `await_suspend`,
   4. `resume`,
   5. `propagate_failure_inline`,
   6. `wrap_failure_spawn`.

Benefice: compilateur/runtime testables sur le meme referentiel.

#### 3.1.1 Etat et statuts d'awaitable

Proposition d'etat machine:

`M = <exec, frames, env, pc, A, meta>`

1. `exec`: identite execution (`executionId`, parent, `codeVersion`),
2. `frames/env/pc`: pile + variables + position,
3. `A`: table des awaitables (`aid -> AwaitableState`),
4. `meta`: informations snapshot/version.

Avec:

1. `AwaitableState = Pending | Succeeded(value) | Failed(failure)`.

#### 3.1.2 Points d'arret d'une execution

Une execution active ne s'arrete qu'a:

1. `await_suspend` (attente d'un awaitable `Pending`),
2. fin normale (retour racine),
3. echec non gere a la racine (`Failed(Failure)`).

#### 3.1.3 Regle unifiee `create_effect`

Unifier creation d'awaitable "action" et "spawn child" dans une seule primitive runtime:

`create_effect(kind, payload) -> handle(*T)`

Ou:

1. `kind = action` (http, timer, receive, ...),
2. `kind = child` (`spawn f(...)`).

Effets de la transition:

1. allouer `aid`,
2. inserer `A[aid] = Pending`,
3. retourner handle `*T(aid)`,
4. enregistrer metadata minimale dans l'etat.

Specialisations necessaires:

1. `kind = child` cree une execution fille et renseigne `childExecutionId`,
2. le wrapping de failure ne s'applique qu'a la frontiere `spawn`.

#### 3.1.4 Regles de reprise et propagation

`await_resolved`:

1. si `A[aid] = Succeeded(v)` alors `await h => v`,
2. si `A[aid] = Failed(f)` alors `await h => f`.

`await_suspend`:

1. si `A[aid] = Pending`,
2. snapshot de `M`,
3. retour runtime `Suspended(snapshot, waitingOn=aid)`.

`resume`:

1. recharger snapshot,
2. appliquer resolutions runtime dans `A`,
3. reprendre au meme `pc`.

`propagate_failure_inline`:

1. appel `f(...)` inline,
2. pas de frontiere distribuee,
3. la meme `Failure` se propage (pas de wrapper).

`wrap_failure_spawn`:

1. `await spawn f(...)` observe echec enfant,
2. parent recoit `FunctionFailed`,
3. `cause = failure enfant`.

#### 3.1.5 Etat snapshot et awaitables vivants

A chaque suspension, le snapshot doit inclure:

1. pile/locals/pc,
2. awaitables vivants references par l'etat courant (ids + statuts connus),
3. metadata d'effet necessaires a la reprise (`kind`, liens enfant, etc.).

Note: pas besoin de persister tout l'historique des awaitables termines non references.

### 3.2 Contrat d'etat versionne

Introduire une norme stricte de compatibilite:

1. Chaque execution porte:
   1. `languageVersion`,
   2. `codeVersion`,
   3. `stateSchemaVersion`.
2. Reprise autorisee si:
   1. `languageVersion` compatible,
   2. `codeVersion` identique, ou migration declaree.
3. Migration exige:
   1. `@stateVersion(n)` dans le code,
   2. un migrateur pur `state(n-1) -> state(n)`.

### 3.3 Bloc de policy runtime normalise

Ne pas mettre `retry/timeout/rate-limit` dans le langage coeur, mais standardiser un schema policy.

Proposition:

1. Fichier runtime dedie (ex: `relang.runtime.yaml`),
2. Matching de policy par ordre:
   1. `workflow.function`,
   2. `family.action`,
   3. `family.*`,
   4. `global`.
3. Strategie "most specific wins".

Schema minimal:

1. `retry` (`maxAttempts`, `backoff`, `jitter`),
2. `timeout`,
3. `concurrency`,
4. `circuitBreaker`,
5. `idempotency`.

### 3.4 Failure v2 stable + projection runtime

Conserver le modele causal `Failure` v0.1, mais normaliser 2 vues:

1. Vue langage (stable):
   1. `id`,
   2. `kind`,
   3. `failedAt`,
   4. `sourceId`,
   5. `execution`,
   6. `cause` / `causes`.
2. Vue runtime (diagnostic):
   1. `RuntimeFailureReport`,
   2. relation par `failureId`.

Ajouter helper normatif:

1. `failure.root()`,
2. `failure.chain()`,
3. `failure.leafFailures()`.

### 3.5 Determinisme de l'agregation

Rendre explicites les invariants de coordination:

1. `AllFailed.causes` ordonne par ordre lexical des branches,
2. `and` fail-fast retourne la premiere failure observee,
3. `or` gagnant ignore les losers dans le resultat langage (mais conserve en observabilite runtime).

### 3.6 Localisation du point d'echec

Ajouter un `CodeLocation` au contrat langage:

```relang
type CodeLocation {
  module: String
  function: String
  line: Int?
  column: Int?
  checkpointLabel: String?
}
```

Puis:

```relang
type Failure {
  ...
  location: CodeLocation?
}
```

But: debugging dev sans details machine.

### 3.7 Select minimal dans le coeur

Proposer un `select` minimal pour exprimer proprement signaux/timers/courses multi-sources.

Surface:

```relang
select {
  x = taskA -> ...
  y = taskB -> ...
  _ = timer(30s) -> ...
  s = receive<Cancel>() -> ...
}
```

Semantique:

1. premier evenement resolu gagne,
2. annulation best-effort des autres branches,
3. comportement identique en resume.

### 3.8 Test de conformite officiel

Definir une suite de reference (must pass):

1. propagation inline vs spawn,
2. `or` all-failed (ordre lexical des causes),
3. `or` winner + losers ignores en resultat,
4. resume apres chaque `await`,
5. timeout par `(task or timer(d))`,
6. compatibilite et migration d'etat,
7. code root failure -> `Failed(Failure)`.

Reference proposee:

1. `relang-v0.2-conformance-tests.md` (matrice Given/When/Then, CT-001..CT-024).

## 4. Non-objectifs v0.2

1. Function types first-class,
2. Streaming natif dans le coeur langage,
3. DSL policy inline dans les fonctions,
4. Operator overloading utilisateur.

## 5. Impact sur l'ecosysteme

### 5.1 Runtime

1. Besoin d'un moteur de migration d'etat,
2. Besoin d'un evaluateur policy standard,
3. Besoin d'une projection `RuntimeFailureReport`.

### 5.2 Tooling

1. Linter "compatibilite resume",
2. Visualiseur de chaines de failure,
3. Runner conformance.

### 5.3 DX

1. Moins d'ambiguites en prod,
2. Debug plus rapide (failure causale + location),
3. Evolution de code plus sure.

## 6. Plan de migration v0.1 -> v0.2

### Etape 1: Stabiliser les contrats

1. Geler `Failure` langage v2,
2. Geler schema policy runtime,
3. Geler format `state metadata`.

### Etape 2: Ajouter compatibilite ascendante

1. Runtime lit encore les snapshots v0.1,
2. Upgrade automatique vers `stateSchemaVersion` v0.2 si possible.

### Etape 3: Activer la conformance

1. Executer la suite de reference sur chaque runtime,
2. marquer un runtime "ReLang v0.2 compliant" si 100% passe.

## 7. ADR proposes (ordre recommande)

1. ADR-001: Operational semantics small-step,
2. ADR-002: State versioning + migration contract,
3. ADR-003: Runtime policy schema and precedence,
4. ADR-004: Failure core vs runtime diagnostics split,
5. ADR-005: Select core semantics,
6. ADR-006: Conformance test suite.

## 8. Recommendation pratique

Si tu veux livrer vite sans explosion de scope:

1. implémenter d'abord ADR-002 + ADR-004 + ADR-006,
2. puis ADR-001,
3. puis ADR-003/ADR-005.

Cet ordre donne le meilleur ratio "fiabilite production / effort".

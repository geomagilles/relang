# ReLang v0.1 Canonique

Status: Normative draft  
Path: `specs/v0.1/relang-spec-v0.1-canonique.md`
Companion: `specs/v0.1/relang-failures-proposal.md`

## 1. Objectif

ReLang est un langage d'orchestration durable.  
Un programme ReLang:

1. execute de la logique metier deterministe,
2. cree des effets externes explicites,
3. checkpoint son etat sur les frontieres d'effet,
4. reprend exactement depuis le dernier checkpoint.

Cette version v0.1 fixe un noyau coherent et implementable.

## 2. Choix normatifs (tranches)

1. `await` est un mot-cle explicite: `await e` (jamais implicite).
2. Optionnels: surface langage = `T?` + valeur `none`. Pas de `Some/None` en surface.
3. Types utilisateur immuables. Pas de mutation de champ `x.field = ...`.
4. Pas de code executable top-level dans les modules.
5. Un seul modele d'echec `Failure` (shape unique).
6. `and`/`or` sont polymorphes par type d'operandes, mais une seule precedance logique.
7. `now()` suit une horloge naturelle (wall-clock UTC de la machine d'execution), compatible avec le modele snapshot.
8. Pas de mot-cle `checkpoint` en surface langage.
9. Pas de construction `retry` dans le noyau v0.1 (politique runtime/configuration).

## 3. Modele d'execution

### 3.1 Principes

1. Execution durable par snapshots.
2. Toute valeur locale doit etre serializable.
3. Toute operation externe retourne un awaitable `*T`.
4. Un checkpoint est un mecanisme interne runtime cree a chaque `await` qui consomme un awaitable non resolu.

### 3.4 Clarifications operationnelles (normatives)

1. Les awaitables ont une semantique de demarrage logique immediate a la creation.
2. Le runtime peut differer l'execution physique (scheduling, quotas, limites), sans changer le comportement observable du programme.
3. Toute execution est liee a une `codeVersion` runtime.
4. Reprise par defaut: uniquement avec la meme `codeVersion`.
5. Si la `codeVersion` change:
   1. reprise refusee sans migration explicite,
   2. migration autorisee uniquement via mecanisme de version d'etat (ex: `stateVersion` + migrateur).

### 3.2 Etat d'execution

Etat minimal:

1. identite d'execution (`self.id`, `self.parentId`, `self.createdAt`),
2. pile d'appels + variables locales,
3. position de programme (PC),
4. table des awaitables crees (id, statut, resultat),
5. horloge workflow,
6. metadonnees de version d'etat.

### 3.3 Resume

Au resume:

1. runtime recharge le dernier snapshot,
2. restaure pile/locals/PC,
3. restaure les resultats deja resolus,
4. continue l'execution sans rejouer les effets resolves.

## 4. Types

### 4.1 Primitifs

`Int`, `Float`, `Bool`, `String`, `Bytes`, `Duration`, `Timestamp`, `Json`, `Unit`, `Failure`.

### 4.2 Optionnels

`T?` est un sucre pour `T | none` (representation interne possible en `None`).  
Valeur d'absence en surface: `none`.

### 4.3 Collections

`[T]` (liste homogene), `Map<K, V>`.

### 4.4 Produits et unions

1. Produit (tuple): `A & B`, valeur `(a & b)`.
2. Union: `A | B`.

Note: la syntaxe tuple canonique est `&` (pas de tuple virgule).

### 4.5 Types utilisateur

1. `type` = enregistrement nominal immutable.
2. `sealed` = groupe exhaustif de variantes.
3. Pas d'heritage general.

## 5. Fonctions et modules

### 5.1 Fonctions

1. `fn` uniquement top-level.
2. Fonctions fermees: acces uniquement aux parametres et variables locales.
3. Deux modes d'appel:
   1. `f(...)` inline, retourne `T`,
   2. `spawn f(...)`, retourne `*T`.

### 5.2 Lambdas

1. Lambdas inline autorisees pour operations built-in de collection.
2. Pas de type de fonction first-class en v0.1.
3. Pas de stockage/retour/passage de lambda hors built-ins.

### 5.3 Modules

1. Un fichier = un module.
2. Declarations top-level seulement (pas d'expressions top-level).
3. Imports explicites.

## 6. Effets, awaitables et coordination

### 6.1 Frontiere d'effet

Un effet est une operation non deterministe observable:

1. action externe (`http`, `grpc`, `openapi`, `shell`, `container`, etc.),
2. timer (`timer(...)`),
3. signal entrant (`receive<T>()`).

Tous retournent `*T`.

Note v0.1:

1. `timer(...)` est la primitive temporelle canonique.
2. Aucun primitif `timeout(...)` n'est defini dans le noyau.
3. Le pattern de timeout s'exprime par coordination: `(task or timer(d))`.

### 6.2 `await`

Regle de typage:

```relang
await : *T -> T | Failure
```

Propagation:

```relang
await e!    // sucre pour (await e)!
```

`x!` decompresse succes, sinon propage `Failure`.

### 6.3 Demarrage et identite

1. Les awaitables primitifs demarrent eagerly a la creation.
2. Awaitable primitif: identite stable (`id`, `createdAt`).
3. Awaitable compose (`and`/`or`): pas d'identite propre.

### 6.4 Coordination

```relang
*A and *B : *(A & B)
*A or  *B : *(A | B)
```

Semantique:

1. `and`: tous doivent reussir, echec rapide au premier `Failure`.
2. `or`: premier succes gagne, si tous echouent -> `Failure(kind = AllFailed(...))`.
3. Annulation des perdants: best-effort.
4. Si `or` trouve un gagnant:
   1. les losers n'ajoutent pas d'erreur au resultat langage,
   2. leurs details restent dans la couche runtime/observabilite.

## 7. Echec canonique

### 7.1 Type unique

```relang
type Failure {
  id: String
  kind: FailureKind
  sourceId: String?
  failedAt: Timestamp
  execution: ExecutionRef
  cause: Failure?       // wrapping cause (single)
  causes: [Failure]?    // aggregation causes (multiple)
}

type ExecutionRef {
  executionId: String
  functionName: String?
  parentExecutionId: String?
}

sealed FailureKind
type ActionFailed      : FailureKind { actionType: String, error: ActionError, attempts: [Attempt] }
type ActionTimedOut    : FailureKind { actionType: String, timeout: Duration, attempts: [Attempt] }
type FunctionFailed    : FailureKind { functionName: String, childExecutionId: String }
type FunctionTimedOut  : FailureKind { functionName: String, timeout: Duration }
type FunctionCancelled : FailureKind { functionName: String, cancelledAt: Timestamp }
type AllFailed         : FailureKind { operator: String, branchCount: Int }
```

`error` dans `ActionFailed` est specialise par contrat de famille d'action (ex: `HttpError`).

Invariants normatifs:

1. `cause` et `causes` ne peuvent pas etre renseignes en meme temps.
2. `FunctionFailed` exige `cause != none`.
3. `AllFailed` exige `causes != none` et `causes.length > 0`.
4. Les erreurs feuille (`ActionFailed`, `ActionTimedOut`, `FunctionTimedOut`, `FunctionCancelled`) ont `cause == none` et `causes == none`.
5. Pour `AllFailed`, l'ordre de `causes` suit l'ordre lexical des branches dans le code source.

Contrat de famille d'action:

1. `ActionError` est un type scelle propre a la famille (`HttpError`, `DbError`, etc.).
2. Pour `ActionFailed`, `kind.error` appartient a cette famille.

Regles de propagation (inline vs distribue):

1. Appel inline `f(...)`: meme execution, la `Failure` se propage telle quelle (pas de wrapper).
2. Appel distribue `await spawn f(...)`: frontiere d'execution, la `Failure` observee par le parent est `FunctionFailed` avec `cause = failure enfant`.

Regle de terminaison racine:

1. Une `Failure` non geree a la racine termine l'execution en etat `Failed(Failure)`.
2. Aucun mecanisme d'exception hors modele langage n'est requis pour la semantique ReLang.

Frontiere langage/runtime pour le diagnostic:

1. `Failure` est le contrat langage (portable, stable, serializable).
2. Les details machine (host, worker, pod, region, trace, etc.) appartiennent au runtime et non au type langage.
3. Le runtime peut exposer ces details via un rapport separe, relie par `failure.id`.

### 7.2 Separation metier/infrastructure

1. Erreurs metier: valeurs de domaine (`Declined`, `InsufficientFunds`, etc.), gerees par `match`.
2. Erreurs d'infra/execution: toujours via `Failure`.

## 8. Temps et determinisme

### 8.1 Horloge naturelle (`now()`)

1. `now()` lit l'horloge UTC courante de la machine qui execute l'instruction.
2. ReLang n'impose pas de replay strict base sur le temps; il repose sur snapshots d'etat.
3. Si la valeur retournee par `now()` est stockee dans une variable avant checkpoint, elle est restauree a l'identique apres resume.
4. Un nouvel appel a `now()` apres resume lit le temps courant (donc potentiellement different).
5. Les timers utilisent une echeance absolue (`fireAt`) persistee dans l'etat.

Contraintes d'implementation recommandees:

1. Utiliser UTC pour toute valeur `Timestamp`.

### 8.2 Aleatoire

`random*` est deterministe (seed derive de `self.id` + compteur logique) et snapshot-safe.

## 9. Regles de checkpoint

Checkpoint obligatoire a:

1. `await` sur awaitable non resolu,
2. transitions de statut d'awaitable observees par runtime,
3. avant suspension longue (`receive`, timer non elu).

Contenu snapshot:

1. locals/pile/PC,
2. table awaitables + resolutions,
3. contexte `self`,
4. metadonnees de version.

## 10. EBNF minimale (v0.1)

```ebnf
Program        = { Decl } ;
Decl           = TypeDecl | SealedDecl | FnDecl | ImportDecl ;

ImportDecl      = "import" ImportSpec "from" ModuleRef ;
ImportSpec      = "*" | "*" "as" Ident | Ident { "," Ident } ;

TypeDecl        = "type" Ident "{" { FieldDecl } "}" ;
SealedDecl      = "sealed" Ident ;
FieldDecl       = Ident ":" Type [ "=" Expr ] ;

FnDecl          = { Annotation } "fn" Ident "(" [ Params ] ")" [ ":" Type ] FnBody ;
Params          = Param { "," Param } ;
Param           = Ident ":" Type [ "=" Expr ] ;
FnBody          = Block | "=" Expr ;

Block           = "{" { Stmt } [ Expr ] "}" ;
Stmt            = LetStmt | AssignStmt | ReturnStmt | ExprStmt | IfStmt | MatchStmt
                | ForStmt | WhileStmt | BreakStmt | ContinueStmt ;

LetStmt         = "let" Pattern "=" Expr ;
AssignStmt      = LValue "=" Expr ;
ReturnStmt      = "return" Expr ;
ExprStmt        = Expr ;

IfStmt          = "if" Expr Block [ "else" Block ] ;
MatchStmt       = "match" Expr "{" { MatchArm } "}" ;
MatchArm        = Pattern "->" (Block | Expr) ;

ForStmt         = "for" Pattern "in" Expr Block ;
WhileStmt       = "while" Expr Block ;

Expr            = AwaitExpr | SpawnExpr | BinaryExpr | UnaryExpr | PrimaryExpr ;
AwaitExpr       = "await" Expr [ "!" ] ;
SpawnExpr       = "spawn" CallExpr ;
CallExpr        = PrimaryExpr "(" [ Args ] ")" ;
Args            = Arg { "," Arg } ;
Arg             = [ Ident ":" ] Expr ;

PrimaryExpr     = Literal | Ident | "(" Expr ")" | TupleExpr | ListExpr | MatchExpr | IfExpr ;
TupleExpr       = "(" Expr "&" Expr { "&" Expr } ")" ;
ListExpr        = "[" [ Expr { "," Expr } ] "]" ;

Type            = SimpleType | OptionalType | ListType | MapType | ProductType | UnionType ;
SimpleType      = Ident ;
OptionalType    = Type "?" ;
ListType        = "[" Type "]" ;
MapType         = "Map" "<" Type "," Type ">" ;
ProductType     = Type "&" Type { "&" Type } ;
UnionType       = Type "|" Type { "|" Type } ;
```

## 11. Exemple court canonique

```relang
type Order { id: String, total: Int }
type Receipt { id: String }

fn charge(order: Order): *Receipt {
  payments.charge(order)
}

fn process(order: Order): Receipt | Failure {
  let payment = await charge(order)
  match payment {
    r: Receipt -> r
    f: Failure -> f
  }
}
```

## 12. Hors scope v0.1

1. Function types first-class.
2. Overloading utilisateur d'operateurs.
3. Streaming gRPC/HTTP.
4. Multi-runtime consistency model distribue detaille (a specifier v0.2+).
5. Syntaxe de retry en langage (`retry { ... }`, etc.).

Retry en v0.1:

1. gere par politiques runtime/configuration par famille d'actions,
2. visible via metadonnees d'echec (`attempts` dans `ActionFailed` / `ActionTimedOut`),
3. sans mot-cle dedie dans le noyau.

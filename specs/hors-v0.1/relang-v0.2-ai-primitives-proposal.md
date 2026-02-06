# ReLang v0.2 - AI Primitives Proposal

Status: Draft proposal  
Scope: v0.2+ (hors noyau v0.1)

## 1. Objectif

Proposer un petit set de primitives IA de haut niveau pour ReLang, afin de simplifier le developpement d'applications agents en production, tout en conservant:

1. le modele durable snapshot-first,
2. `await` explicite,
3. le contrat unique d'echec `Failure`.

## 2. Principes de design

1. Peu de primitives, mais tres utiles.
2. Primitives typed-by-default (eviter JSON fragile partout).
3. Compatibilite native avec policies runtime (budget, retry, timeout, rate-limit).
4. Integrables avec `spawn`, `and`, `or`, `timer`, `receive`.
5. Separation langage/runtime: details fournisseur dans le runtime, contrat stable dans le langage.

## 3. Primitives proposees

### 3.1 `ai.text`

Signature cible:

```relang
ai.text(prompt: PromptInput, options: AiTextOptions?): *AiText
```

Role:

1. generation textuelle generaliste,
2. reformulation, resume, extraction libre.

Pourquoi utile:

1. point d'entree minimal pour cas LLM classiques,
2. standardise telemetrie/couts/tokens.

Pourquoi mieux qu'un appel HTTP/gRPC brut:

1. contrat uniforme entre providers,
2. meme schema de `Failure` et retry policy,
3. moins de glue code (headers, parsing, retries, metriques).

### 3.2 `ai.structured<T>`

Signature cible:

```relang
ai.structured<T>(prompt: PromptInput, schema: TypeRef<T>, options: AiStructuredOptions?): *T
```

Role:

1. produire une sortie structuree validee,
2. mapper directement vers un type ReLang.

Pourquoi utile:

1. supprime une grande partie du code de parsing/validation,
2. rend les workflows plus robustes et explicites.

Pourquoi mieux qu'un appel HTTP/gRPC brut:

1. evite le pattern fragile "JSON libre + parser ad hoc",
2. erreurs de schema remontees de facon standard (`AiOutputInvalid`),
3. facilite la composition type-safe dans le reste du code.

### 3.3 `ai.embed`

Signature cible:

```relang
ai.embed(input: String | [String], options: AiEmbedOptions?): *Vector | *[Vector]
```

Role:

1. generation d'embeddings pour RAG/recherche semantique,
2. support mono et batch.

Pourquoi utile:

1. primitive fondamentale pour index vectoriels,
2. permet des pipelines RAG simples et homogènes.

Pourquoi mieux qu'un appel HTTP/gRPC brut:

1. normalise dimensions/vecteurs et erreurs de modele,
2. simplifie les batchs et leur observabilite,
3. meilleure portabilite multi-provider.

### 3.4 `ai.tools`

Signature cible:

```relang
ai.tools(prompt: PromptInput, tools: [ToolSpec], options: AiToolsOptions?): *AiTurn
```

Role:

1. execute un tour de function-calling,
2. retourne soit une reponse finale, soit une demande d'outil structuree.

Pourquoi utile:

1. base naturelle des agents outilles,
2. clarifie la boucle "raisonner -> appeler outil -> continuer".

Pourquoi mieux qu'un appel HTTP/gRPC brut:

1. schema unifie des tool-calls (independant vendor),
2. validation stricte des arguments outil,
3. facilite les patterns d'orchestration (boucle agent) sans boilerplate.

### 3.5 `ai.classify`

Signature cible:

```relang
ai.classify(input: String, labels: [String], options: AiClassifyOptions?): *Classification
```

Role:

1. routing/intention/priorisation,
2. classification rapide a faible cout.

Pourquoi utile:

1. cas ultra frequent en app metier,
2. simplifie les gatekeepers avant workflows lourds.

Pourquoi mieux qu'un appel HTTP/gRPC brut:

1. contrat de sortie stable (`label`, `confidence`),
2. fallback/policy runtime homogenes,
3. moins de duplication dans chaque service.

### 3.6 `ai.rerank`

Signature cible:

```relang
ai.rerank(query: String, docs: [RerankDoc], options: AiRerankOptions?): *[RankedDoc]
```

Role:

1. re-ordonner des documents candidats,
2. ameliorer precision RAG avant generation.

Pourquoi utile:

1. augmente la qualite des contextes injectes au LLM,
2. limite l'hallucination en recentrant sur les bons docs.

Pourquoi mieux qu'un appel HTTP/gRPC brut:

1. resultat normalize et simple a enchaîner,
2. composition directe avec `ai.embed` + recherche vectorielle,
3. policy commune sur latence/cout.

## 4. Types de sortie minimaux

```relang
type AiText {
  text: String
  finishReason: String?
  usage: AiUsage?
}

type AiUsage {
  inputTokens: Int
  outputTokens: Int
  totalTokens: Int
  estimatedCost: Float?
}

type AiTurn =
  | AiFinal { message: String, usage: AiUsage? }
  | AiToolCall { name: String, arguments: Json, callId: String, usage: AiUsage? }

type Classification {
  label: String
  confidence: Float?
  scores: Map<String, Float>?
}

type RerankDoc {
  id: String
  text: String
  metadata: Json?
}

type RankedDoc {
  id: String
  score: Float
  metadata: Json?
}
```

## 5. Modele d'echec

Les primitives IA restent des effets ReLang standards:

```relang
await ai.text(...) : AiText | Failure
```

Recommendation v0.2:

1. `ActionFailed { actionType: "ai", error: AiError }`,
2. erreurs IA typiques:
   1. `AiRateLimited`,
   2. `AiModelUnavailable`,
   3. `AiContextTooLarge`,
   4. `AiOutputInvalid`,
   5. `AiSafetyBlocked`,
   6. `AiBudgetExceeded`.

## 6. Policies runtime recommandees

A ne pas encoder en primitive langage, mais en policy runtime:

1. model routing (`cheap`, `balanced`, `quality`),
2. budget tokens/cout par execution,
3. retries/backoff,
4. timeouts,
5. redaction PII,
6. cache de prompts/reponses.

## 7. Pourquoi c'est structurellement superieur a HTTP/gRPC brut

### 7.1 Gains developpeur

1. moins de boilerplate (auth, headers, payload vendor),
2. types de sortie coherents,
3. erreurs homogenes.

### 7.2 Gains runtime

1. observabilite cross-provider uniforme,
2. gouvernance cout/latence centralisee,
3. migration provider sans toucher tout le code applicatif.

### 7.3 Gains langage

1. composition native avec `spawn`/`and`/`or`/`timer`/`receive`,
2. semantics de reprise identiques aux autres effets,
3. meme contrat de failure et de causalite.

## 8. Exemples

### 8.1 Extraction structuree

```relang
type TicketSummary {
  intent: String
  urgency: String
  customerId: String?
}

let summary = (await ai.structured<TicketSummary>(
  prompt: "Analyse ce ticket: ${ticketText}",
  schema: TicketSummary
))!
```

### 8.2 Agent outille (tour unique)

```relang
let turn = await ai.tools(
  prompt: "Trouve l'etat de la commande ORD-42",
  tools: [toolGetOrder, toolGetShipment]
)

match turn {
  t: AiFinal -> reply(t.message)
  c: AiToolCall -> executeTool(c)
  f: Failure -> handleFailure(f)
}
```

### 8.3 RAG simple

```relang
let qv = (await ai.embed(question))!
let candidates = (await vector.search(index: "kb", query: qv, topK: 40))!
let ranked = (await ai.rerank(question, candidates))!
let answer = (await ai.text(buildPrompt(question, ranked)))!
```

## 9. Scope v0.2 propose

Inclusion recommandee v0.2:

1. `ai.text`,
2. `ai.structured<T>`,
3. `ai.embed`,
4. `ai.tools`.

Optionnel v0.2, sinon v0.3:

1. `ai.classify`,
2. `ai.rerank`.

## 10. Non-objectifs

1. token streaming au niveau langage (possible via runtime events),
2. fine-tuning lifecycle,
3. semantics multi-tour agent complete dans le noyau.

---

*End of proposal*

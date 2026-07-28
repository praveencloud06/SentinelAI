# SentinelAI Knowledge Service V2 - Sequence Diagrams

## 1. Context Retrieval Flow

### Sequence Diagram: Context Retrieval API

```
┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐    ┌──────────────────┐
│   Core      │    │  Knowledge  │    │  Context Retrieval     │    │  Hybrid Search   │
│  Service    │    │   Service   │    │       Engine           │    │     Service      │
└──────┬──────┘    └──────┬──────┘    └───────────┬─────────────┘    └────────┬─────────┘
       │                  │                        │                         │
       │ POST /api/context/retrieve                  │                         │
       │ {logs, service, environment}               │                         │
       │───────────────────>                        │                         │
       │                  │                        │                         │
       │                  │  1. Entity Extraction  │                         │
       │                  │──────────────────────>│                         │
       │                  │                        │                         │
       │                  │  2. Query Expansion    │                         │
       │                  │                        │ - Generate embedding    │
       │                  │                        │                         │
       │                  │  3. Hybrid Search      │                         │
       │                  │<──────────────────────│                         │
       │                  │                        │                         │
       │                  │  4. Vector Search      │                         │
       │                  │─────────────────────────────────────────────────>│
       │                  │                        │                         │
       │                  │                        │         Vector results   │
       │                  │                        │<────────────────────────│
       │                  │                        │                         │
       │                  │  5. SQL Search         │                         │
       │                  │─────────────────────────────────────────────────>│
       │                  │                        │                         │
       │                  │                        │         SQL results      │
       │                  │                        │<────────────────────────│
       │                  │                        │                         │
       │                  │  6. Graph Search        │                         │
       │                  │─────────────────────────────────────────────────>│
       │                  │                        │                         │
       │                  │                        │         Graph results    │
       │                  │                        │<────────────────────────│
       │                  │                        │                         │
       │                  │  7. Timeline Search     │                         │
       │                  │─────────────────────────────────────────────────>│
       │                  │                        │                         │
       │                  │                        │         Timeline results │
       │                  │                        │<────────────────────────│
       │                  │                        │                         │
       │                  │  8. Merge Candidates    │                         │
       │                  │──────────────────────>│                         │
       │                  │                        │                         │
       │                  │  9. Rank Evidence       │                         │
       │                  │                        │ - Calculate signals     │
       │                  │                        │ - Combine scores        │
       │                  │                        │ - Apply thresholds      │
       │                  │                        │                         │
       │                  │  10. Build Response     │                         │
       │                  │<──────────────────────│                         │
       │                  │                        │                         │
       │  ContextRetrievalResponse                     │                         │
       │ {evidence, metadata}                         │                         │
       │<──────────────────                           │                         │
       │                  │                        │                         │
```

## 2. Embedding Generation Flow

### Sequence Diagram: Async Embedding Generation

```
┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐    ┌──────────────┐    ┌──────────┐
│   Sync      │    │  Knowledge  │    │  Embedding Generation   │    │   AI Engine   │    │  Vector  │
│  Service    │    │   Service   │    │       Service           │    │               │    │   DB     │
└──────┬──────┘    └──────┬──────┘    └───────────┬─────────────┘    └───────┬───────┘    └────┬─────┘
       │                  │                        │                         │                  │
       │ POST /api/sync    │                        │                         │                  │
       │──────────────────>                        │                         │                  │
       │                  │                        │                         │                  │
       │                  │  1. Save Entity        │                         │                  │
       │                  │  (Jira, Commit, etc.)  │                         │                  │
       │                  │                        │                         │                  │
       │                  │  2. Set Status = PENDING│                         │                  │
       │                  │                        │                         │                  │
       │                  │  3. Commit Transaction  │                         │                  │
       │                  │                        │                         │                  │
       │  SyncResponse     │                        │                         │                  │
       │<──────────────────                        │                         │                  │
       │                  │                        │                         │                  │
       │                  │  4. @Async Trigger      │                         │                  │
       │                  │──────────────────────>│                         │                  │
       │                  │                        │                         │                  │
       │                  │                        │  5. Extract Content     │                  │
       │                  │                        │  (message, description) │                  │
       │                  │                        │                         │                  │
       │                  │                        │  6. Chunk if Large      │                  │
       │                  │                        │                         │                  │
       │                  │                        │  7. Call AI Engine      │                  │
       │                  │                        │────────────────────────>│                  │
       │                  │                        │                         │                  │
       │                  │                        │                         │  8. Generate     │
       │                  │                        │                         │     Embedding    │
       │                  │                        │                         │                  │
       │                  │                        │         Embedding vector │                  │
       │                  │                        │<────────────────────────│                  │
       │                  │                        │                         │                  │
       │                  │                        │  9. Store in DB         │                  │
       │                  │                        │──────────────────────────────────────────>│
       │                  │                        │                         │                  │
       │                  │                        │  10. Set Status =       │                  │
       │                  │                        │      COMPLETED           │                  │
       │                  │                        │                         │                  │
       │                  │                        │  [If Failed]            │                  │
       │                  │                        │  11. Set Status =       │                  │
       │                  │                        │      FAILED             │                  │
       │                  │                        │  12. Schedule Retry     │                  │
       │                  │                        │                         │                  │
```

## 3. RCA Flow Evolution

### Sequence Diagram: Current vs New RCA Flow

#### Current Flow
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────────┐
│   Core      │    │  Knowledge  │    │   Prompt    │    │    AI    │
│  Service    │    │   Service   │    │  Builder    │    │  Engine  │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘    └────┬─────┘
       │                  │                  │                │
       │ 1. POST /api/timeline                │                │
       │──────────────────>                  │                │
       │                  │                  │                │
       │ 2. POST /api/deployments             │                │
       │──────────────────>                  │                │
       │                  │                  │                │
       │ 3. POST /api/jira/issues             │                │
       │──────────────────>                  │                │
       │                  │                  │                │
       │ 4. POST /api/releases                │                │
       │──────────────────>                  │                │
       │                  │                  │                │
       │ All metadata    │                  │                │
       │<──────────────────                  │                │
       │                  │                  │                │
       │ 5. Build large prompt              │                │
       │ (includes all metadata)             │                │
       │────────────────────────────────────>│                │
       │                  │                  │                │
       │                  │  6. Send to AI   │                │
       │                  │──────────────────────────────────>│
       │                  │                  │                │
       │                  │                  │  7. RCA with   │
       │                  │                  │     noise      │
       │                  │                  │<───────────────│
       │                  │                  │                │
       │ 8. RCA Response  │                  │                │
       │<────────────────────────────────────│                │
       │                  │                  │                │
```

#### New Flow
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────────┐
│   Core      │    │  Knowledge  │    │   Prompt    │    │    AI    │
│  Service    │    │   Service   │    │  Builder    │    │  Engine  │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘    └────┬─────┘
       │                  │                  │                │
       │ 1. POST /api/context/retrieve      │                │
       │ {logs, service, environment}       │                │
       │──────────────────>                  │                │
       │                  │                  │                │
       │                  │  2. Hybrid Search│                │
       │                  │  3. Ranking      │                │
       │                  │  4. Filter       │                │
       │                  │                  │                │
       │ Only Top 10      │                  │                │
       │ ranked evidence  │                  │                │
       │<──────────────────                  │                │
       │                  │                  │                │
       │ 5. Build optimized prompt          │                │
       │ (only relevant evidence)            │                │
       │────────────────────────────────────>│                │
       │                  │                  │                │
       │                  │  6. Send to AI   │                │
       │                  │──────────────────────────────────>│
       │                  │                  │                │
       │                  │                  │  7. High-quality│
       │                  │                  │     RCA        │
       │                  │                  │<───────────────│
       │                  │                  │                │
       │ 8. RCA Response  │                  │                │
       │<────────────────────────────────────│                │
       │                  │                  │                │
```

## 4. Hybrid Search Flow

### Sequence Diagram: Hybrid Search Components

```
┌─────────────────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Context Retrieval       │    │   Vector     │    │     SQL      │    │    Graph     │
│       Engine            │    │   Search     │    │   Search     │    │   Search     │
└──────────┬──────────────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
           │                         │                    │                    │
           │ 1. Search All Sources    │                    │                    │
           │──────────────────────────────────────────────────────────────────>│
           │                         │                    │                    │
           │                         │  2. Vector Search  │                    │
           │                         │──────────────────>│                    │
           │                         │                    │                    │
           │                         │                    │  3. SQL Search     │
           │                         │                    │──────────────────>│
           │                         │                    │                    │
           │                         │                    │                    │  4. Graph Search
           │                         │                    │                    │──────────────────>
           │                         │                    │                    │
           │                         │                    │                    │  5. Timeline Search
           │                         │                    │                    │──────────────────>
           │                         │                    │                    │
           │                         │  Vector results    │                    │
           │                         │<──────────────────│                    │
           │                         │                    │                    │
           │                         │                    │  SQL results       │
           │                         │                    │<──────────────────│
           │                         │                    │                    │
           │                         │                    │                    │  Graph results
           │                         │                    │                    │<──────────────────
           │                         │                    │                    │
           │                         │                    │                    │  Timeline results
           │                         │                    │                    │<──────────────────
           │                         │                    │                    │
           │  6. Merge Results       │                    │                    │
           │<──────────────────────────────────────────────────────────────────│
           │                         │                    │                    │
           │  7. Deduplicate         │                    │                    │
           │  8. Apply Filters       │                    │                    │
           │                         │                    │                    │
```

## 5. Ranking Flow

### Sequence Diagram: Ranking Engine

```
┌─────────────────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Context Retrieval       │    │   Ranking    │    │   Signal     │    │  Evidence     │
│       Engine            │    │   Engine     │    │  Calculators │    │  Builder      │
└──────────┬──────────────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
           │                         │                    │                    │
           │ 1. Rank Candidates      │                    │                    │
           │──────────────────────────────────────────────────────────────────>│
           │                         │                    │                    │
           │                         │  2. For each candidate:                  │
           │                         │─────────────────────────────────────────>│
           │                         │                    │                    │
           │                         │                    │  3. Vector Similarity│
           │                         │                    │──────────────────>│
           │                         │                    │                    │
           │                         │                    │  4. Relationship     │
           │                         │                    │──────────────────>│
           │                         │                    │                    │
           │                         │                    │  5. Timeline        │
           │                         │                    │──────────────────>│
           │                         │                    │                    │
           │                         │                    │  6. Service Match    │
           │                         │                    │──────────────────>│
           │                         │                    │                    │
           │                         │                    │  7. Environment Match│
           │                         │                    │──────────────────>│
           │                         │                    │                    │
           │                         │                    │  8. Recency         │
           │                         │                    │──────────────────>│
           │                         │                    │                    │
           │                         │                    │  9. Exact Match     │
           │                         │                    │──────────────────>│
           │                         │                    │                    │
           │                         │  10. Combine Signals│                    │
           │                         │  - Apply weights    │                    │
           │                         │  - Calculate final   │                    │
           │                         │<─────────────────────────────────────────│
           │                         │                    │                    │
           │                         │  11. Build Evidence │                    │
           │                         │─────────────────────────────────────────>│
           │                         │                    │                    │
           │                         │  12. Filter by threshold                │
           │                         │  13. Sort by score                      │
           │                         │  14. Limit to top N                      │
           │                         │                    │                    │
           │  Ranked Evidence        │                    │                    │
           │<──────────────────────────────────────────────────────────────────│
           │                         │                    │                    │
```

## 6. Engineering Context Explorer Flow

### Sequence Diagram: Direct UI Usage

```
┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐    ┌──────────────────┐
│    UI       │    │  Knowledge  │    │  Engineering Context    │    │  Hybrid Search   │
│  Browser    │    │   Service   │    │       Explorer          │    │     Service      │
└──────┬──────┘    └──────┬──────┘    └───────────┬─────────────┘    └────────┬─────────┘
       │                  │                        │                         │
       │ 1. Paste logs    │                        │                         │
       │    or query      │                        │                         │
       │──────────────────>                        │                         │
       │                  │                        │                         │
       │                  │ 2. POST /api/context/explorer                  │
       │                  │──────────────────────>│                         │
       │                  │                        │                         │
       │                  │                        │ 3. Parse search type   │
       │                  │                        │  - SEMANTIC            │
       │                  │                        │  - SIMILAR_INCIDENTS   │
       │                  │                        │  - RELATED_JIRA        │
       │                  │                        │  - DEPLOYMENT_HISTORY  │
       │                  │                        │  - RUNBOOKS            │
       │                  │                        │                         │
       │                  │                        │ 4. Execute search      │
       │                  │                        │────────────────────────>│
       │                  │                        │                         │
       │                  │                        │ 5. Rank results        │
       │                  │                        │<────────────────────────│
       │                  │                        │                         │
       │                  │ 6. Build response       │                         │
       │                  │<──────────────────────│                         │
       │                  │                        │                         │
       │ 7. Display evidence                         │                         │
       │    - Type icons                            │                         │
       │    - Scores                                │                         │
       │    - Reasons                               │                         │
       │    - Links                                 │                         │
       │<──────────────────                         │                         │
       │                  │                        │                         │
```

## 7. Backfill Embeddings Flow

### Sequence Diagram: Backfill Existing Data

```
┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐    ┌──────────────┐
│   Admin     │    │  Knowledge  │    │  Embedding Generation   │    │   AI Engine   │
│  Interface  │    │   Service   │    │       Service           │    │               │
└──────┬──────┘    └──────┬──────┘    └───────────┬─────────────┘    └───────┬───────┘
       │                  │                        │                         │
       │ 1. Trigger backfill                        │                         │
       │    POST /api/admin/backfill-embeddings     │                         │
       │──────────────────>                        │                         │
       │                  │                        │                         │
       │                  │ 2. Query entities with   │                         │
       │                  │    embeddingStatus = NULL│                         │
       │                  │    or FAILED             │                         │
       │                  │                        │                         │
       │                  │ 3. Batch processing      │                         │
       │                  │──────────────────────>│                         │
       │                  │                        │                         │
       │                  │                        │ 4. For each entity:     │
       │                  │                        │    - Extract content    │
       │                  │                        │    - Chunk if needed    │
       │                  │                        │                         │
       │                  │                        │ 5. Call AI Engine      │
       │                  │                        │────────────────────────>│
       │                  │                        │                         │
       │                  │                        │         Embedding vector │
       │                  │                        │<────────────────────────│
       │                  │                        │                         │
       │                  │                        │ 6. Store in DB          │
       │                  │                        │ 7. Update status        │
       │                  │                        │                         │
       │                  │ 8. Progress update      │                         │
       │<──────────────────                        │                         │
       │                  │                        │                         │
       │ 9. Display progress │                        │                         │
       │    - Total processed                        │                         │
       │    - Success rate                          │                         │
       │    - Failed count                           │                         │
       │<──────────────────                         │                         │
       │                  │                        │                         │
```

## 8. Webhook Ingestion with Embedding

### Sequence Diagram: Enhanced Webhook Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐
│  External   │    │  Knowledge  │    │   Sync      │    │  Embedding Generation   │
│   System    │    │   Service   │    │  Service    │    │       Service           │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘    └───────────┬─────────────┘
       │                  │                  │                        │
       │ 1. Webhook event │                  │                        │
       │ (GitHub, Jira, etc.)                │                        │
       │──────────────────>                  │                        │
       │                  │                  │                        │
       │                  │ 2. Validate and parse                      │
       │                  │──────────────────────────────────────────>│
       │                  │                  │                        │
       │                  │ 3. Save entity to DB │                        │
       │                  │──────────────────────────────────────────>│
       │                  │                  │                        │
       │                  │                  │ 4. Set embeddingStatus = │
       │                  │                  │    PENDING               │
       │                  │                  │                        │
       │                  │                  │ 5. Commit transaction    │
       │                  │                  │                        │
       │                  │ 6. 202 Accepted   │                        │
       │<──────────────────                  │                        │
       │                  │                  │                        │
       │                  │                  │ 7. @Async trigger       │
       │                  │                  │────────────────────────>│
       │                  │                  │                        │
       │                  │                  │ 8. Generate embedding    │
       │                  │                  │                        │
       │                  │                  │ 9. Store and update     │
       │                  │                  │    status = COMPLETED    │
       │                  │                  │                        │
```

## 9. AI Engine Embedding Endpoint

### Sequence Diagram: AI Engine Integration

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────────────┐
│  Knowledge  │    │   AI Engine │    │   Provider  │    │  External    │
│   Service   │    │             │    │   Router    │    │  Provider    │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘    └──────┬───────┘
       │                  │                  │                  │
       │ 1. POST /api/embeddings               │                  │
       │    {text, model}                      │                  │
       │──────────────────────────────────────>│                  │
       │                  │                  │                  │
       │                  │ 2. Route to provider│                  │
       │                  │──────────────────>│                  │
       │                  │                  │                  │
       │                  │                  │ 3. Select provider │
       │                  │                  │    - OpenRouter   │
       │                  │                  │    - OpenAI       │
       │                  │                  │    - Ollama       │
       │                  │                  │    - Azure OpenAI │
       │                  │                  │                  │
       │                  │                  │ 4. Call provider  │
       │                  │                  │─────────────────>│
       │                  │                  │                  │
       │                  │                  │ 5. Generate embedding
       │                  │                  │                  │
       │                  │                  │ 6. Return vector  │
       │                  │                  │<─────────────────│
       │                  │                  │                  │
       │                  │ 7. Return embedding vector            │
       │                  │<──────────────────│                  │
       │                  │                  │                  │
       │ 8. Embedding vector                  │                  │
       │<──────────────────────────────────────│                  │
       │                  │                  │                  │
```

## 10. Error Handling and Retry Flow

### Sequence Diagram: Embedding Failure Retry

```
┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐    ┌──────────────┐
│   Sync      │    │  Knowledge  │    │  Embedding Generation   │    │  Retry       │
│  Service    │    │   Service   │    │       Service           │    │  Scheduler   │
└──────┬──────┘    └──────┬──────┘    └───────────┬─────────────┘    └──────┬───────┘
       │                  │                        │                         │
       │ 1. Save entity   │                        │                         │
       │──────────────────>                        │                         │
       │                  │                        │                         │
       │                  │ 2. @Async trigger      │                         │
       │                  │──────────────────────>│                         │
       │                  │                        │                         │
       │                  │                        │ 3. Call AI Engine      │
       │                  │                        │────────────────────────>│
       │                  │                        │                         │
       │                  │                        │ 4. ERROR!              │
       │                  │                        │<────────────────────────│
       │                  │                        │                         │
       │                  │                        │ 5. Set status = FAILED │
       │                  │                        │ 6. Increment retry count│
       │                  │                        │                         │
       │                  │                        │ 7. Schedule retry       │
       │                  │                        │────────────────────────>│
       │                  │                        │                         │
       │                  │                        │                         │ 8. Wait (backoff)
       │                  │                        │                         │
       │                  │                        │                         │ 9. Retry trigger
       │                  │                        │<────────────────────────│
       │                  │                        │                         │
       │                  │                        │ 10. Retry generation    │
       │                  │                        │                         │
       │                  │                        │ [If max attempts reached]
       │                  │                        │ 11. Set status = PERMANENTLY_FAILED
       │                  │                        │ 12. Alert admin          │
       │                  │                        │                         │
```

## Key Observations from Sequence Diagrams

1. **Async Processing**: Embedding generation is completely decoupled from sync operations
2. **Hybrid Search**: Multiple search strategies run in parallel and merge results
3. **Ranking**: Multiple signals are calculated and combined for final scoring
4. **Backward Compatibility**: Existing APIs remain unchanged, new APIs are additive
5. **Error Resilience**: Retry mechanisms handle transient failures
6. **Direct UI Usage**: Engineering Context Explorer works independently of RCA flow
7. **AI Engine Abstraction**: Knowledge Service never directly depends on AI SDKs
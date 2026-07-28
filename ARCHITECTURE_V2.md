# SentinelAI Knowledge Service V2 - Engineering Context Platform

**Document Version:** 2.1  
**Last Updated:** 2026-07-23  
**Status:** BFF Pattern Implemented, V2 Features In Progress

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current State Analysis](#current-state-analysis)
   - [System Components - Implementation Status](#system-components---implementation-status)
   - [Current Architecture Flow](#current-architecture-flow)
   - [Existing Architecture](#existing-architecture)
   - [Current Problems](#current-problems)
3. [V2 Architecture Vision](#v2-architecture-vision)
   - [Core Philosophy](#core-philosophy)
   - [Backend-for-Frontend (BFF) Pattern](#backend-for-frontend-bff-pattern---implemented)
4. [High-Level Architecture](#high-level-architecture)
   - [Complete System Architecture with BFF Pattern](#complete-system-architecture-with-bff-pattern)
5. [New Package Structure](#new-package-structure)
6. [Database Schema Changes](#database-schema-changes)
7. [Entity Models](#entity-models)
8. [Embedding Design](#embedding-design)
9. [Context Retrieval Engine](#context-retrieval-engine)
10. [Ranking Algorithm](#ranking-algorithm)
11. [Context Retrieval API](#context-retrieval-api)
12. [Engineering Context Explorer API](#engineering-context-explorer-api)
13. [RCA Flow Evolution](#rca-flow-evolution)
14. [Vector Search with pgvector](#vector-search-with-pgvector)
15. [Chunking Strategy](#chunking-strategy)
16. [Migration Strategy](#migration-strategy)
17. [Backward Compatibility Strategy](#backward-compatibility-strategy)
18. [Implementation Plan](#implementation-plan)
19. [Future Scalability](#future-scalability)
20. [Production Considerations](#production-considerations)
21. [Configuration Examples](#configuration-examples)
22. [Current Deployment Guide](#current-deployment-guide)
23. [Conclusion](#conclusion)

---

## Executive Summary

The Knowledge Service V2 transforms from a simple metadata repository into an intelligent **Engineering Context Platform**. The platform is responsible for finding, ranking, and filtering engineering context, ensuring that the Core Service receives only relevant, high-quality engineering evidence for Root Cause Analysis (RCA).

### Current Implementation Status (2026-07-23)

**✅ COMPLETED:**
- Backend-for-Frontend (BFF) pattern implemented
- All UI requests route through SentinelAI Core (API Gateway)
- Engineering Context Explorer UI integrated with BFF layer
- Graceful degradation when services unavailable
- Complete demo data environment with 5 production incidents
- Log RCA and ELK Investigation features fully working

**⚠️ IN PROGRESS:**
- Knowledge Service V2 Context Retrieval endpoint
- Embedding generation infrastructure
- Vector search with pgvector
- Hybrid search and ranking engine

**🎯 NEXT PRIORITIES:**
1. Implement ContextRetrievalController in Knowledge Service
2. Add embedding generation service (async)
3. Create knowledge_embedding table with pgvector
4. Implement vector search service
5. Implement ranking engine with multi-signal scoring

### Architecture Highlights

The system now follows enterprise **Backend-for-Frontend (BFF)** architecture:

```
Browser UI → Core (API Gateway) → Microservices (Knowledge, AI Engine)
```

All three features route through Core:
- ✅ Log RCA
- ✅ ELK Investigation  
- ✅ Engineering Context Explorer

The V2 enhancement adds **semantic search** and **intelligent ranking** to the Knowledge Service, enabling AI-powered correlation of engineering evidence across GitHub, Jira, Confluence, Jenkins, and historical incidents.

## Current State Analysis

### System Components - Implementation Status

#### ✅ SentinelAI Core (BFF Layer) - COMPLETE
- **Location:** `SentinelAI-Core/`
- **Status:** Production-ready with BFF pattern implemented
- **Port:** 8080
- **Implemented Features:**
  - ✅ RCAController - Log RCA endpoint
  - ✅ ElkController - ELK Investigation endpoint
  - ✅ ContextController - Engineering Context Explorer BFF endpoint (NEW)
  - ✅ KnowledgeServiceClient with dual methods:
    - `retrieveContext()` - For RCA/ELK internal flows
    - `retrieveContextForUI()` - For Engineering Context Explorer (NEW)
  - ✅ Graceful degradation when Knowledge Service unavailable
  - ✅ Comprehensive error handling and logging

#### ✅ SentinelAI UI (React Frontend) - COMPLETE
- **Location:** `sentinelai-ui/`
- **Status:** Production-ready with all screens implemented
- **Port:** 3000
- **Implemented Features:**
  - ✅ Log RCA Screen (routes through Core)
  - ✅ ELK Investigation Screen (routes through Core)
  - ✅ Engineering Context Explorer Screen (routes through Core via BFF)
  - ✅ contextService.js updated to use Core API (localhost:8080)
  - ✅ package.json proxy configured for Core
  - ✅ All API calls route through BFF layer

#### ⚠️ Knowledge Service V2 - PARTIALLY COMPLETE
- **Location:** `SentinelAI-Knowledge-Service/`
- **Status:** Metadata APIs complete, Context Retrieval V2 pending
- **Port:** 8090
- **Implemented Features:**
  - ✅ SyncController - Webhook ingestion (GitHub, Jira, Confluence, Jenkins)
  - ✅ KnowledgeController - Metadata APIs (timeline, deployments, jira, etc.)
  - ✅ WebhookController - External webhook handling
  - ✅ All existing JPA repositories and entities
  - ✅ Relationship graph queries
  - ❌ ContextRetrievalController - **NOT YET IMPLEMENTED**
  - ❌ EmbeddingGenerationService - **NOT YET IMPLEMENTED**
  - ❌ Vector search infrastructure - **NOT YET IMPLEMENTED**
  - ❌ Hybrid search services - **NOT YET IMPLEMENTED**
  - ❌ Ranking engine - **NOT YET IMPLEMENTED**

#### ✅ AI Engine - COMPLETE (Basic)
- **Location:** `SentinelAI-Engine/`
- **Status:** Basic functionality working
- **Port:** 8000
- **Implemented Features:**
  - ✅ POST /api/rca - RCA generation (GPT-4/Claude)
  - ⚠️ POST /api/embeddings - **Needed for V2 but not yet implemented**

#### ✅ PostgreSQL Database - COMPLETE
- **Status:** Business tables complete, embeddings table pending
- **Port:** 5432
- **Implemented Tables:**
  - ✅ knowledge_repository
  - ✅ knowledge_commit
  - ✅ knowledge_pull_request
  - ✅ knowledge_jira_issue
  - ✅ knowledge_confluence_document
  - ✅ knowledge_jenkins_deployment
  - ✅ knowledge_relationship
  - ❌ knowledge_embedding - **NOT YET CREATED**

#### ✅ Elasticsearch (ELK Stack) - COMPLETE
- **Status:** Production-ready
- **Port:** 9200
- **Implemented Features:**
  - ✅ Log storage and indexing
  - ✅ Full-text search
  - ✅ Query DSL support
  - ✅ Integration with ELK Investigation feature

### Current Architecture Flow

**Working Flows:**
1. ✅ **Log RCA:** UI → Core → Knowledge Service (metadata) → AI Engine → RCA
2. ✅ **ELK Investigation:** UI → Core → Elasticsearch + Knowledge Service (metadata) → AI Engine → Analysis
3. ⚠️ **Engineering Context Explorer:** UI → Core → Knowledge Service → **Returns empty (endpoint not implemented)**

### Existing Architecture
- **Knowledge Service**: Synchronizes metadata from GitHub, Jira, Confluence, Jenkins
- **Core Service**: Calls Knowledge Service APIs (timeline, deployments, jira, relationships)
- **Current Flow**: Core receives ALL metadata → Appends to LLM prompt → AI generates RCA

### Current Problems
1. **Large Prompts**: All metadata is sent to LLM regardless of relevance
2. **Irrelevant Information**: No filtering based on incident context
3. **Poor Ranking**: No semantic understanding of engineering evidence
4. **Lower RCA Quality**: AI overwhelmed with noise
5. **High Token Cost**: Unnecessary data increases costs
6. **No Semantic Retrieval**: Missing vector similarity search

## V2 Architecture Vision

### Core Philosophy
**Knowledge Service becomes the orchestration layer for engineering context retrieval**

- Core should NEVER receive unnecessary data
- Knowledge Service is responsible for finding, ranking, filtering
- Hybrid retrieval combines multiple signals
- Semantic understanding via embeddings
- Backward compatibility is mandatory

### Backend-for-Frontend (BFF) Pattern - ✅ IMPLEMENTED

The system now follows enterprise BFF architecture where the Core service acts as an API Gateway:

**Architecture Principle:**
```
Browser → Core (BFF/API Gateway) → Microservices (Knowledge, AI Engine)
```

**Benefits:**
- ✅ Single backend entry point for UI
- ✅ Centralized authentication and authorization
- ✅ No CORS issues (package.json proxy)
- ✅ Consistent logging and monitoring
- ✅ Request validation at gateway level
- ✅ Future caching capability at Core
- ✅ UI independent of microservice architecture
- ✅ Consistent pattern across all features

**Implementation Files:**
- `SentinelAI-Core/src/main/java/com/sentinel/ai/controller/ContextController.java` - BFF proxy controller
- `SentinelAI-Core/src/main/java/com/sentinel/ai/service/KnowledgeServiceClient.java` - Enhanced with `retrieveContextForUI()`
- `SentinelAI-Core/src/main/java/com/sentinel/ai/dto/ContextRetrievalRequest.java` - Request DTO
- `SentinelAI-Core/src/main/java/com/sentinel/ai/dto/ContextRetrievalResponse.java` - Response DTO
- `sentinelai-ui/src/services/contextService.js` - Updated to use Core API

**Current Status:**
- ✅ BFF layer fully implemented in Core
- ✅ UI routes all requests through Core
- ✅ Graceful degradation when Knowledge Service unavailable
- ⚠️ Knowledge Service V2 endpoint still pending implementation

## High-Level Architecture

### Complete System Architecture with BFF Pattern

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Browser / UI (React)                                  │
│                        (localhost:3000)                                       │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────────┐    │
│  │ Log RCA        │  │ ELK            │  │ Engineering Context         │    │
│  │ Screen         │  │ Investigation  │  │ Explorer                    │    │
│  └────────┬───────┘  └────────┬───────┘  └────────┬───────────────────┘    │
└───────────┼──────────────────┼─────────────────────┼──────────────────────┘
            │                  │                     │
            │ POST /api/rca    │ POST /api/elk      │ POST /api/context/retrieve
            │                  │ /investigate        │
            └──────────────────┴─────────────────────┘
                                │
                                │ (All UI requests route through Core)
                                │ (package.json proxy: localhost:8080)
                                ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                      SentinelAI Core (BFF Layer)                              │
│                         (localhost:8080)                                      │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │ Controllers (API Gateway)                                             │   │
│  │  - RCAController        → /api/rca                                   │   │
│  │  - ElkController         → /api/elk/investigate                      │   │
│  │  - ContextController     → /api/context/retrieve                     │   │
│  └────────┬──────────────────┬────────────────────┬──────────────────────┘   │
│           │                  │                    │                          │
│  ┌────────▼──────────┐  ┌───▼────────────┐  ┌───▼──────────────────────┐   │
│  │ RCAService        │  │ ElkInvestigation│  │ KnowledgeServiceClient   │   │
│  │                   │  │ Service         │  │                          │   │
│  │ - Prompt building │  │                 │  │ - retrieveContext()      │   │
│  │ - AI Engine call  │  │ - ELK queries   │  │ - retrieveContextForUI() │   │
│  │ - Context merge   │  │ - AI Engine call│  │ - buildEmptyResponse()   │   │
│  └────────┬──────────┘  └───┬────────────┘  └───┬──────────────────────┘   │
└───────────┼──────────────────┼─────────────────┼─────────────────────────┘
            │                  │                 │
            └──────────────────┴─────────────────┘
                          │
         ┌────────────────┼────────────────────────┐
         │                │                        │
         ↓                ↓                        ↓
┌─────────────────┐ ┌──────────────────┐ ┌────────────────────────────────┐
│  AI Engine      │ │  Elasticsearch   │ │  Knowledge Service V2          │
│  (localhost:    │ │  (localhost:9200)│ │  (Engineering Context Platform)│
│   8000)         │ │                  │ │  (localhost:8090)              │
│                 │ │  - Log storage   │ └────────┬───────────────────────┘
│  - /api/rca     │ │  - Full-text     │          │
│  - /api/        │ │    search        │          │ POST /api/context/retrieve
│    embeddings   │ │  - ELK queries   │          │
└─────────────────┘ └──────────────────┘          │
                                                   ↓
┌──────────────────────────────────────────────────────────────────────────────┐
│                         Knowledge Service V2                                  │
│                    (Engineering Context Platform)                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ API Layer                                                              │  │
│  │  - ContextRetrievalController   (NEW - BFF endpoint)                 │  │
│  │  - EngineeringContextExplorerController (NEW - direct search)        │  │
│  │  - KnowledgeController           (EXISTING - metadata APIs)          │  │
│  │  - SyncController                (EXISTING - ingestion)               │  │
│  │  - WebhookController             (EXISTING - webhooks)                │  │
│  └────────┬──────────────────────────────────────────────────────────────┘  │
│           │                                                                   │
│  ┌────────▼──────────────────────────────────────────────────────────────┐  │
│  │ Application Layer                                                      │  │
│  │  - ContextRetrievalService          (NEW - orchestrates retrieval)   │  │
│  │  - EngineeringContextExplorerService (NEW - search service)           │  │
│  │  - EmbeddingGenerationService       (NEW - async embedding)          │  │
│  │  - SyncService                      (EXISTING - enhanced w/ embeddings)│  │
│  │  - KnowledgeQueryService            (EXISTING - metadata queries)     │  │
│  │  - RelationshipService              (EXISTING - graph queries)        │  │
│  └────────┬──────────────────────────────────────────────────────────────┘  │
│           │                                                                   │
│  ┌────────▼──────────────────────────────────────────────────────────────┐  │
│  │ Retrieval & Ranking Layer (NEW)                                       │  │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │  │
│  │  │ Context Retrieval│  │ Hybrid Search    │  │ Ranking Engine   │   │  │
│  │  │ Engine           │  │ Service          │  │                  │   │  │
│  │  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘   │  │
│  └───────────┼──────────────────────┼──────────────────────┼─────────────┘  │
│              │                      │                      │                 │
│  ┌───────────▼──────────────────────▼──────────────────────▼─────────────┐  │
│  │ Search Services                                                        │  │
│  │  - VectorSearchService      (NEW - pgvector semantic search)         │  │
│  │  - SqlSearchService         (NEW - JPA exact matches)                │  │
│  │  - GraphSearchService       (NEW - relationship traversal)           │  │
│  │  - TimelineSearchService    (NEW - temporal proximity)               │  │
│  └────────┬────────────────────┬────────────────────┬───────────────────┘  │
│           │                    │                    │                       │
│  ┌────────▼────────────────────▼────────────────────▼───────────────────┐  │
│  │ Data Access Layer                                                     │  │
│  │  - KnowledgeEmbeddingJpaRepository  (NEW - embeddings table)        │  │
│  │  - PgVectorOperations               (NEW - vector operations)        │  │
│  │  - [Existing JPA Repositories]      (UNCHANGED - business data)     │  │
│  └────────┬────────────────────┬────────────────────┬───────────────────┘  │
└───────────┼────────────────────┼────────────────────┼─────────────────────┘
            │                    │                    │
            ↓                    ↓                    ↓
┌──────────────────────────────────────────────────────────────────────────────┐
│                      PostgreSQL Database (localhost:5432)                     │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │ Business Tables (EXISTING - unchanged)                                │   │
│  │  - knowledge_repository                                              │   │
│  │  - knowledge_commit                                                  │   │
│  │  - knowledge_pull_request                                            │   │
│  │  - knowledge_jira_issue                                              │   │
│  │  - knowledge_confluence_document                                     │   │
│  │  - knowledge_jenkins_deployment                                      │   │
│  │  - knowledge_relationship                                            │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │ Embeddings Table (NEW - with pgvector extension)                     │   │
│  │  - knowledge_embedding                                               │   │
│  │      * id, entity_type, entity_id, chunk_number                      │   │
│  │      * content, embedding (vector(1536))                             │   │
│  │      * embedding_model, status, content_hash                         │   │
│  │      * Indexes: HNSW vector index, entity_type, status              │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                      External Systems (Ingestion Sources)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ GitHub       │  │ Jira         │  │ Confluence   │  │ Jenkins      │    │
│  │ (Webhooks)   │  │ (Webhooks)   │  │ (Webhooks)   │  │ (Webhooks)   │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
└─────────┼──────────────────┼─────────────────┼─────────────────┼────────────┘
          │                  │                 │                 │
          └──────────────────┴─────────────────┴─────────────────┘
                             │ (Webhook ingestion)
                             ↓
          [Knowledge Service WebhookController]
                             │
                             ↓
          [Sync Service saves entity + triggers async embedding]
```

## New Package Structure

```
com.sentinelai.knowledge/
├── api/
│   ├── controller/
│   │   ├── ContextRetrievalController.java      # NEW
│   │   ├── EngineeringContextExplorerController.java  # NEW
│   │   ├── KnowledgeController.java           # EXISTING (unchanged)
│   │   ├── SearchController.java               # EXISTING (unchanged)
│   │   ├── SyncController.java                 # EXISTING (unchanged)
│   │   └── WebhookController.java              # EXISTING (unchanged)
│   ├── dto/
│   │   ├── ContextRetrievalRequest.java        # NEW
│   │   ├── ContextRetrievalResponse.java       # NEW
│   │   ├── EngineeringEvidence.java            # NEW
│   │   ├── EngineeringContextExplorerRequest.java  # NEW
│   │   └── [existing DTOs...]                   # EXISTING (unchanged)
│   └── mapper/
│       ├── EngineeringEvidenceMapper.java      # NEW
│       └── KnowledgeMapper.java                # EXISTING (unchanged)
├── application/
│   ├── ContextRetrievalService.java             # NEW
│   ├── EngineeringContextExplorerService.java  # NEW
│   ├── KnowledgeQueryService.java              # EXISTING (unchanged)
│   ├── RelationshipService.java                 # EXISTING (unchanged)
│   ├── SyncService.java                         # EXISTING (enhanced)
│   └── WebhookIngestionService.java            # EXISTING (unchanged)
├── config/
│   ├── AsyncConfig.java                         # NEW
│   ├── EmbeddingConfig.java                     # NEW
│   ├── VectorSearchConfig.java                  # NEW
│   └── ExternalPlatformProperties.java         # EXISTING (unchanged)
├── connector/
│   ├── [existing connectors...]                 # EXISTING (enhanced with embedding trigger)
│   └── EngineeringConnector.java               # EXISTING (unchanged)
├── domain/
│   ├── EmbeddingStatus.java                     # NEW
│   ├── EmbeddingProvider.java                   # NEW
│   ├── EntityType.java                          # NEW
│   ├── [existing domains...]                    # EXISTING (unchanged)
├── embedding/
│   ├── EmbeddingGenerationService.java         # NEW
│   ├── EmbeddingProviderImpl/
│   │   ├── OpenRouterEmbeddingProvider.java     # NEW
│   │   ├── OpenAIEmbeddingProvider.java         # NEW
│   │   ├── OllamaEmbeddingProvider.java         # NEW
│   │   └── AzureOpenAIEmbeddingProvider.java    # NEW
│   └── scheduler/
│       └── EmbeddingRetryScheduler.java        # NEW
├── retrieval/
│   ├── ContextRetrievalEngine.java              # NEW
│   ├── HybridSearchService.java                # NEW
│   ├── VectorSearchService.java                # NEW
│   ├── SqlSearchService.java                   # NEW
│   ├── GraphSearchService.java                 # NEW
│   └── TimelineSearchService.java               # NEW
├── ranking/
│   ├── RankingEngine.java                       # NEW
│   ├── RankingStrategy.java                     # NEW
│   └── signal/
│       ├── VectorSimilaritySignal.java         # NEW
│       ├── RelationshipStrengthSignal.java     # NEW
│       ├── TimelineProximitySignal.java        # NEW
│       ├── ServiceMatchSignal.java             # NEW
│       ├── EnvironmentMatchSignal.java         # NEW
│       ├── RecencySignal.java                  # NEW
│       └── ExactMatchSignal.java                # NEW
├── chunking/
│   ├── DocumentChunker.java                     # NEW
│   ├── ChunkingStrategy.java                    # NEW
│   └── SemanticChunker.java                    # NEW
├── infrastructure/
│   ├── persistence/
│   │   ├── KnowledgeEmbeddingEntity.java       # NEW
│   │   ├── [existing entities...]              # EXISTING (unchanged)
│   ├── repository/
│   │   ├── KnowledgeEmbeddingJpaRepository.java # NEW
│   │   └── [existing repositories...]         # EXISTING (unchanged)
│   └── vector/
│       └── PgVectorOperations.java             # NEW
└── scheduler/
    └── KnowledgeSyncScheduler.java              # EXISTING (unchanged)
```

## Database Schema Changes

### New Table: knowledge_embedding

```sql
CREATE TABLE knowledge_embedding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(256) NOT NULL,
    chunk_number INTEGER NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    embedding vector(1536),
    embedding_model VARCHAR(120) NOT NULL,
    embedding_version VARCHAR(40) NOT NULL DEFAULT 'v1',
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    content_hash VARCHAR(64),
    last_embedded TIMESTAMP WITH TIME ZONE,
    created_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uk_embedding_entity UNIQUE (entity_type, entity_id, chunk_number)
);

-- Indexes for vector similarity search
CREATE INDEX idx_embedding_vector ON knowledge_embedding 
USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100);

-- Indexes for metadata filtering
CREATE INDEX idx_embedding_entity_type ON knowledge_embedding(entity_type);
CREATE INDEX idx_embedding_status ON knowledge_embedding(status);
CREATE INDEX idx_embedding_content_hash ON knowledge_embedding(content_hash);

-- Trigger for updated_time
CREATE TRIGGER update_knowledge_embedding_time
BEFORE UPDATE ON knowledge_embedding
FOR EACH ROW
EXECUTE FUNCTION update_updated_time_column();
```

### Entity Type Enum Values
- `GITHUB_COMMIT`
- `GITHUB_PULL_REQUEST`
- `GITHUB_RELEASE`
- `JIRA_ISSUE`
- `CONFLUENCE_DOCUMENT`
- `JENKINS_DEPLOYMENT`

### Embedding Status Enum
- `PENDING` - Waiting to be embedded
- `PROCESSING` - Currently being embedded
- `COMPLETED` - Successfully embedded
- `FAILED` - Embedding failed (will retry)
- `SKIPPED` - Entity marked as not requiring embedding

## Entity Models

### EngineeringEvidence (DTO)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineeringEvidence {
    private String type;              // COMMIT, JIRA_ISSUE, DEPLOYMENT, etc.
    private String title;             // Human-readable title
    private String summary;           // Brief summary of evidence
    private Double score;             // Combined ranking score (0-1)
    private String reason;            // Why this evidence was selected
    private Map<String, Object> metadata;  // Additional metadata
    private String sourceSystem;      // GITHUB, JIRA, CONFLUENCE, etc.
    private String sourceId;          // ID in source system
    private String link;              // Direct link to source
    
    // Individual signal scores
    private Double vectorSimilarityScore;
    private Double relationshipScore;
    private Double timelineScore;
    private Double serviceMatchScore;
    private Double environmentMatchScore;
    private Double recencyScore;
    private Double exactMatchScore;
}
```

### ContextRetrievalRequest

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextRetrievalRequest {
    @NotBlank
    private String logs;              // Error logs or incident description
    
    // Optional filters
    private String service;           // Service name
    private String environment;       // Environment (prod, staging, etc.)
    private String repository;        // Repository name
    private String deployment;        // Deployment identifier
    private String commit;            // Commit hash
    private Instant time;             // Incident time
    
    // Retrieval options
    @Builder.Default
    private Integer maxResults = 10;   // Max evidence to return
    @Builder.Default
    private Double minScore = 0.3;    // Minimum score threshold
    
    // Search options
    private Boolean includeHistoricalIncidents;
    private Boolean includeRunbooks;
    private Boolean includeArchitectureDocs;
}
```

### ContextRetrievalResponse

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextRetrievalResponse {
    private String summary;                    // Overall context summary
    private List<EngineeringEvidence> evidence; // Ranked evidence
    private RetrievalMetadata metadata;        // Retrieval statistics
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievalMetadata {
        private int totalCandidates;
        private int filteredResults;
        private long retrievalTimeMs;
        private Map<String, Integer> signalWeights;
        private String queryEmbeddingModel;
    }
}
```

## Embedding Design

### What Should Be Embedded

#### GitHub
- ✅ Commit Message
- ✅ PR Title
- ✅ PR Description
- ✅ Release Notes
- ❌ Commit Hash
- ❌ Branch Names
- ❌ IDs

#### Jira
- ✅ Summary
- ✅ Description
- ✅ Comments
- ❌ Priority
- ❌ Status
- ❌ IDs

#### Confluence
- ✅ Title
- ✅ Body Content
- ✅ Runbooks
- ✅ Architecture Decisions
- ✅ Troubleshooting Guides
- ❌ Metadata fields

#### Jenkins
- ✅ Deployment Notes
- ✅ Failure Reason
- ✅ Build Summary
- ❌ Build Number
- ❌ IDs

### Embedding Generation Flow

```
Sync Service Saves Entity
         ↓
Set embeddingStatus = PENDING
         ↓
Transaction Commits
         ↓
@Async Trigger
         ↓
EmbeddingGenerationService
         ↓
Extract Embeddable Content
         ↓
Chunking (if > 2000 chars)
         ↓
Call AI Engine /api/embeddings
         ↓
Store in knowledge_embedding
         ↓
Set embeddingStatus = COMPLETED
         ↓
If Failed: Set FAILED, Schedule Retry
```

### Embedding Provider Abstraction

```java
public interface EmbeddingProvider {
    String getName();
    String getModel();
    int getDimension();
    List<Float> generateEmbedding(String text);
    boolean isAvailable();
}
```

### Configuration

```yaml
sentinelai:
  knowledge:
    embedding:
      enabled: true
      provider: openrouter  # openai, ollama, azure-openai
      model: text-embedding-3-small
      dimension: 1536
      batch-size: 10
      async:
        enabled: true
        thread-pool-size: 5
        queue-capacity: 100
      retry:
        max-attempts: 3
        backoff-ms: 5000
      chunking:
        enabled: true
        max-chunk-size: 2000
        chunk-overlap: 200
```

## Context Retrieval Engine

### Architecture

```
ContextRetrievalEngine
    │
    ├── 1. Entity Extraction
    │   └── Extract service, environment, error patterns from logs
    │
    ├── 2. Query Expansion
    │   └── Generate query embedding
    │
    ├── 3. Hybrid Search
    │   ├── Vector Search (semantic similarity)
    │   ├── SQL Search (exact matches, filters)
    │   ├── Graph Search (relationship traversal)
    │   ├── Timeline Search (temporal proximity)
    │   └── Service/Environment Matching
    │
    ├── 4. Signal Aggregation
    │   └── Collect all signal scores
    │
    ├── 5. Ranking
    │   └── Combine signals with weights
    │
    └── 6. Evidence Construction
        └── Build EngineeringEvidence objects
```

### Hybrid Search Algorithm

```java
public class HybridSearchService {
    
    public List<Candidate> search(ContextRetrievalRequest request) {
        // 1. Vector Search - Semantic similarity
        List<Candidate> vectorResults = vectorSearchService.search(request);
        
        // 2. SQL Search - Exact matches and filters
        List<Candidate> sqlResults = sqlSearchService.search(request);
        
        // 3. Graph Search - Relationship traversal
        List<Candidate> graphResults = graphSearchService.search(request);
        
        // 4. Timeline Search - Temporal proximity
        List<Candidate> timelineResults = timelineSearchService.search(request);
        
        // 5. Merge and deduplicate
        return mergeAndDeduplicate(
            vectorResults, 
            sqlResults, 
            graphResults, 
            timelineResults
        );
    }
}
```

## Ranking Algorithm

### Signal Components

1. **Vector Similarity Score** (0-1)
   - Cosine similarity between query and document embedding
   - Weight: 0.35

2. **Relationship Strength Score** (0-1)
   - Graph distance from incident entities
   - Direct relationships: 1.0
   - 1-hop: 0.7
   - 2-hop: 0.4
   - Weight: 0.20

3. **Timeline Proximity Score** (0-1)
   - Temporal distance from incident time
   - Same day: 1.0
   - Same week: 0.8
   - Same month: 0.5
   - Older: 0.2
   - Weight: 0.15

4. **Service Match Score** (0-1)
   - Exact match: 1.0
   - Partial match: 0.6
   - No match: 0.0
   - Weight: 0.10

5. **Environment Match Score** (0-1)
   - Exact match: 1.0
   - Related env (staging → prod): 0.5
   - No match: 0.0
   - Weight: 0.10

6. **Recency Score** (0-1)
   - Last 7 days: 1.0
   - Last 30 days: 0.8
   - Last 90 days: 0.5
   - Older: 0.3
   - Weight: 0.05

7. **Exact Match Score** (0-1)
   - Exact string matches in logs
   - Error codes, exception types
   - Weight: 0.05

### Combined Score Formula

```
finalScore = (
    vectorSimilarityScore × 0.35 +
    relationshipScore × 0.20 +
    timelineScore × 0.15 +
    serviceMatchScore × 0.10 +
    environmentMatchScore × 0.10 +
    recencyScore × 0.05 +
    exactMatchScore × 0.05
)
```

### Ranking Engine

```java
public class RankingEngine {
    
    public List<EngineeringEvidence> rank(
        List<Candidate> candidates, 
        ContextRetrievalRequest request
    ) {
        return candidates.stream()
            .map(candidate -> {
                Map<String, Double> signals = calculateSignals(candidate, request);
                double finalScore = combineSignals(signals);
                return toEvidence(candidate, signals, finalScore);
            })
            .filter(e -> e.getScore() >= request.getMinScore())
            .sorted(Comparator.comparing(EngineeringEvidence::getScore).reversed())
            .limit(request.getMaxResults())
            .toList();
    }
    
    private Map<String, Double> calculateSignals(Candidate candidate, ContextRetrievalRequest request) {
        Map<String, Double> signals = new HashMap<>();
        signals.put("vectorSimilarity", vectorSimilaritySignal.calculate(candidate, request));
        signals.put("relationship", relationshipStrengthSignal.calculate(candidate, request));
        signals.put("timeline", timelineProximitySignal.calculate(candidate, request));
        signals.put("serviceMatch", serviceMatchSignal.calculate(candidate, request));
        signals.put("environmentMatch", environmentMatchSignal.calculate(candidate, request));
        signals.put("recency", recencySignal.calculate(candidate, request));
        signals.put("exactMatch", exactMatchSignal.calculate(candidate, request));
        return signals;
    }
}
```

## Context Retrieval API

### POST /api/context/retrieve

**Purpose**: Given logs, return only relevant engineering evidence

**Request**:
```json
{
  "logs": "ERROR: NullPointerException in PaymentService at line 45",
  "service": "payment-service",
  "environment": "production",
  "repository": "payment-api",
  "time": "2026-07-22T10:30:00Z",
  "maxResults": 10,
  "minScore": 0.3
}
```

**Response**:
```json
{
  "summary": "Found 8 relevant engineering evidence items related to NullPointerException in payment-service",
  "evidence": [
    {
      "type": "JIRA_ISSUE",
      "title": "PAY-1234: Fix NullPointerException in PaymentService",
      "summary": "Fixed NPE occurring when payment method is null",
      "score": 0.92,
      "reason": "High semantic similarity, exact service match, recent fix",
      "metadata": {
        "issueKey": "PAY-1234",
        "status": "Resolved",
        "fixVersion": "2.1.0"
      },
      "sourceSystem": "JIRA",
      "sourceId": "PAY-1234",
      "link": "https://jira.company.com/browse/PAY-1234",
      "vectorSimilarityScore": 0.95,
      "relationshipScore": 0.80,
      "timelineScore": 0.90,
      "serviceMatchScore": 1.0,
      "environmentMatchScore": 0.8,
      "recencyScore": 0.95,
      "exactMatchScore": 0.90
    },
    {
      "type": "COMMIT",
      "title": "Fix null check in PaymentService.processPayment",
      "summary": "Added null check for payment method before processing",
      "score": 0.88,
      "reason": "High semantic similarity, related to same service",
      "metadata": {
        "hash": "abc123def456",
        "author": "john.doe",
        "committedAt": "2026-07-20T15:30:00Z"
      },
      "sourceSystem": "GITHUB",
      "sourceId": "abc123def456",
      "link": "https://github.com/company/payment-api/commit/abc123def456",
      "vectorSimilarityScore": 0.90,
      "relationshipScore": 0.75,
      "timelineScore": 0.85,
      "serviceMatchScore": 1.0,
      "environmentMatchScore": 0.7,
      "recencyScore": 0.90,
      "exactMatchScore": 0.85
    }
  ],
  "metadata": {
    "totalCandidates": 156,
    "filteredResults": 8,
    "retrievalTimeMs": 245,
    "signalWeights": {
      "vectorSimilarity": 0.35,
      "relationship": 0.20,
      "timeline": 0.15,
      "serviceMatch": 0.10,
      "environmentMatch": 0.10,
      "recency": 0.05,
      "exactMatch": 0.05
    },
    "queryEmbeddingModel": "text-embedding-3-small"
  }
}
```

## Engineering Context Explorer API

### POST /api/context/explorer

**Purpose**: Enable direct UI usage for engineering context exploration without RCA

**Request**:
```json
{
  "query": "payment service timeout errors",
  "filters": {
    "service": "payment-service",
    "environment": "production",
    "timeRange": {
      "from": "2026-07-01T00:00:00Z",
      "to": "2026-07-22T23:59:59Z"
    }
  },
  "searchType": "SEMANTIC",
  "maxResults": 20
}
```

**Search Types**:
- `SEMANTIC` - Vector similarity search
- `EXACT` - Exact text match
- `HYBRID` - Combine semantic and exact
- `SIMILAR_INCIDENTS` - Find similar past incidents
- `RELATED_JIRA` - Find related Jira issues
- `DEPLOYMENT_HISTORY` - Find deployment history
- `RUNBOOKS` - Find relevant runbooks
- `COMMITS` - Find related commits

**Response**: Same structure as ContextRetrievalResponse

## RCA Flow Evolution

### Current Production Flow (WITH BFF PATTERN)
```
User uploads logs
  ↓
UI (Log RCA Screen)
  ↓
POST /api/rca (Core)
  ↓
RCAController
  ↓
RCAService
  ├─→ KnowledgeServiceClient.retrieveContext() → Knowledge Service (metadata)
  ├─→ Build comprehensive prompt with logs + metadata
  └─→ POST /api/rca (AI Engine) → GPT-4/Claude → Structured RCA
  ↓
Return RCA to UI
```

### New Flow with Context Retrieval API (V2 TARGET)
```
User uploads logs
  ↓
UI (Log RCA Screen)
  ↓
POST /api/rca (Core)
  ↓
RCAController
  ↓
RCAService
  ├─→ KnowledgeServiceClient.retrieveContext()
  │     ↓
  │   POST /api/context/retrieve (Knowledge Service V2)
  │     ↓
  │   ContextRetrievalEngine
  │     ├─→ Vector Search (semantic similarity)
  │     ├─→ SQL Search (exact matches)
  │     ├─→ Graph Search (relationships)
  │     ├─→ Timeline Search (temporal proximity)
  │     └─→ Ranking Engine (combine signals)
  │     ↓
  │   Top 10 Ranked Evidence Only
  │     ↓
  ├─→ Build focused prompt with logs + evidence
  └─→ POST /api/rca (AI Engine) → GPT-4/Claude → Structured RCA
  ↓
Return RCA to UI
```

### ELK Investigation Flow (WITH BFF PATTERN)
```
User searches ELK logs
  ↓
UI (ELK Investigation Screen)
  ↓
POST /api/elk/investigate (Core)
  ↓
ElkController
  ↓
ElkInvestigationService
  ├─→ Query Elasticsearch (log entries)
  ├─→ KnowledgeServiceClient.retrieveContext() → Knowledge Service (metadata)
  ├─→ Build comprehensive prompt with ELK logs + metadata
  └─→ POST /api/rca (AI Engine) → GPT-4/Claude → Investigation results
  ↓
Return analysis to UI
```

### Engineering Context Explorer Flow (WITH BFF PATTERN)
```
User pastes logs / searches
  ↓
UI (Engineering Context Explorer Screen)
  ↓
POST /api/context/retrieve (Core)
  ↓
ContextController
  ↓
KnowledgeServiceClient.retrieveContextForUI()
  ↓
POST /api/context/retrieve (Knowledge Service V2)
  ↓
ContextRetrievalController
  ↓
ContextRetrievalService
  ├─→ Vector Search (semantic similarity)
  ├─→ SQL Search (exact matches)
  ├─→ Graph Search (relationships)
  ├─→ Timeline Search (temporal proximity)
  └─→ Ranking Engine (combine signals)
  ↓
Ranked Evidence with Scores
  ↓
Return to UI (display in panels with metadata)
```

### Key Architecture Principles

1. **Backend-for-Frontend (BFF) Pattern**
   - UI NEVER calls microservices directly
   - Core acts as API Gateway for all UI requests
   - Centralized authentication, validation, logging

2. **Service Separation**
   - Core: Business logic, orchestration, API gateway
   - Knowledge Service: Data ingestion, embeddings, semantic search
   - AI Engine: LLM calls, embeddings generation

3. **Graceful Degradation**
   - If Knowledge Service unavailable → Core returns empty response
   - UI handles empty responses gracefully
   - No cascading failures

## Vector Search with pgvector

### Implementation

```java
@Repository
public interface KnowledgeEmbeddingJpaRepository extends JpaRepository<KnowledgeEmbeddingEntity, UUID> {
    
    @Query(value = """
        SELECT id, entity_type, entity_id, chunk_number, content, 
               embedding, embedding_model, embedding_version, status,
               1 - (embedding <=> :queryEmbedding) as similarity
        FROM knowledge_embedding
        WHERE status = 'COMPLETED'
        AND embedding IS NOT NULL
        ORDER BY embedding <=> :queryEmbedding
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findBySimilarity(@Param("queryEmbedding") String queryEmbedding, 
                                      @Param("limit") int limit);
}
```

### HNSW Index Configuration

```sql
-- For better performance on large datasets
CREATE INDEX idx_embedding_hnsw ON knowledge_embedding 
USING hnsw (embedding vector_cosine_ops) 
WITH (m = 16, ef_construction = 64);
```

## Chunking Strategy

### Document Chunker

```java
public class DocumentChunker {
    
    private static final int MAX_CHUNK_SIZE = 2000;
    private static final int CHUNK_OVERLAP = 200;
    
    public List<String> chunk(String content) {
        if (content.length() <= MAX_CHUNK_SIZE) {
            return List.of(content);
        }
        
        List<String> chunks = new ArrayList<>();
        int start = 0;
        
        while (start < content.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, content.length());
            
            // Try to break at sentence boundary
            if (end < content.length()) {
                int lastSentence = content.lastIndexOf('.', end);
                if (lastSentence > start + MAX_CHUNK_SIZE / 2) {
                    end = lastSentence + 1;
                }
            }
            
            chunks.add(content.substring(start, end).trim());
            start = end - CHUNK_OVERLAP;
        }
        
        return chunks;
    }
}
```

## Migration Strategy

### Phase 1: Foundation (Week 1-2)
1. Add new packages and structure
2. Create knowledge_embedding table
3. Implement embedding provider abstraction
4. Configure async processing
5. Add pgvector dependency

### Phase 2: Embedding Generation (Week 3-4)
1. Implement EmbeddingGenerationService
2. Add embedding status to existing entities
3. Update sync flows to trigger embedding generation
4. Implement retry scheduler
5. Backfill existing data with embeddings

### Phase 3: Search Infrastructure (Week 5-6)
1. Implement VectorSearchService
2. Implement SqlSearchService
3. Implement GraphSearchService
4. Implement TimelineSearchService
5. Create HybridSearchService

### Phase 4: Ranking Engine (Week 7)
1. Implement all signal calculators
2. Implement RankingEngine
3. Tune signal weights
4. Add scoring tests

### Phase 5: Context Retrieval API (Week 8)
1. Implement ContextRetrievalEngine
2. Implement ContextRetrievalController
3. Implement EngineeringEvidence mappers
4. Add API documentation

### Phase 6: Engineering Context Explorer (Week 9)
1. Implement EngineeringContextExplorerService
2. Implement EngineeringContextExplorerController
3. Add specialized search types

### Phase 7: AI Engine Integration (Week 10)
1. Extend AI Engine with /api/embeddings endpoint
2. Integrate with embedding providers
3. Add routing for different models

### Phase 8: Core Service Integration (Week 11)
1. Update KnowledgeServiceClient
2. Switch to Context Retrieval API
3. Update prompt building
4. Test RCA flow

### Phase 9: Testing & Validation (Week 12)
1. Backward compatibility testing
2. Performance testing
3. Accuracy testing
4. Load testing

### Phase 10: Rollout (Week 13)
1. Feature flags
2. Gradual rollout
3. Monitoring
4. Documentation

## Backward Compatibility Strategy

### Existing APIs - No Changes
- `/api/repositories` - Unchanged
- `/api/commits` - Unchanged
- `/api/jira/issues` - Unchanged
- `/api/deployments` - Unchanged
- `/api/releases` - Unchanged
- `/api/timeline` - Unchanged
- `/api/relationships` - Unchanged
- `/api/sync` - Unchanged
- Webhook endpoints - Unchanged

### Existing Database Tables - No Changes
- All existing tables remain unchanged
- New embedding_status column added (nullable, default null)
- New knowledge_embedding table (separate, no foreign keys to existing tables)

### Existing Sync Flow - Enhanced Only
- Current sync logic unchanged
- New async embedding trigger added after successful transaction
- No blocking changes

### Core Service - Gradual Migration
- Feature flag for new Context Retrieval API
- Old API remains as fallback
- Gradual rollout with monitoring

## Implementation Plan

### Step 1: Project Setup
1. Update pom.xml with new dependencies
2. Create new package structure
3. Configure async processing
4. Configure pgvector

### Step 2: Database Layer
1. Create KnowledgeEmbeddingEntity
2. Create KnowledgeEmbeddingJpaRepository
3. Create database migration
4. Add embedding_status columns to existing tables

### Step 3: Embedding Infrastructure
1. Create EmbeddingProvider interface
2. Implement provider classes
3. Create EmbeddingGenerationService
4. Create EmbeddingRetryScheduler
5. Configure async thread pool

### Step 4: Chunking Infrastructure
1. Create DocumentChunker interface
2. Implement SemanticChunker
3. Add chunking configuration

### Step 5: Sync Integration
1. Update connectors to set embedding status
2. Add async trigger after sync
3. Implement backfill job

### Step 6: Search Infrastructure
1. Create VectorSearchService
2. Create SqlSearchService
3. Create GraphSearchService
4. Create TimelineSearchService
5. Create HybridSearchService

### Step 7: Ranking Infrastructure
1. Create signal interfaces
2. Implement all signal calculators
3. Create RankingEngine
4. Add ranking configuration

### Step 8: Context Retrieval
1. Create EngineeringEvidence model
2. Create ContextRetrievalEngine
3. Create ContextRetrievalService
4. Create mappers for all entity types

### Step 9: API Layer
1. Create ContextRetrievalController
2. Create EngineeringContextExplorerController
3. Add API documentation
4. Add validation

### Step 10: AI Engine Integration
1. Add /api/embeddings endpoint to AI Engine
2. Implement provider routing
3. Add model configuration

### Step 11: Core Service Integration
1. Update KnowledgeServiceClient
2. Add feature flag
3. Update prompt building
4. Add fallback logic

### Step 12: Testing
1. Unit tests for all components
2. Integration tests for APIs
3. Backward compatibility tests
4. Performance tests

### Step 13: Documentation
1. API documentation
2. Architecture documentation
3. Migration guide
4. Runbook

## Future Scalability

### Additional Integrations (No Core Changes Required)
- ServiceNow - Add connector, embedding, and mapper
- Azure DevOps - Add connector, embedding, and mapper
- Slack - Add connector, embedding, and mapper
- Teams - Add connector, embedding, and mapper
- Datadog - Add connector, embedding, and mapper
- Splunk - Add connector, embedding, and mapper
- Grafana - Add connector, embedding, and mapper

### Advanced Features
- Multi-tenant embedding isolation
- Custom embedding models per tenant
- A/B testing for ranking strategies
- Machine learning for signal weight optimization
- Real-time embedding updates
- Distributed vector search for scale

## Production Considerations

### Performance
- Embedding generation: Async, batched, queued
- Vector search: HNSW index, parallel queries
- Caching: Redis for frequent queries
- Connection pooling: Optimized for concurrent requests

### Reliability
- Retry logic for embedding failures
- Dead letter queue for failed embeddings
- Circuit breakers for external AI services
- Graceful degradation on service failures

### Monitoring
- Embedding generation metrics
- Search latency metrics
- Ranking score distributions
- API success rates
- Token usage tracking

### Security
- API authentication for embedding providers
- Encryption of stored embeddings
- Rate limiting for API calls
- Audit logging for context retrieval

## Configuration Examples

### application.yml additions

```yaml
sentinelai:
  knowledge:
    embedding:
      enabled: true
      provider: openrouter
      model: text-embedding-3-small
      dimension: 1536
      async:
        enabled: true
        thread-pool-size: 5
        queue-capacity: 100
      retry:
        max-attempts: 3
        backoff-ms: 5000
      chunking:
        enabled: true
        max-chunk-size: 2000
        chunk-overlap: 200
    retrieval:
      ranking:
        vector-similarity-weight: 0.35
        relationship-weight: 0.20
        timeline-weight: 0.15
        service-match-weight: 0.10
        environment-match-weight: 0.10
        recency-weight: 0.05
        exact-match-weight: 0.05
      max-results: 10
      min-score: 0.3
    ai-engine:
      url: http://localhost:8000
      embeddings-endpoint: /api/embeddings
      timeout-ms: 30000
```

## Conclusion

This architecture transforms the Knowledge Service from a passive metadata repository into an intelligent Engineering Context Platform. The design ensures:

1. **Backward Compatibility**: All existing APIs and functionality remain unchanged
2. **Modularity**: New capabilities are additive, not replacing existing features
3. **Performance**: Async processing, efficient indexing, optimized queries
4. **Scalability**: Easy to add new integrations without changing Core
5. **Quality**: Hybrid retrieval with multiple signals for accurate ranking
6. **Enterprise-Grade**: Production-ready with proper error handling, monitoring, and security

The phased migration approach minimizes risk while delivering incremental value. The architecture is future-proof and ready for additional integrations and advanced features.

---

## Current Deployment Guide

### System Requirements
- **Java:** 17 or higher
- **Node.js:** 16 or higher
- **Maven:** 3.8+
- **Docker:** For PostgreSQL, Elasticsearch, Ollama (optional)
- **PostgreSQL:** 12+ with pgvector extension installed

**Important:** The Knowledge Service requires the `pgvector` extension in PostgreSQL. If using Docker, it's included in the `ankane/pgvector` image. For manual setup, see `PGVECTOR_SETUP.md`.

### Quick Start (Development)

#### 1. Start Infrastructure
```bash
# PostgreSQL (with pgvector extension)
docker-compose -f postgres-docker-compose.yml up -d

# Enable pgvector extension
docker exec sentinelai-postgres psql -U sentinel -d sentinelai_knowledge -c "CREATE EXTENSION IF NOT EXISTS vector;"

# Elasticsearch
docker-compose -f elk-docker-compose.yml up -d

# Ollama (Optional - for local AI)
docker-compose -f ollama-docker-compose.yml up -d
```

#### 2. Start Backend Services

**AI Engine (Port 8000):**
```bash
cd SentinelAI-Engine
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

**Knowledge Service (Port 8090):**
```bash
cd SentinelAI-Knowledge-Service
mvn clean install -DskipTests
mvn spring-boot:run
```

**Core Service (Port 8080):**
```bash
cd SentinelAI-Core
mvn clean install -DskipTests
mvn spring-boot:run
```

#### 3. Start Frontend (Port 3000)
```bash
cd sentinelai-ui
npm install
npm start
```

#### 4. Load Demo Data

**ELK Data:**
```bash
cd demo-data\elk
.\elk-seed-demo-incidents.bat
```

**Knowledge Service Data:**
```bash
cd demo-data\knowledge
.\knowledge-demo-data.bat
```

**RCA Logs:**
- Upload files from `demo-data/rca/` through the Log RCA UI

### Service Endpoints

| Service | Port | Health Check |
|---------|------|--------------|
| UI | 3000 | http://localhost:3000 |
| Core | 8080 | http://localhost:8080/actuator/health |
| Knowledge Service | 8090 | http://localhost:8090/actuator/health |
| AI Engine | 8000 | http://localhost:8000/health |
| PostgreSQL | 5432 | `psql -h localhost -U postgres` |
| Elasticsearch | 9200 | http://localhost:9200 |

### UI Access

Once all services are running:

1. **Log RCA:** http://localhost:3000/rca
   - Upload log file from `demo-data/rca/`
   - Click "Analyze Logs"
   - View structured RCA with engineering context

2. **ELK Investigation:** http://localhost:3000/elk
   - Search by service: `payment-service`
   - Search by severity: `ERROR`
   - Search by time range
   - View logs with engineering context

3. **Engineering Context Explorer:** http://localhost:3000/context
   - Paste logs or describe issue
   - Click "Search"
   - View ranked engineering evidence with scores

### Architecture Verification

To verify BFF pattern is working:

```bash
# Check UI calls Core (not Knowledge Service directly)
# Open browser DevTools → Network tab
# UI requests should go to http://localhost:3000/api/* 
# (proxied to Core at localhost:8080)

# Check Core forwards to Knowledge Service
# Check Core logs for:
# "Forwarding context retrieval request to Knowledge Service"
```

### Demo Data Overview

**5 Production Incidents:**
1. Payment Service - HikariCP pool exhaustion
2. Order Service - Kafka consumer lag
3. Vehicle Registration - Oracle timeout
4. Notification Service - Redis outage
5. Authentication Service - JWT validation failure

**Data Sources:**
- **RCA Logs:** 5 log files with 200-500 entries each
- **ELK Data:** 48 log entries across all incidents
- **Knowledge Service:** GitHub commits, Jira issues, Confluence docs, Jenkins deployments

**Interconnected Data:**
- Same commit hashes across systems
- Same Jira IDs in commits and deployments
- Same timestamps (±2 hours)
- Semantic relationships for AI correlation

### Troubleshooting

**UI not loading:**
- Check all backend services are running
- Verify ports 8080, 8090, 8000 are not in use
- Check browser console for errors

**Empty context in Engineering Explorer:**
- Expected behavior - Knowledge Service V2 endpoint not yet implemented
- Core returns empty response gracefully
- No errors should be displayed

**RCA/ELK not working:**
- Check AI Engine is running (port 8000)
- Check Knowledge Service is running (port 8090)
- Verify OpenRouter/OpenAI API keys are configured

**Demo data not loading:**
- Check Elasticsearch is running (port 9200)
- Check PostgreSQL is running (port 5432)
- Re-run seed scripts from demo-data/ folder

**pgvector extension missing:**
- Error: `type "vector" does not exist`
- Solution: `docker exec sentinelai-postgres psql -U sentinel -d sentinelai_knowledge -c "CREATE EXTENSION IF NOT EXISTS vector;"`
- See `PGVECTOR_SETUP.md` for details

### Next Steps for Production

1. **Complete Knowledge Service V2:**
   - Implement ContextRetrievalController
   - Add embedding generation service
   - Add vector search with pgvector
   - Add ranking engine

2. **Security:**
   - Add Spring Security to Core
   - Implement JWT authentication
   - Add API rate limiting
   - Secure Knowledge Service endpoints

3. **Observability:**
   - Add distributed tracing (Zipkin/Jaeger)
   - Add metrics (Prometheus/Grafana)
   - Add log aggregation (ELK/Splunk)
   - Add alerting

4. **Performance:**
   - Add Redis caching in Core
   - Optimize database queries
   - Add connection pooling tuning
   - Load testing and profiling

5. **Deployment:**
   - Containerize all services (Docker)
   - Kubernetes deployment manifests
   - CI/CD pipeline
   - Infrastructure as Code (Terraform)

---

## Documentation References

- **BFF Implementation:** See `BFF_IMPLEMENTATION_SUMMARY.md`
- **Demo Guide:** See `demo-data/DEMO_GUIDE.md`
- **Demo Data:** See `demo-data/README.md`
- **Architecture Review:** See `ARCHITECTURE_REVIEW_REPORT.md`
- **Main README:** See `README.md`
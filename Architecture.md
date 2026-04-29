# SentinelAI — Architecture (High Level)

This document gives architects a single, high-signal view of SentinelAI: components, runtime flows, key decisions, and extensibility points.

## 1) Executive Summary

SentinelAI is a **log RCA system** with two complementary capabilities:

- **Memory (Retrieval / RAG)**: find similar historical incidents using embeddings + similarity search.
- **Reasoning (Generation)**: generate a structured RCA (issue, root cause, impact, recommended fix) using an LLM provider.

The system is implemented as a **3-tier runtime**:
- **React UI** for log submission and result visualization
- **Spring Boot Core** as the orchestration layer (APIs, retrieval, persistence, governance)
- **Python AI-Engine** as an AI gateway (provider routing, LLM prompt execution)

## 2) System Context (C4 – Context)

Actors / external systems:
- **Engineer (UI)**: uploads/pastes logs and consumes RCA output
- **SentinelAI-Core (Spring Boot)**: primary backend API and orchestrator
- **SentinelAI-Engine (FastAPI)**: AI gateway (provider adapters)
- **PostgreSQL (+ pgvector image for local)**: incident store (log text + embeddings)
- **Ollama (optional)**: local embeddings (and optionally local LLM inference)
- **Groq / HuggingFace / OpenAI (optional)**: remote providers via AI-Engine
- **Jira (optional)**: ticket creation from RCA output

## 3) Container View (C4 – Container)

- **`sentinelai-ui` (React)**: browser app, calls Core
- **`SentinelAI-Core` (Spring Boot)**: REST APIs, retrieval pipeline, DB integration, calls AI-Engine
- **`SentinelAI-Engine` (FastAPI)**: provider routing, returns stable RCA payload
- **`postgres` (pgvector image)**: durable storage for incidents + embeddings
- **`ollama` (optional)**: local embedding model endpoint

Local infra compose files are in `SentinelAI/`:
- `SentinelAI/postgres-docker-compose.yml`
- `SentinelAI/ollama-docker-compose.yml`

## 4) Key Runtime Flows

### 4.1 RCA (RAG-style)

1. **UI → Core**: `POST /api/rca/analyze` with `{ "log": "<text>" }`
2. **Core (optional retrieval)**:
   - If `sentinelai.semantic-search.enabled=true`, Core embeds the input log and retrieves top-K similar incidents.
   - If embeddings are unavailable (e.g., Ollama down), Core logs a warning and continues without retrieval context.
3. **Core → AI-Engine**: `POST <ai-engine-url>?provider=<provider>` with `{ "logs": "<prompt/log+context>" }`
4. **AI-Engine → Provider**: runs LLM inference via the selected provider adapter
5. **AI-Engine → Core**: returns structured RCA fields plus `errors[]` (never crashes the pipeline with opaque 500s)
6. **Core → UI**: returns structured RCA response (including `provider` and `errors` for visibility)

### 4.2 Incident Ingestion + Embeddings

1. Client calls `POST /api/logs/upload` (JSON payload)
2. Core persists incident/log content
3. Core generates an embedding for the log text and stores it for future retrieval

### 4.3 Semantic Search

1. Client calls `POST /api/logs/search`
2. Core embeds the query text
3. Core searches the stored incident embeddings and returns top matches

## 5) Architecture Diagram

```mermaid
flowchart TB
  U[Engineer] --> UI[React UI]

  UI -->|POST /api/rca/analyze| CORE[SentinelAI-Core<br/>Spring Boot]

  CORE -->|optional retrieval<br/>semantic search| VSTORE[(Postgres<br/>embeddings + incidents)]
  CORE -->|embeddings (local)| OLLAMA[(Ollama<br/>Embeddings)]

  CORE -->|POST /analyze?provider=...| AIE[SentinelAI-Engine<br/>FastAPI]
  AIE -->|provider adapter| PROVIDER[(Groq / Ollama / HF / ...)]

  CORE -->|optional| JIRA[(Jira)]
  CORE --> UI
```

## 6) Key Design Decisions (Why this split)

- **Spring Boot as the orchestrator**: owns APIs, config profiles, governance, persistence, and future enterprise integrations.
- **Python as the AI gateway**: isolates provider-specific logic and makes provider switching low-friction.
- **Structured output contract**: AI-Engine returns stable fields plus `errors[]` so failures are debuggable from UI/cURL.
- **Pluggable retrieval**: embeddings + similarity search are isolated behind interfaces so swapping to Qdrant is an implementation detail.

## 7) Extensibility Points (Future)

- **Vector store switch (pgvector → Qdrant)**:
  - Implement a new vector search adapter (behind the existing Core vector-search interface).
  - Keep Core APIs and RCA orchestration unchanged.
- **Embedding provider switch**:
  - Keep embeddings in Core (enterprise control plane) and plug in a different embedding client if required.
- **RCA routing policy**:
  - Add confidence thresholds or rules to decide when to include retrieval context, when to run multiple providers, etc.

## 8) Operational Notes

- If you want the system to run without Ollama, set:
  - `sentinelai.semantic-search.enabled=false`
  - (RCA generation still works via AI-Engine provider such as `groq`, assuming credentials are configured.)


# SentinelAI — Architecture & Design Notes

## 1) Executive Summary
SentinelAI is a Spring Boot application that ingests logs, generates vector embeddings for semantic retrieval, and performs RCA (Root Cause Analysis) using a local LLM (Ollama, e.g., `llama3`). It optionally creates Jira tickets from confirmed RCA output.

Key architectural intent:
- **Local-first AI**: all inference runs locally via Ollama.
- **Clean layering**: Controller → Service → Repository → DB.
- **RAG-style RCA**: retrieve similar past incidents, then ask the LLM to produce structured output.

## 2) System Context (C4-Context)
Actors / external systems:
- **API Consumer**: uploads logs, runs semantic search, requests RCA.
- **PostgreSQL**: stores logs and knowledge base entries (log text + embeddings).
- **Ollama**: provides embedding model + chat model (e.g., `llama3`).
- **Jira (optional)**: ticket creation from RCA results.

Diagram: `SentinelAI-Architecture.drawio`

## 3) Container View (C4-Container)
Primary runtime containers:
- **Spring Boot App** (Java, Spring MVC, Spring Data JPA, Spring AI, springdoc-openapi)
- **PostgreSQL** (Docker, exposed on host `5111`)
- **pgAdmin** (Docker, exposed on host `5050`) — dev tooling
- **Ollama** (Docker, exposed on host `11434`)

## 4) Component View (inside Spring Boot)

### 4.1 Controllers (HTTP layer)
- `LogController` — `POST /api/logs/upload`
- `LogSearchController` — `POST /api/logs/search`
- `RCAController` — `POST /api/rca/analyze`, `POST /api/rca/jira`

### 4.2 Services (domain/application layer)
- `LogService`
  - Preprocesses raw logs → stores `Log` entity
  - Triggers embedding generation via `LogEmbeddingOrchestrator`
- `LogEmbeddingOrchestrator`
  - Calls `EmbeddingService` after a log is persisted
- `EmbeddingService`
  - Calls Spring AI `EmbeddingModel` to create an embedding for a log/query
  - Persists `KnowledgeBaseEntry`
- `SemanticSearchService`
  - Generates an embedding for the query
  - Computes cosine similarity vs stored embeddings
  - Returns top-K matches
- `RCAService`
  - Retrieves similar incidents (via `SemanticSearchService`)
  - Builds a prompt and calls Spring AI `ChatClient`
  - Expects JSON-like response and maps it to `RCAResponse`
- `JiraService`
  - Uses Jira REST API (`/rest/api/2/issue`) with Basic auth
  - Enabled only when `sentinelai.jira.*` is configured

### 4.3 Repositories (persistence layer)
- `LogRepository` (JPA)
- `KnowledgeBaseRepository` (JPA)

### 4.4 Configuration
- `application.yaml` — default profile configuration (DB + Ollama + logging)
- `application-local.yml` — optional profile override (activated with `local`)
- `ChatModelConfig` — adapts Spring AI `ChatModel` to `ChatClient`
- `JiraConfig` — binds Jira properties (safe defaults to avoid startup failures)

## 5) Data Model

### 5.1 `logs` table
Entity: `Log`
- `id` (UUID)
- `raw_log` (TEXT)
- `processed_log` (TEXT)
- `created_at` (timestamp)

### 5.2 `knowledge_base` table
Entity: `KnowledgeBaseEntry`
- `id` (UUID)
- `log_text` (TEXT)
- `embedding` (`real[]`)
- `resolution_notes` (TEXT)

Note:
- The Docker image is `ankane/pgvector`, but the current implementation stores embeddings as **`real[]`** and performs similarity search **in application code**.
- Future upgrade: switch `embedding` to a true `vector` column and compute similarity in SQL using pgvector operators for scalability.

## 6) Runtime Flows

### 6.1 Log Ingestion + Embedding
1. Client calls `POST /api/logs/upload`
2. `LogService` preprocesses and persists `Log`
3. `LogEmbeddingOrchestrator` generates and stores embedding as `KnowledgeBaseEntry`

### 6.2 Semantic Search
1. Client calls `POST /api/logs/search`
2. `SemanticSearchService` embeds query via `EmbeddingModel`
3. Loads knowledge base entries and ranks by cosine similarity (top-K)

### 6.3 RCA (RAG-style)
1. Client calls `POST /api/rca/analyze`
2. Service retrieves similar incidents (top 3)
3. Service prompts the LLM to output JSON with:
   - `issue`, `rootCause`, `impactedService`, `recommendedFix`
4. Response is mapped to `RCAResponse`

### 6.4 Jira Ticket Creation (optional)
1. Client calls `POST /api/rca/jira?projectKey=...`
2. `JiraService` posts to Jira REST API using configured credentials

## 7) Deployment (local dev)
Use `docker-compose.yml`:
- Postgres: host `localhost:5111` → container `5432`
- Ollama: host `localhost:11434`
- pgAdmin: host `localhost:5050`

Spring Boot runs on host (default `8080`) and connects to:
- PostgreSQL via `jdbc:postgresql://localhost:5111/sentinelai`
- Ollama via `http://localhost:11434`

## 8) Observability & Error Handling
- Basic logging via SLF4J
- `GlobalExceptionHandler` returns 500 with a simplified message

Recommended next step (architectural hardening):
- Add structured error response DTOs (timestamp, path, correlationId)
- Add request tracing / MDC correlation IDs
- Add health checks for Postgres + Ollama

## 9) Key Architectural Decisions (ADRs-style)
1. **Spring AI abstraction** for embeddings and chat to avoid vendor lock-in.
2. **Local Ollama** to keep inference offline and controllable.
3. **Layering** to keep controllers thin and move logic to services.
4. **In-app similarity** as MVP; pgvector SQL search is a planned scale upgrade.

## 10) Demo Script (for presentation)
1. Start infrastructure: `docker-compose up -d`
2. Run app: `./mvnw spring-boot:run`
3. Open Swagger: `http://localhost:8080/swagger-ui.html`
4. Upload a log (`/api/logs/upload`)
5. Search (`/api/logs/search`)
6. Analyze (`/api/rca/analyze`)
7. (Optional) Create Jira ticket (`/api/rca/jira`) after configuring Jira properties


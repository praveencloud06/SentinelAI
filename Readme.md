# SentinelAI (Monorepo)

SentinelAI helps engineers understand logs and incidents by combining:
1) **Memory**: retrieve similar historical incidents (RAG retrieval)
2) **Reasoning**: generate a structured RCA (root cause + fix) via an AI provider

This repo contains three deployable parts (UI, Core, AI-Engine) plus Docker compose files for local infra.

## Components

- **`SentinelAI/sentinelai-ui` (React)**
  - Single-page UI to paste log text or upload a log file.
  - Calls the Core API (`/api/rca/analyze`) and renders the structured RCA response.

- **`SentinelAI/SentinelAI-Core` (Java, Spring Boot)**
  - System orchestrator.
  - Optional retrieval (semantic search) over stored incidents.
  - Calls the Python AI-Engine for RCA generation and returns the result to the UI.
  - Stores incidents/embeddings in Postgres (pgvector image is provided for local).

- **`SentinelAI/SentinelAI-Engine` (Python, FastAPI)**
  - AI gateway that routes requests to an AI provider (e.g., `groq` or `ollama`).
  - Returns a stable, structured RCA payload back to Core.

## Data Flow (End-to-End)

1. **User → UI**
   - Paste log text or upload a file (file is read as text in the browser).
2. **UI → Core**
   - `POST /api/rca/analyze` with `{ "log": "<log text>" }`.
3. **Core (optional) retrieval**
   - If `sentinelai.semantic-search.enabled=true`, Core embeds the log and searches for similar historical incidents to build context.
   - If Ollama/embeddings are unavailable, Core logs a warning and continues without historical context.
4. **Core → AI-Engine**
   - Core sends a single prompt (log + any retrieved context) to `POST <ai-engine-url>?provider=<provider>`.
5. **AI-Engine → Provider**
   - Provider performs LLM inference and returns an RCA.
6. **Core → UI**
   - Core returns:
     - `issue`, `rootCause`, `impactedService`, `recommendedFix`
     - `provider` and `errors` (for visibility when provider/auth fails)

## API (Core)

- `POST /api/rca/analyze`
  - Request: `{ "log": "..." }`
  - Response: `{ issue, rootCause, impactedService, recommendedFix, provider, errors }`
- `POST /api/logs/upload`
  - Request: `{ "logType": "text|json|csv", "content": "..." }`
  - Purpose: store incidents + embeddings for future retrieval
- `POST /api/logs/search`
  - Request: `{ "query": "..." }`
  - Purpose: semantic search over stored incidents (requires embeddings/Ollama)

## AI Provider Routing

Core controls which provider is used for RCA generation via config:

- `sentinelai.ai-engine.url` (example: `http://localhost:8000/analyze`)
- `sentinelai.ai-engine.provider` (example: `groq`, `ollama`, `huggingface`, `openai`)

Notes:
- `provider` affects **analysis** (RCA generation) through the Python AI-Engine.
- Embeddings for retrieval are owned by Core and currently use **Ollama** when semantic search is enabled.

## Tech Stack (and why)

- **React**: fast iteration for a simple, form-based UI.
- **Spring Boot (Java)**: reliable orchestration layer; strong ecosystem for APIs, config profiles, observability, and future enterprise integrations.
- **FastAPI (Python)**: lightweight AI gateway; easy to add/swap provider adapters.
- **PostgreSQL + pgvector (Docker image)**: single durable store for incident text + embeddings; simplest RAG storage baseline.
- **Ollama (optional)**: local embeddings (and optionally local LLM) to support fully local development.

## Local Run (Typical)

1. Start infra (optional but typical):
   - Postgres/pgvector: `SentinelAI/postgres-docker-compose.yml`
   - Ollama (for embeddings): `SentinelAI/ollama-docker-compose.yml`
2. Start AI-Engine:
   - `cd SentinelAI/SentinelAI-Engine`
   - `python -m uvicorn app.main:app --reload`
3. Start Core:
   - `cd SentinelAI/SentinelAI-Core`
   - run Spring Boot with the intended profile (e.g., `local`) and verify config in `application-local.yml`
4. Start UI:
   - `cd SentinelAI/sentinelai-ui`
   - `npm start`

## Configuration Quick Reference (Core)

- `sentinelai.semantic-search.enabled`
  - `true`: retrieval enabled (requires Ollama embeddings)
  - `false`: retrieval skipped; Core still calls AI-Engine for RCA generation

## Repo Navigation

- UI: `SentinelAI/sentinelai-ui/README.md`
- Core: `SentinelAI/SentinelAI-Core/README.md`
- AI-Engine: `SentinelAI/SentinelAI-Engine/README.md`
- Docker: `SentinelAI/postgres-docker-compose.yml`, `SentinelAI/ollama-docker-compose.yml`

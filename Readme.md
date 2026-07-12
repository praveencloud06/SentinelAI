# SentinelAI

SentinelAI is an AI-assisted incident investigation platform that combines log analysis, live observability queries, and engineering context to help teams understand incidents faster and produce structured RCA output.

## Current capabilities

The repository currently includes four runnable parts:

- UI: a React-based interface for RCA and ELK investigation
- Core: a Spring Boot orchestration service for RCA, log ingestion, and Elasticsearch integration
- AI Engine: a Python FastAPI service that routes requests to Groq, Ollama, HuggingFace, or OpenAI
- Knowledge Service: a Spring Boot microservice that builds an engineering knowledge index from GitHub, Jira, Confluence, and Jenkins data

## What is implemented now

### 1. Log RCA workflow
- Paste log text or upload a log file from the UI
- Send the content to Core through the RCA API
- Generate a structured RCA response with issue, root cause, impacted service, and recommended fix
- Optionally enrich the RCA with engineering context from the Knowledge Service

### 2. Live ELK investigation
- Query Elasticsearch directly from the UI using service, severity, and timeframe
- Retrieve recent logs, summarize them, and produce an AI-generated investigation result
- Return suspicious log lines along with the likely root cause and recommended action

### 3. Semantic retrieval and incident memory
- Store uploaded incidents and optionally create embeddings for semantic retrieval
- Search stored incidents using natural-language queries when embeddings are enabled

### 4. Engineering knowledge enrichment
- The Core service can call the Knowledge Service to fetch recent timeline events, deployments, releases, and Jira issues
- That context is added to the RCA prompt and displayed in the UI as engineering evidence

### 5. Knowledge Service capabilities
- Sync metadata from GitHub, Jira, Confluence, and Jenkins
- Ingest webhook events from those systems
- Expose timeline, relationship, repository, deployment, release, and Jira search APIs
- Build a normalized engineering knowledge index without performing AI reasoning itself

## Architecture at a glance

- Frontend: React in the sentinelai-ui folder
- Backend orchestrator: Spring Boot in SentinelAI-Core
- AI gateway: Python FastAPI in SentinelAI-Engine
- Knowledge index service: Spring Boot in SentinelAI-Knowledge-Service

## Main components

### sentinelai-ui
- Two main screens: Log RCA and ELK Investigation
- Calls the Core RCA and ELK endpoints
- Displays structured RCA output and engineering context evidence

### SentinelAI-Core
- Orchestrates RCA generation and live investigation flows
- Integrates with Elasticsearch and the AI Engine
- Optionally calls the Knowledge Service for engineering context
- Exposes REST endpoints for logs, RCA, and ELK investigation

### SentinelAI-Engine
- Routes analysis requests to the configured provider
- Supports Groq, Ollama, HuggingFace, and OpenAI-compatible providers
- Returns structured RCA JSON to Core

### SentinelAI-Knowledge-Service
- Stores normalized engineering metadata only
- Supports webhook-driven ingestion and scheduled or manual sync
- Exposes APIs for timeline, relationships, repositories, deployments, releases, Jira issues, and search

## Current API highlights

### Core APIs
- POST /api/rca/analyze
- POST /api/logs/upload
- POST /api/logs/search
- POST /api/elk-investigation/search

### Knowledge Service APIs
- POST /api/sync/github
- POST /api/sync/jira
- POST /api/sync/confluence
- POST /api/sync/jenkins
- POST /api/sync/all
- POST /api/webhooks/{sourceSystem}
- GET /api/timeline
- GET /api/relationships
- GET /api/repositories
- GET /api/deployments
- GET /api/releases
- GET /api/jira/issues

## Local run

1. Start the supporting infrastructure
   - Postgres/pgvector: postgres-docker-compose.yml
   - Ollama: ollama-docker-compose.yml
   - Elasticsearch: elk-docker-compose.yml
2. Seed sample Elasticsearch data if needed
   - Run elk-seed-data.bat from the repo root
3. Start all services
   - Run start-all.bat from the repo root
4. Or run them individually
   - AI Engine: cd SentinelAI-Engine && python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
   - Core: cd SentinelAI-Core && mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local -Dspring.profiles.active=local
   - UI: cd sentinelai-ui && npm install && npm start
   - Knowledge Service: cd SentinelAI-Knowledge-Service && mvnw.cmd spring-boot:run

## Configuration notes

- Core uses sentinelai.ai-engine.url and sentinelai.ai-engine.provider for AI routing
- ELK investigation is controlled through sentinelai.elk.enabled, sentinelai.elk.url, sentinelai.elk.index, and sentinelai.elk.max-log-results
- Knowledge Service configuration is driven through the sentinelai.knowledge.platforms settings in its application configuration
- The Knowledge Service is currently optional for RCA; if it is unavailable, Core continues RCA generation without engineering context

## Status summary

Implemented now:
- RCA analysis from logs
- Live ELK investigation
- AI provider routing
- Knowledge Service sync and webhook ingestion
- Engineering-context enrichment for RCA

Planned or evolving:
- Deeper incident correlation across systems
- Real-time Kafka-based monitoring
- Automated Jira/Slack remediation workflows
- Stronger production-grade integrations with enterprise platforms

## Repo navigation

- UI: sentinelai-ui/README.md
- Core: SentinelAI-Core/README.md
- AI Engine: SentinelAI-Engine/README.md
- Knowledge Service: SentinelAI-Knowledge-Service/README.md
- ELK integration guide: ELK_Integration_README.md
- Architecture diagrams: SentinelAI-Architecture-v2.drawio, SentinelAI-ELK-Investigation-v1.drawio, SentinelAI-Agentic-Flow-v1.drawio
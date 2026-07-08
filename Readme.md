# SentinelAI (Monorepo)

SentinelAI helps engineers understand logs and incidents by combining:
1) **Memory**: retrieve similar historical incidents (RAG retrieval)
2) **Reasoning**: generate a structured RCA (root cause + fix) via an AI provider
3) **Live Investigation**: query live Elasticsearch logs and receive instant AI-generated analysis without manual file uploads

This repo contains three deployable parts (UI, Core, AI-Engine) plus Docker compose files for local infra.

## What makes it different
The category we are building in is called AIOps (Artificial Intelligence for IT Operations) or Autonomous Incident Remediation. 
Existing Solutions in the Market
Datadog & Dynatrace: They have "AI-powered RCA," but it is often closed-source, heavily dependent on their own platform agents, and expensive.
PagerDuty & BigPanda: They excel at incident correlation (grouping alerts together), but their automated remediation and deep log parsing via LLMs are still evolving.
Kubiya / Anyscale / LangChain-based Agents: Open-source and startup tools that build specialized software engineering "AI Agents" to write fixes or query infrastructure.

What makes your current implementation different?
Vendor Agnosticism: Datadog forces you to keep your logs inside Datadog. Your project sits on top of open-source components (ELK, Postgres) and can swap LLM backends seamlessly.Hybrid Local/Cloud Design: Using Groq for speed while maintaining an immediate fallback to an onsite Ollama model solves a massive compliance problem for enterprise companies who refuse to leak sensitive application log files to public clouds.

What "Brand New" things can you add
                  ┌───────────────────────┐
                  │   Kafka Event Bus     │
                  └───────────┬───────────┘
                              │ Live Stream
                              ▼
┌───────────────────────────────────────────────────────────┐
│              SentinelAI - Advanced AI Layer               │
│                                                           │
│  ┌─────────────────────────┐   ┌───────────────────────┐  │
│  │ 1. Vector Time-Slicing │   │ 2. Predictive Blast   │  │
│  │    (Log Chronology)     │   │    Radius Analysis    │  │
│  └─────────────────────────┘   └───────────────────────┘  │
│  ┌─────────────────────────┐   ┌───────────────────────┐  │
│  │ 3. Automated Git Patch  │   │ 4. Deterministic Guard │  │
│  │    Generation (PRs)     │   │    (No-Hallucination) │  │
│  └─────────────────────────┘   └───────────────────────┘  │
└───────────────────────────────────────────────────────────┘

## Components

- **`SentinelAI/sentinelai-ui` (React)**
  - Single-page UI with two tabs: **Log RCA** and **ELK Investigation**.
  - **Log RCA tab**: paste log text or upload a log file; calls `/api/rca/analyze` and renders the structured RCA response.
  - **ELK Investigation tab**: select a service, severity level, and time range; fetches live logs from Elasticsearch and displays AI-generated analysis.

- **`SentinelAI/SentinelAI-Core` (Java, Spring Boot)**
  - System orchestrator.
  - Optional retrieval (semantic search) over stored incidents.
  - Calls the Python AI-Engine for RCA generation and returns the result to the UI.
  - Stores incidents/embeddings in Postgres (pgvector image is provided for local).
  - **ELK Investigation pipeline**: `ElkClient` → `ElkPromptBuilder` → `PythonAiMlService` → `ElkResponseParser`.

- **`SentinelAI/SentinelAI-Engine` (Python, FastAPI)**
  - AI gateway that routes requests to an AI provider (e.g., `groq` or `ollama`).
  - Returns a stable, structured RCA payload back to Core.

## Data Flow — Log RCA (End-to-End)

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

## Data Flow — ELK Investigation (End-to-End)

1. **User → UI** (`ElkInvestigationPage`)
   - Selects **Service Name**, **Severity** (ERROR / WARN / INFO / DEBUG), and **Time Range** (15 min – 24 hr).
2. **UI → Core**
   - `POST /api/elk-investigation/search` with `{ "service", "severity", "timeframeMinutes" }`.
3. **Core → Elasticsearch** (`ElkClient`)
   - Executes a BoolQuery: `term(service)` + `term(severity)` + `date range(now − X minutes)`.
   - Returns up to 50 log lines, sorted by timestamp DESC, each truncated to 500 chars.
4. **Core → AI-Engine** (`ElkPromptBuilder` + `PythonAiMlService`)
   - Deduplicates and numbers the log lines, then builds a structured prompt requesting JSON with keys `issue`, `rootCause`, `impactedService`, `recommendedFix`, `suspiciousLogs`.
   - Sends prompt to `POST <ai-engine-url>/analyze?provider=groq`.
5. **AI-Engine → Groq** (or configured provider)
   - LLM returns a JSON object with the five fields above.
6. **Core → UI** (`ElkResponseParser`)
   - Parser maps AI response fields into `ElkInvestigationResponse` (summary, probableRootCause, impactedService, recommendedAction, suspiciousLogs).
   - UI renders: **Summary** card, **Key Findings** card, **Suspicious Logs** card.

## API (Core)

### Log RCA
- `POST /api/rca/analyze`
  - Request: `{ "log": "..." }`
  - Response: `{ issue, rootCause, impactedService, recommendedFix, provider, errors }`
- `POST /api/logs/upload`
  - Request: `{ "logType": "text|json|csv", "content": "..." }`
  - Purpose: store incidents + embeddings for future retrieval
- `POST /api/logs/search`
  - Request: `{ "query": "..." }`
  - Purpose: semantic search over stored incidents (requires embeddings/Ollama)

### ELK Investigation
- `POST /api/elk-investigation/search`
  - Request: `{ "service": "payment-service", "severity": "ERROR", "timeframeMinutes": 30 }`
  - Response: `{ summary, probableRootCause, impactedService, recommendedAction, suspiciousLogs[] }`
  - Returns HTTP 503 if `sentinelai.elk.enabled=false` or Elasticsearch is unreachable.

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
- **Elasticsearch 7.x**: live log store; queried via ES Java client BoolQuery for targeted log retrieval.
- **Groq (LLM provider)**: cloud LLM inference (~3 s); `llama-3.3-70b-versatile` model used for both RCA and ELK investigation.
- **Ollama (optional)**: local embeddings (and optionally local LLM) to support fully local development.

## Local Run (Typical)

1. Start infra (optional but typical):
   - Postgres/pgvector: `SentinelAI/postgres-docker-compose.yml`
   - Ollama (for embeddings): `SentinelAI/ollama-docker-compose.yml`
   - Elasticsearch: `SentinelAI/elk-docker-compose.yml` (or use an existing local ES instance)
2. Seed Elasticsearch test data (optional):
   - Run `elk-seed-data.bat` from the root — inserts 15 sample log documents with timestamps in the last 30 minutes.
3. Start all services at once:
   - Run `start-all.bat` from the root — opens separate terminals for Engine (:8000), Core (:8080), and UI (:3000).
4. Or start individually:
   - **AI-Engine**: `cd SentinelAI-Engine && python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`
   - **Core**: `cd SentinelAI-Core && mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local -Dspring.profiles.active=local`
   - **UI**: `cd sentinelai-ui && npm install && npm start`

## Configuration Quick Reference (Core)

- `sentinelai.semantic-search.enabled`
  - `true`: retrieval enabled (requires Ollama embeddings)
  - `false`: retrieval skipped; Core still calls AI-Engine for RCA generation

- `sentinelai.elk.enabled` (default: `true`)
  - `true`: ELK Investigation feature active
  - `false`: endpoint returns HTTP 503 immediately
- `sentinelai.elk.url` — Elasticsearch base URL (default: `http://localhost:9200`)
- `sentinelai.elk.index` — Target index (default: `logs-application`)
- `sentinelai.elk.max-log-results` — Max log lines fetched per query (default: `50`)

## Repo Navigation

- UI: `sentinelai-ui/README.md`
- Core: `SentinelAI-Core/README.md`
- AI-Engine: `SentinelAI-Engine/README.md`
- ELK Integration guide: `ELK_Integration_README.md`
- Architecture diagrams (draw.io): `SentinelAI-Architecture-v2.drawio`, `SentinelAI-ELK-Investigation-v1.drawio`, `SentinelAI-Agentic-Flow-v1.drawio`
- Docker compose files: `postgres-docker-compose.yml`, `ollama-docker-compose.yml`, `elk-docker-compose.yml`
- Tooling scripts: `start-all.bat`, `stop-all.bat`, `elk-seed-data.bat`, `elk-curl-examples.bat`


---
##  AI Layer on top of Observability Stack
#AI Incident Correlation + Auto RCA
Observability Stack
   ↓
Kafka Event Bus
   ↓
SentinelAI AI Layer
   ↓
RCA + Correlation + Automation

##AI-Assisted Incident Investigation Platform
Input:

service=payment-service
time=last 30 mins
severity=ERROR
customer=enterprise-client

##Deployment Regression Detection

✅ P1 (Core MVP Features)

These should be your first major enterprise-grade capabilities beyond current manual upload RCA.

🥇 1. ELK-Based Live Investigation
Use Case

Instead of uploading logs manually, engineers investigate directly from Elasticsearch / Kibana.

User Flow

Engineer selects:

service
timeframe
severity

SentinelAI:

fetches logs from ELK
analyzes failures
generates investigation summary + RCA
Why This Matters

This makes SentinelAI part of the real production workflow.

🥈 2. Natural Language Operational Search
Use Case

Engineer searches using plain English instead of Kibana DSL.

Example
"show payment failures after latest deployment"

SentinelAI:

converts request into ELK query
retrieves logs
summarizes findings

🥉 3. Incident Correlation & Investigation Summary
Use Case

Automatically correlate related failures across systems.

Example
deployment
→ kafka lag
→ db timeout
→ payment failures

🚀 P2 (Advanced Enterprise Features)

After P1 stabilizes.

🏅 4. Real-Time Kafka/ELK Incident Monitoring
Use Case

Continuously monitor production streams.

SentinelAI:

detects anomalies
groups incidents
triggers AI investigation automatically
Example
ELK → Kafka → SentinelAI
🏅 5. Jira + Slack Investigation Automation
Use Case

Automatically create and share investigation summaries.
🎯 Final Recommended Roadmap
Phase	Feature
P1	ELK-based live investigation
P1	Natural language operational search
P1	Incident correlation & AI investigation summary
P2	Real-time Kafka/ELK monitoring
P2	Jira + Slack investigation automation
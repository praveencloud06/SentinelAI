# SentinelAI — ELK / Observability Integration (Roadmap)

Goal: add an enterprise-grade “live logs” layer so SentinelAI can query real-time/production logs (ELK/Elastic) by **service/container + time range + environment + keywords**, and use those logs as evidence for RCA.

## What We Add (New Layer)

### 1) Log Source Connectors (Core-owned)
Add a **Log Source** abstraction in `SentinelAI-Core`:
- `LogSourceClient` (interface)
  - `search(query, timeRange, service/container, env, severity, traceId, limit)`
  - `tail(subscription)` / `stream(...)` (optional for continuous mode)
- Implementation today: `ElasticLogSourceClient` (ElasticSearch query API)
- Future: Loki, Splunk, Datadog, CloudWatch, etc.

Why in Core: enterprise governance (auth, auditing, tenancy, RBAC, rate limits, retries), and it becomes a stable “tool API” for agentic workflows.

### 2) “Live RCA” API (Core)
New endpoint(s) in Core (proposed):
- `POST /api/live/rca`
  - Request: filters + time window + question
  - Core queries Elastic → optionally retrieves historical incidents (vector store) → sends evidence to AI-Engine → returns RCA

Optional:
- `POST /api/live/search` (returns raw log samples + aggregations)

### 3) Continuous Monitoring (Optional)
Two enterprise patterns:
- **Scheduled polling**: run queries every N minutes (simple)
- **Event-driven**: ship logs → detect incidents → trigger RCA (best at scale)

For event-driven:
- Use Kafka as the backbone (logs/alerts/incidents), and trigger RCA jobs when conditions match.

## How RCA Uses ELK Data (Flow)

1. User specifies filters: service/container, env/cluster, time range, keywords/error codes, (optional) traceId.
2. Core queries Elastic and returns:
   - sampled log lines
   - extracted exceptions/stack traces
   - aggregations (error counts over time, top messages, top pods)
3. Core builds a compact “evidence pack”:
   - key errors + top patterns + representative snippets (avoid sending megabytes)
4. Core calls AI-Engine for structured RCA generation:
   - provider chosen via `sentinelai.ai-engine.provider`
5. Output is returned to UI; optionally user confirms → create Jira.

## Enterprise Tech Stack (Recommended)

### Observability / Logs
- **ElasticSearch + Kibana** (source of truth for logs)
- **Elastic Agent / Filebeat** (shipping logs)
- **APM + Tracing**: OpenTelemetry + Elastic APM (or OTEL → any backend)

### Orchestration / APIs
- **Spring Boot (Core)**: connectors, auth, APIs, retrieval, governance
- **FastAPI (AI-Engine)**: provider adapters + (later) agent runtime

### Streaming / Jobs (when you scale)
- **Kafka**: incident events + workflow triggers
- **Flink / Spark / Kafka Streams**: high-volume log enrichment/detection (optional)
- **Quartz/Spring Scheduler**: simple polling jobs (MVP)

### Retrieval / RAG Store
- **Today (simple)**: Postgres + embeddings (MVP)
- **Later (scale)**: **Qdrant** for vectors + Postgres for incident metadata

### Security / Governance
- **OAuth2/OIDC** (Keycloak / Okta / Azure AD)
- **RBAC + audit logging**
- **Secrets**: Vault / cloud secret manager

### Deployment
- **Kubernetes**
- Elastic on K8s via **ECK** (Elastic Cloud on Kubernetes)
- Observability via **OpenTelemetry Collector**

## Local Development Setup (Suggested)

For local ELK testing, run Elastic + Kibana in Docker:
- ElasticSearch (single node) + Kibana
- Configure Core with:
  - `sentinelai.log-source.type=elastic`
  - `sentinelai.elastic.url=http://localhost:9200`
  - credentials if enabled

You can start with “no security” locally and enable auth later.

## What We Need to Build Next (Concrete Checklist)

1. **Core**
   - Add `LogSourceClient` interface + `ElasticLogSourceClient`
   - Add request DTO for “live query” (service/container/time range/etc)
   - Add `POST /api/live/rca`
   - Add a “evidence pack” builder (chunking + summarization rules)
2. **AI-Engine**
   - Ensure prompts accept “evidence pack” and return strict RCA JSON
3. **UI**
   - Add a “Live Logs” screen (filters + time picker + preview logs + run RCA)
4. **Ops**
   - Local docker-compose for elastic+kibana
   - Add basic rate limiting + timeouts for Elastic queries

## Notes on “Continuous RCA”

Continuous mode should be a separate capability:
- detection rules (thresholds/anomalies) trigger “investigate”
- agentic workflow can:
  - fetch more logs
  - correlate across services
  - propose RCA + confidence
  - open a Jira ticket for review


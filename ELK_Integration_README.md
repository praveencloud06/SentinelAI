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
---

## ELK Investigation Feature — Local Run & Testing Guide

> This section covers the **implemented** ELK Investigation feature.  
> All commands assume the project root `SentinelAI/` as the working directory.

---

### Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Docker Desktop | any recent | must be running |
| Java | 17+ | for Spring Boot |
| Maven | 3.8+ | or use `mvnw` |
| Node / npm | 18+ | for React UI |
| curl | built-in | Windows 10/11 ships with curl |

---

### Step 1 — Start Postgres (existing)

```bat
docker compose -f postgres-docker-compose.yml up -d
```

---

### Step 2 — Start Elasticsearch + Kibana (new)

```bat
docker compose -f elk-docker-compose.yml up -d
```

Wait ~30 seconds for Elasticsearch to be healthy, then verify:

```bat
curl http://localhost:9200/_cluster/health?pretty
```

Expected: `"status": "green"` or `"yellow"` (never `"red"`).

Kibana UI → http://localhost:5601

---

### Step 3 — Load sample log data

```bat
elk-seed-data.bat
```

This script:
1. Creates the `logs-application` index with correct field mappings
2. Bulk-inserts 15 sample documents across `payment-service`, `order-service`, and `auth-service`

Verify in Elasticsearch:

```bat
curl "http://localhost:9200/logs-application/_count"
```

Expected: `{"count":15,...}`

---

### Step 4 — Start the AI Engine (Python)

```bat
cd SentinelAI-Engine
pip install -r requirements.txt
python main.py
```

Running on: http://localhost:8000

---

### Step 5 — Start Spring Boot backend

```bat
cd SentinelAI-Core
mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Running on: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui/index.html

The `local` profile sets `sentinelai.elk.enabled=true` and points to `http://localhost:9200`.

---

### Step 6 — Start the React UI

```bat
cd sentinelai-ui
npm install
npm start
```

Running on: http://localhost:3000

Click the **ELK Investigation** tab in the top navigation bar.

---

### Testing the API

#### Via the UI

1. Open http://localhost:3000
2. Click **ELK Investigation**
3. Enter `payment-service`, severity `ERROR`, time range `Last 30 minutes`
4. Click **Investigate**
5. View the Summary, Key Findings, and Suspicious Logs cards

#### Via curl (copy individual commands as needed)

**Basic investigation:**
```bat
curl -s -X POST http://localhost:8080/api/elk-investigation/search ^
  -H "Content-Type: application/json" ^
  -d "{\"service\":\"payment-service\",\"severity\":\"ERROR\",\"timeframeMinutes\":30}"
```

**Pretty-print the response:**
```bat
curl -s -X POST http://localhost:8080/api/elk-investigation/search ^
  -H "Content-Type: application/json" ^
  -d "{\"service\":\"payment-service\",\"severity\":\"ERROR\",\"timeframeMinutes\":60}" ^
  | python -m json.tool
```

**order-service investigation:**
```bat
curl -s -X POST http://localhost:8080/api/elk-investigation/search ^
  -H "Content-Type: application/json" ^
  -d "{\"service\":\"order-service\",\"severity\":\"ERROR\",\"timeframeMinutes\":60}"
```

**Validation error test (missing service):**
```bat
curl -s -X POST http://localhost:8080/api/elk-investigation/search ^
  -H "Content-Type: application/json" ^
  -d "{\"severity\":\"ERROR\",\"timeframeMinutes\":30}"
```
Expected: HTTP 400

**No-results test (service does not exist):**
```bat
curl -s -X POST http://localhost:8080/api/elk-investigation/search ^
  -H "Content-Type: application/json" ^
  -d "{\"service\":\"nonexistent-service\",\"severity\":\"ERROR\",\"timeframeMinutes\":30}"
```
Expected: response with `"No logs matching..."` summary and empty `suspiciousLogs`

Run all examples at once:
```bat
elk-curl-examples.bat
```

#### Verify existing RCA still works (backward-compat check)

```bat
curl -s -X POST http://localhost:8080/api/rca/analyze ^
  -H "Content-Type: application/json" ^
  -d "{\"log\":\"ERROR: NullPointerException in PaymentService at line 42\"}"
```

---

### Expected API Response Shape

```json
{
  "summary": "Multiple Kafka timeout errors and database pool exhaustion detected in payment-service over the past 30 minutes...",
  "probableRootCause": "Kafka broker unavailability causing consumer lag and cascading connection pool exhaustion",
  "impactedService": "payment-service",
  "recommendedAction": "Check Kafka broker health; consider increasing connection pool size and circuit-breaker thresholds",
  "suspiciousLogs": [
    "Kafka timeout while processing invoice INV-8821 – consumer group lag exceeded 5000",
    "Database connection pool exhausted – all 20 connections in use (pool: payment-db-pool)",
    "Circuit breaker OPEN for downstream service billing-service after 10 consecutive failures"
  ]
}
```

---

### Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `503 ELK integration is disabled` | `elk.enabled=false` | Ensure you use `-Dspring-boot.run.profiles=local` |
| `503 Failed to query Elasticsearch` | ES not running | `docker compose -f elk-docker-compose.yml up -d` |
| `"No logs matching..."` in summary | Seed data not loaded or wrong timeframe | Run `elk-seed-data.bat`; use a wider timeframe |
| AI response is empty | AI Engine not running | Start `python main.py` in `SentinelAI-Engine/` |
| Port 9200 already in use | Another ES instance | Stop it or change the port in `elk-docker-compose.yml` |

---

### Adding Your Own Log Data

To index your own log documents manually:

```bat
curl -s -X POST "http://localhost:9200/logs-application/_doc" ^
  -H "Content-Type: application/json" ^
  -d "{\"timestamp\":\"2026-05-07T10:00:00Z\",\"service\":\"my-service\",\"severity\":\"ERROR\",\"message\":\"Your log message here\",\"traceId\":\"xyz123\",\"environment\":\"local\"}"
```

Required fields: `timestamp` (ISO-8601), `service` (keyword), `severity` (keyword), `message` (text).


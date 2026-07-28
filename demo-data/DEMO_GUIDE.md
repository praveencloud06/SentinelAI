# SentinelAI V2 - Enterprise Demonstration Guide

**Version:** 2.0  
**Date:** 2026-07-23  
**Purpose:** Step-by-step guide for demonstrating SentinelAI's AI-assisted engineering investigation capabilities

---

## Table of Contents

1. [Overview](#overview)
2. [Demo Environment Setup](#demo-environment-setup)
3. [Demo Story & Presentation Flow](#demo-story--presentation-flow)
4. [Demo 1: Log RCA](#demo-1-log-rca)
5. [Demo 2: ELK Investigation](#demo-2-elk-investigation)
6. [Demo 3: Engineering Context Explorer](#demo-3-engineering-context-explorer)
7. [Key Messages](#key-messages)
8. [Troubleshooting](#troubleshooting)

---

## Overview

This demonstration showcases SentinelAI V2's three core capabilities:

1. **Log RCA** - AI-assisted root cause analysis from log files
2. **ELK Investigation** - Semantic search across Elasticsearch logs with engineering context
3. **Engineering Context Explorer** - Unified semantic search across GitHub, Jira, Confluence, Jenkins, and incidents

All three modules share the same **SentinelAI Knowledge Service**, enabling consistent semantic understanding across your entire engineering knowledge base.

---

## Demo Environment Setup

### 1. Prerequisites

Ensure the following are installed and running:

- Java 17+
- Node.js 18+
- PostgreSQL 13+
- Elasticsearch 8.x
- Ollama (with embedding model) OR OpenAI API key

### 2. Start Infrastructure

```powershell
# Start PostgreSQL
docker-compose -f postgres-docker-compose.yml up -d

# Start Elasticsearch
docker-compose -f elk-docker-compose.yml up -d

# Start Ollama (if using local embeddings)
docker-compose -f ollama-docker-compose.yml up -d
```

### 3. Start SentinelAI Services

**Terminal 1 - AI Engine:**
```powershell
cd SentinelAI-Engine
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

**Terminal 2 - Core Service:**
```powershell
cd SentinelAI-Core
mvn spring-boot:run
```

**Terminal 3 - Knowledge Service:**
```powershell
cd SentinelAI-Knowledge-Service
mvn spring-boot:run
```

**Terminal 4 - UI:**
```powershell
cd sentinelai-ui
npm install
npm start
```

### 4. Verify Services

- **UI:** http://localhost:3000
- **Core API:** http://localhost:8080/actuator/health
- **Knowledge Service:** http://localhost:8090/actuator/health
- **AI Engine:** http://localhost:8000/
- **Elasticsearch:** http://localhost:9200

All should return healthy status.

### 5. Load Demo Data

**Load ELK logs:**
```powershell
.\elk-seed-data.ps1
```

**Load Knowledge Service data:**
```powershell
cd demo-data\knowledge
.\knowledge-demo-data.ps1
```

**Verify data:**
```powershell
# Check ELK
curl http://localhost:9200/sentinelai-logs/_count

# Check Knowledge Service
curl http://localhost:8090/api/repositories
curl http://localhost:8090/api/jira/issues
```

---

## Demo Story & Presentation Flow

### The Scenario

You are a Site Reliability Engineer (SRE) at a large enterprise. It's Monday morning, and you have three production incidents to investigate:

1. Payment service experiencing database connection timeouts
2. Order service showing high Kafka consumer lag
3. Multiple services reporting intermittent failures

Traditional investigation would require:
- Manually searching logs across multiple systems
- Checking GitHub for recent deployments
- Looking up Jira tickets
- Reading Confluence documentation
- Correlating all this information manually

**With SentinelAI, AI does the correlation for you.**

### Recommended Presentation Order

1. **Start with Log RCA** (simplest) - Shows basic AI analysis
2. **Move to ELK Investigation** (intermediate) - Shows semantic search across logs
3. **Finish with Engineering Context Explorer** (most powerful) - Shows unified knowledge retrieval

This builds progressively and shows how the same knowledge base powers all three features.

---

## Demo 1: Log RCA

### Objective
Show how AI can analyze raw log files and provide structured root cause analysis enriched with engineering context.

### Demo Steps

1. **Navigate to Log RCA**
   - Open http://localhost:3000
   - Click **"Log RCA"** tab

2. **Upload Incident Log**
   - Click **"Or Upload Log File"**
   - Select: `demo-data/rca/incident-1-payment-hikari-pool-exhaustion.log`
   - Click **"Analyze Log"**

3. **Wait for Analysis** (~10-30 seconds)
   - Show the AI is processing the log
   - Mention it's calling the AI Engine (Python FastAPI service)

4. **Review Results**

   **Expected Issue:**
   > Payment service database connection pool exhaustion

   **Expected Root Cause:**
   > HikariCP connection pool exhausted due to increased database transaction timeout from 5s to 30s in v2.1.5 deployment. All 20 connections in pool were active, preventing new transactions.

   **Expected Impacted Service:**
   > payment-service

   **Expected Recommended Fix:**
   > 1. Immediately rollback to v2.1.4 which had 5s timeout
   > 2. Review commit abc123def456 that increased timeout
   > 3. If timeout increase is needed, also increase pool size from 20 to 40
   > 4. Add connection pool monitoring alerts
   > 5. Review Jira issue PAY-421 and Confluence guide on HikariCP configuration

5. **Highlight Engineering Context Panel**
   - Click **"Engineering Context Used"** to expand
   - Show **Summary** (e.g., "Found 3 related deployments and 2 similar incidents")
   - Show **Confidence Score** (e.g., 0.87 / 1.0)
   - Show **Evidence Count** (e.g., 5 engineering artifacts)
   - Expand to show evidence details:
     - GitHub commit abc123def456
     - Jira issue PAY-421
     - Confluence page on Hikari configuration
     - Deployment Build #1245
     - Previous incident INC-2024-089

### Key Points to Emphasize

✅ **AI read the logs and identified the problem**  
✅ **Engineering context automatically retrieved** (commits, Jira, docs)  
✅ **Previous similar incident found** (INC-2024-089 from 2024)  
✅ **Actionable recommendations provided** (not just "fix it")  
✅ **All in under 30 seconds** vs hours of manual investigation

### Expected Questions

**Q: Where does the AI get the engineering context?**  
A: From the SentinelAI Knowledge Service, which indexes GitHub, Jira, Confluence, Jenkins, and past incidents. Everything is semantically searchable.

**Q: Does it work with other log formats?**  
A: Yes! It works with any text log format - Spring Boot, Node.js, Python, custom formats. The AI understands log patterns.

**Q: What if Knowledge Service is down?**  
A: It gracefully degrades. You still get the AI analysis, just without the enriched engineering context.

---

## Demo 2: ELK Investigation

### Objective
Show how AI can search Elasticsearch logs, identify suspicious patterns, and enrich findings with engineering context.

### Demo Steps

1. **Navigate to ELK Investigation**
   - Click **"ELK Investigation"** tab

2. **Configure Search**
   - **Service Name:** `payment-service`
   - **Severity:** `ERROR`
   - **Timeframe:** `60` minutes
   - Click **"Investigate"**

3. **Wait for Analysis** (~15-45 seconds)
   - AI is querying Elasticsearch
   - Retrieving engineering context
   - Analyzing patterns

4. **Review Results**

   **Expected Summary:**
   > Payment service experiencing database connection pool exhaustion. Multiple SQLTransientConnectionException errors detected starting at 14:30 UTC. Connection pool (max 20) fully active with 28 threads waiting.

   **Expected Probable Root Cause:**
   > HikariCP connection pool configuration change in recent deployment (v2.1.5, Build #1245). Database transaction timeout increased from 5s to 30s without corresponding pool size increase. This caused long-running transactions to hold connections, exhausting the pool.

   **Expected Impacted Service:**
   > payment-service

   **Expected Recommended Action:**
   > 1. Immediate: Rollback to v2.1.4 (Build #1246 already deployed at 15:00 UTC)
   > 2. Short-term: Review commit abc123def456 that changed timeout settings
   > 3. Long-term: Implement connection pool monitoring, review HikariCP sizing guide in Confluence
   > 4. Reference: Jira PAY-421, Previous incident INC-2024-089

5. **Review Suspicious Logs**
   - Show ~10-20 log entries AI identified as relevant
   - Highlight connection timeout errors
   - Show HikariCP pool exhaustion warnings

6. **Highlight Engineering Context Panel**
   - Expand "Engineering Context Used"
   - Show same enrichment as Log RCA
   - Emphasize: **Same knowledge base, different entry point**

### Key Points to Emphasize

✅ **AI searched thousands of logs in Elasticsearch**  
✅ **Identified suspicious patterns automatically**  
✅ **Correlated with deployments, commits, and docs**  
✅ **Provided context-aware recommendations**  
✅ **Same engineering knowledge as Log RCA** (demonstrates unified platform)

### Expected Questions

**Q: What if there are millions of logs?**  
A: Elasticsearch handles the scale. We typically query last N hours/days. AI focuses on ERROR/WARN severity for investigation.

**Q: Can it detect patterns across multiple services?**  
A: Yes! You can search across all services or filter by specific services. AI correlates related failures.

**Q: How does it know which logs are "suspicious"?**  
A: AI uses multiple signals: severity, frequency, timing, error patterns, and semantic similarity to known issues.

---

## Demo 3: Engineering Context Explorer

### Objective
Show the most powerful feature: semantic search across your entire engineering knowledge base (GitHub, Jira, Confluence, Jenkins, incidents) using natural language or logs.

### Demo Scenarios

#### Scenario A: Search by Log Pattern

1. **Navigate to Engineering Context Explorer**
   - Click **"Engineering Context Explorer"** tab

2. **Paste Log Sample**
   - Click **"Logs"** mode (default)
   - Paste the following:
     ```
     ERROR HikariPool-1 - Connection is not available, request timed out after 30017ms
     WARN  HikariPool-1 - Connection pool exhausted. Active=20, Idle=0, Waiting=28
     ```

3. **Configure Search** (optional filters)
   - Service: `payment-service` (optional)
   - Max Results: `10`
   - Click **"Search"**

4. **Review Results**

   **Expected Context Summary:**
   > Found 5 related engineering artifacts related to HikariCP connection pool issues in payment-service. High confidence match with commit abc123def456 and Jira PAY-421.

   **Expected Evidence Cards:**
   
   **1. GitHub Commit - abc123def456**
   - **Title:** "Increase database timeout for long-running transactions"
   - **Relevance Score:** 0.92
   - **Why Selected:**
     - ✓ Same payment-service repository
     - ✓ Modified HikariCP configuration
     - ✓ Deployed 15 minutes before incident
     - ✓ Changed timeout from 5s to 30s
     - ✓ High semantic similarity to error message
   
   **2. Jira Issue - PAY-421**
   - **Title:** "Payment processing timeout errors"
   - **Relevance Score:** 0.89
   - **Why Selected:**
     - ✓ Exact same error signature
     - ✓ References commit abc123def456
     - ✓ Status: Resolved (solution available)
     - ✓ Contains resolution steps
   
   **3. Confluence Page**
   - **Title:** "Payment Service Database Configuration Guide"
   - **Relevance Score:** 0.85
   - **Why Selected:**
     - ✓ Covers HikariCP configuration
     - ✓ Troubleshooting section on pool exhaustion
     - ✓ Recommends pool sizing guidelines
   
   **4. Deployment - Build #1245**
   - **Title:** "payment-service v2.1.5 deployed to production"
   - **Relevance Score:** 0.87
   - **Why Selected:**
     - ✓ Deployed commit abc123def456
     - ✓ Timestamp: 8 minutes before error spike
     - ✓ Environment: Production
   
   **5. Previous Incident - INC-2024-089**
   - **Title:** "HikariCP Connection Pool Exhaustion (2024-03-15)"
   - **Relevance Score:** 0.83
   - **Why Selected:**
     - ✓ Nearly identical symptoms
     - ✓ Same root cause (timeout vs pool size mismatch)
     - ✓ Contains resolution playbook

5. **Highlight Key Features**
   - **Relevance Scores:** AI ranks results by relevance
   - **Explainability:** Each card shows WHY it was selected
   - **Timeline:** Shows temporal relationship between events
   - **Relationships:** Shows how artifacts connect (commit → deployment → incident)

#### Scenario B: Natural Language Search

1. **Switch to Natural Language Mode**
   - Click **"Natural Language"** tab

2. **Ask a Question**
   - Type: `"Find similar payment incidents"`
   - Or: `"Show deployments related to payment-service"`
   - Or: `"Database connection pool exhausted"`
   - Click **"Search"**

3. **Review Results**
   - Same evidence cards as before
   - But retrieved via natural language understanding
   - No need to know exact service names or error codes

#### Scenario C: Multi-Service Investigation

1. **Search Pattern**
   - Type: `"Kafka consumer lag"`
   - Click **"Search"**

2. **Review Results**

   **Expected Evidence:**
   - Commit 789ghi012jkl - Kafka consumer configuration
   - Jira ORD-334 - Consumer lag issue
   - Confluence page on Kafka troubleshooting
   - Deployment Build #2108 for order-service
   - Previous incident INC-2025-156

3. **Show Cross-Service Correlation**
   - Demonstrate how the same search approach works across different services
   - Highlight unified knowledge base

### Key Points to Emphasize

✅ **Semantic search** - Finds relevant info even with different wording  
✅ **Cross-system correlation** - One search across Git, Jira, docs, deployments  
✅ **AI explainability** - Shows WHY each result was selected  
✅ **Temporal awareness** - Understands time relationships  
✅ **Previous incident learning** - Learns from past problems  
✅ **Natural language** - No need to memorize service names or error codes

### Expected Questions

**Q: How does semantic search work?**  
A: We generate embeddings (vector representations) of all engineering artifacts using AI. Similar concepts have similar vectors, enabling semantic matching beyond keyword search.

**Q: Does it work for services not in the demo?**  
A: Yes! As long as data is synced to Knowledge Service (GitHub, Jira, etc.), it works for any service.

**Q: Can it search private repositories?**  
A: Yes, with appropriate API tokens. Data never leaves your infrastructure.

**Q: How fresh is the data?**  
A: Real-time via webhooks, or periodic sync (hourly/daily). Configurable per source.

---

## Key Messages

### For Engineering Leadership

1. **Reduce MTTR** - Mean Time To Resolution drops from hours to minutes
2. **Leverage Institutional Knowledge** - Past incidents inform future investigations
3. **Onboard Faster** - New engineers can quickly find relevant context
4. **Improve Reliability** - Learn from patterns across all incidents

### For SRE/Operations Teams

1. **AI does the correlation** - No more manually checking 5 different systems
2. **Context-aware recommendations** - Not just "what" but "why" and "how to fix"
3. **Works with existing tools** - GitHub, Jira, Confluence, Jenkins, Elasticsearch
4. **Graceful degradation** - If Knowledge Service is down, core RCA still works

### For Development Teams

1. **Faster debugging** - Paste logs, get root cause + fix recommendations
2. **Learn from history** - See how similar issues were resolved before
3. **Better documentation** - AI surfaces relevant runbooks and guides
4. **Deployment safety** - Correlate deployments with incidents immediately

---

## Troubleshooting

### UI Shows "No engineering context available"

**Cause:** Knowledge Service not running or has no data

**Fix:**
1. Check Knowledge Service health: http://localhost:8090/actuator/health
2. Verify data loaded: `curl http://localhost:8090/api/repositories`
3. Re-run: `.\demo-data\knowledge\knowledge-demo-data.ps1`

### Log RCA returns empty result

**Cause:** AI Engine not running or model not loaded

**Fix:**
1. Check AI Engine: http://localhost:8000/
2. Verify Ollama running: `docker ps | grep ollama`
3. Check AI Engine logs for errors

### ELK Investigation shows "No logs found"

**Cause:** Elasticsearch not seeded or wrong service name

**Fix:**
1. Check Elasticsearch: `curl http://localhost:9200/_cat/indices`
2. Re-seed: `.\elk-seed-data.ps1`
3. Use exact service name: `payment-service` (not `payment`)

### Engineering Context Explorer returns 0 results

**Cause:** Knowledge Service has no data or embeddings not generated

**Fix:**
1. Check: `curl http://localhost:8090/api/repositories`
2. Check AI Engine embedding endpoint: `curl http://localhost:8000/api/embeddings -X POST -H "Content-Type: application/json" -d '{"text":"test"}'`
3. Re-run: `.\demo-data\knowledge\knowledge-demo-data.ps1`
4. Wait 1-2 minutes for embeddings to generate

### Services won't start

**Cause:** Port conflicts or missing dependencies

**Fix:**
1. Check ports: `netstat -ano | findstr "8080 8090 8000 3000 9200 5432"`
2. Kill conflicting processes
3. Check PostgreSQL running: `docker ps | grep postgres`
4. Check all docker-compose services: `docker-compose ps`

---

## Demo Checklist

Before the demo:

- [ ] All services started and healthy
- [ ] ELK data seeded (verify log count)
- [ ] Knowledge Service data loaded (verify repository count)
- [ ] Embeddings generated (wait 2-3 minutes after data load)
- [ ] Test Log RCA with incident-1 log file
- [ ] Test ELK Investigation with payment-service
- [ ] Test Context Explorer with sample search
- [ ] Browser tabs ready (UI, Kibana, GitHub)
- [ ] Demo files organized and accessible

During the demo:

- [ ] Start with the scenario story
- [ ] Progress from simple (RCA) to complex (Explorer)
- [ ] Emphasize AI explainability ("why was this selected")
- [ ] Show engineering context enrichment
- [ ] Highlight cross-system correlation
- [ ] End with key business value messages

---

## Additional Demo Ideas

### Advanced Scenarios

1. **Show real-time correlation**
   - Trigger a deployment
   - Show it appears in timeline immediately

2. **Compare manual vs AI investigation**
   - Show how long manual search takes
   - Show AI does it in seconds

3. **Demonstrate learning**
   - Show old incident
   - Show new incident
   - Show AI recommends solution from old incident

4. **Multi-language support**
   - Show logs in different formats (Java, Python, Node.js)
   - Show AI understands all of them

---

## Success Metrics

After demo, audience should understand:

1. ✅ What SentinelAI does (AI-assisted engineering investigation)
2. ✅ Why it's valuable (faster MTTR, institutional knowledge)
3. ✅ How it works (semantic search + AI analysis)
4. ✅ How to use it (three simple interfaces)
5. ✅ Why it's better than manual search (speed + accuracy)

---

## Next Steps After Demo

1. **For POC:** Sync real GitHub/Jira data
2. **For Production:** Configure webhooks for real-time sync
3. **For Scale:** Deploy on Kubernetes
4. **For Security:** Add authentication and authorization

---

**Questions?** Contact the SentinelAI team or refer to the full documentation in the repository.

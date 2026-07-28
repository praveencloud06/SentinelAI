# SentinelAI Demo Environment - Status

**Last Updated:** 2026-07-23

## ✅ Services Running

| Service | Port | Status | Health Check |
|---------|------|--------|--------------|
| **SentinelAI-Core** | 8080 | ✅ Running | http://localhost:8080/actuator/health |
| **SentinelAI-Knowledge-Service** | 8090 | ✅ Running | http://localhost:8090/actuator/health |
| **PostgreSQL** (Core) | 5432 | ✅ Running | sentinel/sentinel_ai |
| **PostgreSQL** (Knowledge) | 5433 | ✅ Running | sentinel/sentinelai_knowledge |
| **Elasticsearch** | 9200 | ✅ Running | http://localhost:9200 |
| **Kibana** | 5601 | ✅ Running | http://localhost:5601 |
| **Ollama** (Optional) | 11434 | ⚠️ Optional | http://localhost:11434 |

## ✅ Demo Data Loaded

### Knowledge Service Data
- **Status:** ✅ Loaded
- **Incidents:** 2 (PAY-421, ORD-334)
- **Data Types:** GitHub commits, Jira issues, Confluence pages, Jenkins deployments
- **Loader:** `demo-data/knowledge/knowledge-demo-data-simple.ps1`
- **Last Run:** Check service logs

### ELK Demo Data
- **Status:** ✅ Loaded with Current Timestamps
- **Documents:** 39 log entries
- **Incidents:** 5 production incidents (30min to 12hrs ago)
- **Index:** `sentinelai-logs`
- **Loader:** `demo-data/elk/elk-seed-demo-incidents.bat`
- **Last Run:** Timestamps are always current (regenerated on each run)

### RCA Log Files
- **Status:** ✅ Available
- **Location:** `demo-data/rca/*.log`
- **Files:** 5 incident log files ready for upload
- **Usage:** Upload via UI for RCA analysis

## 🎯 Demo Scenarios

### 1. Log RCA (Root Cause Analysis)
**Status:** ✅ Ready

1. Navigate to "Log RCA" in UI
2. Upload any log file from `demo-data/rca/`
3. Example: `incident-1-payment-hikari-pool-exhaustion.log`
4. AI analyzes logs and provides root cause

### 2. ELK Investigation
**Status:** ✅ Ready

1. Navigate to "ELK Investigation" in UI
2. Search with:
   - Service: `payment-service`
   - Severity: `ERROR`
   - Time Range: **Last 24 hours** ✅
3. AI analyzes recent logs from Elasticsearch
4. Correlates with Knowledge Service context

### 3. Engineering Context Explorer
**Status:** ✅ Ready (Stub Implementation)

1. Navigate to "Engineering Context" in UI
2. Search for:
   - "payment timeout" → Returns PAY-421 context
   - "kafka lag" → Returns ORD-334 context
3. Shows: GitHub commits, Jira issues, deployments

**Note:** This is a stub implementation with keyword matching. Full vector search (V2 feature) requires embedding configuration.

## 🔧 Configuration Status

### Core Service
- ✅ PostgreSQL connected (localhost:5432)
- ✅ Elasticsearch connected (sentinelai-logs index)
- ✅ Context retrieval endpoints working
- ✅ ELK investigation working with current timestamps

### Knowledge Service
- ✅ PostgreSQL connected with pgvector (localhost:5433)
- ✅ Webhook ingestion working
- ✅ Context retrieval stub endpoints working
- ⚠️ Embedding disabled by default (no API keys configured)

### Embedding Configuration (Optional)
Located in `SentinelAI-Knowledge-Service/src/main/resources/application.yml`:

```yaml
sentinelai:
  knowledge:
    embedding:
      enabled: false  # Set to true after configuring a provider
      provider: openrouter  # Options: openrouter, openai, ollama, azure
      
      openrouter:
        api-key: ${OPENROUTER_API_KEY:}
        base-url: https://openrouter.ai/api/v1
        model: google/gemini-2.0-flash-exp:free
        
      # ... other providers
```

**To enable:**
1. Set environment variable or update API key in config
2. Change `enabled: false` to `enabled: true`
3. Restart Knowledge Service
4. Existing documents will be embedded automatically

## 📊 Data Verification Commands

### Check Knowledge Service Data
```powershell
# Check event count
Invoke-RestMethod http://localhost:8090/actuator/health
```

### Check ELK Data
```powershell
# Document count
Invoke-RestMethod http://localhost:9200/sentinelai-logs/_count

# Recent logs
$query = '{"size":5,"sort":[{"timestamp":"desc"}]}'
Invoke-RestMethod -Uri "http://localhost:9200/sentinelai-logs/_search" `
  -Method Post -ContentType "application/json" -Body $query
```

### Check Context Retrieval
```powershell
# Test Core context endpoint
$body = '{"query":"payment timeout","limit":5}'
Invoke-RestMethod -Uri "http://localhost:8080/api/context/retrieve" `
  -Method Post -ContentType "application/json" -Body $body

# Test Knowledge Service stub
$body = '{"query":"payment pool exhaustion","maxResults":5}'
Invoke-RestMethod -Uri "http://localhost:8090/api/context/retrieve" `
  -Method Post -ContentType "application/json" -Body $body
```

## 🚀 Start Demo Environment

### Option 1: All Services
```bash
# Terminal 1: Databases + ELK
docker-compose -f postgres-docker-compose.yml up -d
docker-compose -f elk-docker-compose.yml up -d

# Terminal 2: Core Service
cd SentinelAI-Core
mvn spring-boot:run

# Terminal 3: Knowledge Service
cd SentinelAI-Knowledge-Service
mvn spring-boot:run

# Wait for services to start, then load data
cd demo-data\knowledge
.\knowledge-demo-data-simple.ps1

cd ..\elk
.\elk-seed-demo-incidents.bat
```

### Option 2: Quick Reload Demo Data Only
```bash
# Assumes services already running
cd demo-data\knowledge
.\knowledge-demo-data-simple.ps1

cd ..\elk
.\elk-seed-demo-incidents.bat
```

## 📝 Known Issues & Limitations

### Resolved Issues
- ✅ Knowledge Service startup (Java 21→17, @Primary on SemanticChunker)
- ✅ Context Explorer 404s (stub endpoints created)
- ✅ ELK index mismatch (changed to sentinelai-logs)
- ✅ ELK timestamps (now generates current timestamps on each run)
- ✅ NDJSON format (fixed line endings and final newline)

### Current Limitations
1. **Context Explorer:** Stub implementation with keyword matching (full vector search is V2)
2. **Embedding:** Disabled by default (requires API key configuration)
3. **AI Analysis:** Requires AI provider configuration (Ollama, OpenRouter, etc.)

## 🔗 Quick Links

- **UI:** http://localhost:3000 (if frontend running)
- **Core API:** http://localhost:8080
- **Knowledge API:** http://localhost:8090
- **Elasticsearch:** http://localhost:9200
- **Kibana:** http://localhost:5601

## 📚 Documentation

- [Demo Data Guide](DEMO_DATA_GUIDE.md) - Complete demo data documentation
- [ELK Demo README](demo-data/elk/README.md) - ELK-specific instructions
- [Knowledge Demo README](demo-data/knowledge/README.md) - Knowledge Service data
- [Architecture V2](ARCHITECTURE_V2.md) - System architecture overview

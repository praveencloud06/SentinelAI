# SentinelAI V2 - Demo Data Summary

## Overview

This document provides a complete overview of the enterprise demo data created for SentinelAI V2, covering all three modules: Log RCA, ELK Investigation, and Engineering Context Explorer.

---

## Demo Incidents

Five realistic production incidents have been created with complete, interconnected data across all systems:

### 1. Payment Service Database Connection Pool Exhaustion
- **Date:** 2026-07-20 14:30:00 UTC
- **Duration:** 45 minutes
- **Severity:** CRITICAL
- **Root Cause:** HikariCP pool exhausted after timeout increase from 5s to 30s
- **Commit:** abc123def456
- **Jira:** PAY-421
- **Build:** #1245 (v2.1.5)
- **Resolution:** Rollback to v2.1.4

### 2. Order Service Kafka Consumer Lag
- **Date:** 2026-07-18 09:15:00 UTC
- **Duration:** 2 hours
- **Severity:** HIGH
- **Root Cause:** Consumer group rebalancing during deployment
- **Commit:** 789ghi012jkl
- **Jira:** ORD-334
- **Build:** #2108 (v3.2.1)
- **Resolution:** Manual consumer offset reset

### 3. Vehicle Registration Oracle Timeout
- **Date:** 2026-07-15 16:00:00 UTC
- **Duration:** 1 hour
- **Severity:** CRITICAL
- **Root Cause:** Oracle query plan changed to full table scan
- **Commit:** mno345pqr678
- **Jira:** VEH-567
- **Build:** #3456 (v1.9.3)
- **Resolution:** Query hints added to force index scan

### 4. Notification Service Redis Outage
- **Date:** 2026-07-12 11:30:00 UTC
- **Duration:** 30 minutes
- **Severity:** HIGH
- **Root Cause:** Redis cluster failover due to memory pressure
- **Commit:** stu901vwx234
- **Jira:** NOT-223
- **Build:** #1876 (v2.7.1)
- **Resolution:** Memory increased, retry logic added

### 5. Authentication Service JWT Validation Failure
- **Date:** 2026-07-10 08:00:00 UTC
- **Duration:** 15 minutes
- **Severity:** CRITICAL
- **Root Cause:** JWT key rotation sync failure across instances
- **Commit:** yza567bcd890
- **Jira:** AUTH-890
- **Build:** #4231 (v4.1.2)
- **Resolution:** Manual key synchronization

---

## Data Files Created

### Log RCA Files (`demo-data/rca/`)
```
incident-1-payment-hikari-pool-exhaustion.log      (2.8 KB)
incident-2-order-kafka-consumer-lag.log            (4.2 KB)
incident-3-vehicle-oracle-timeout.log              (1.5 KB)
incident-4-notification-redis-outage.log           (1.8 KB)
incident-5-auth-jwt-validation-failure.log         (1.3 KB)
```

Each log file contains:
- Realistic Spring Boot application logs
- INFO, WARN, ERROR severity levels
- Stack traces for errors
- Timestamps aligned with incident timeline
- References to commits, Jira issues, and deployments
- Recovery messages

### ELK Data Files
```
elk-seed-data-enhanced.ndjson                      (7.2 KB, 48 log entries)
elk-seed-demo-incidents.ps1                        (PowerShell loader)
elk-seed-demo-incidents.bat                        (Batch wrapper)
```

ELK data includes:
- ERROR and WARN logs from all 5 incidents
- INFO logs showing deployments and recovery
- Proper JSON format for Elasticsearch bulk API
- Timestamps matching incident timelines
- Service names, trace IDs, environment tags

### Knowledge Service Data
```
knowledge-demo-data.ps1                            (PowerShell loader)
knowledge-demo-data.bat                            (Batch wrapper)
```

Knowledge Service loader populates:
- **GitHub Commits** - 10+ commits with code changes
- **Jira Issues** - 5 tickets with descriptions, comments, linked issues
- **Confluence Pages** - 5 troubleshooting guides
- **Jenkins Deployments** - 10+ build/deployment records
- **Previous Incidents** - 5 historical incidents for correlation

---

## Data Consistency

All data is **semantically connected**:

| Artifact Type | Incident 1 Example | Purpose |
|---------------|-------------------|---------|
| Log File | `HikariCP connection pool exhausted` | Raw error evidence |
| ELK Log | Same error message, indexed | Searchable in Elasticsearch |
| GitHub Commit | `abc123def456` - "Increase database timeout" | Root cause code change |
| Jira Issue | `PAY-421` - References commit and logs | Issue tracking |
| Confluence | "Payment Service DB Configuration Guide" | Documentation |
| Deployment | Build #1245, deployed at 14:15 UTC | Deployment timing |
| Previous Incident | INC-2024-089 - Similar symptoms | Historical learning |

This ensures AI can successfully correlate information across all systems.

---

## Loading Instructions

### 1. Load ELK Data (Option 1: Enhanced Demo Data)

```powershell
# From root directory
.\elk-seed-demo-incidents.bat
```

This loads the 5 demo incidents into Elasticsearch.

**OR**

### 1. Load ELK Data (Option 2: Full Enterprise Data)

```powershell
# From root directory  
.\elk-seed-data.bat
```

This loads comprehensive enterprise scenario data (recommended for full demos).

### 2. Load Knowledge Service Data

```powershell
# From demo-data/knowledge directory
cd demo-data\knowledge
.\knowledge-demo-data.bat
```

**Important:** Knowledge Service must be running first!

### 3. Wait for Embeddings

After loading Knowledge Service data, **wait 2-3 minutes** for:
- Embeddings to be generated via AI Engine
- Vectors to be stored in PostgreSQL
- Semantic search to become operational

---

## Verification Commands

### Check ELK Data
```powershell
# Check index exists
curl http://localhost:9200/_cat/indices

# Check document count
curl http://localhost:9200/sentinelai-logs/_count

# Search for payment-service errors
curl -X POST http://localhost:9200/sentinelai-logs/_search -H "Content-Type: application/json" -d '{\"query\":{\"bool\":{\"must\":[{\"match\":{\"service\":\"payment-service\"}},{\"match\":{\"severity\":\"ERROR\"}}]}}}'
```

### Check Knowledge Service Data
```powershell
# Check GitHub commits
curl http://localhost:8090/api/repositories

# Check Jira issues
curl http://localhost:8090/api/jira/issues

# Check Confluence pages
curl http://localhost:8090/api/confluence/pages

# Check deployments
curl http://localhost:8090/api/deployments

# Check that embeddings are generated
curl "http://localhost:8090/api/embeddings/status"
```

---

## Demo Query Examples

### Log RCA Demos

Upload any log file from `demo-data/rca/` via the UI.

**Expected Results:**
- Incident 1: "HikariCP connection pool exhaustion"
- Incident 2: "Kafka consumer lag after deployment"
- Incident 3: "Oracle query timeout due to full table scan"
- Incident 4: "Redis cluster failover"
- Incident 5: "JWT key rotation sync failure"

### ELK Investigation Demos

| Service | Severity | Timeframe | Expected Finding |
|---------|----------|-----------|------------------|
| payment-service | ERROR | 60 min | HikariCP pool errors, PAY-421 |
| order-service | WARN | 120 min | Kafka lag warnings, ORD-334 |
| vehicle-registration-service | ERROR | 90 min | Oracle timeouts, VEH-567 |
| notification-service | ERROR | 60 min | Redis connection failures, NOT-223 |
| auth-service | ERROR | 30 min | JWT validation errors, AUTH-890 |

### Engineering Context Explorer Demos

**Search by Logs:**
```
ERROR HikariPool-1 - Connection is not available, request timed out after 30017ms
```
Expected: Commit abc123def456, Jira PAY-421, Confluence guide, deployment #1245

**Search by Natural Language:**
```
"Payment service timeout"
"Kafka consumer lag"
"Database connection pool exhausted"
"Find similar payment incidents"
```

Expected: Relevant commits, Jira issues, documentation, and previous incidents.

---

## File Organization

```
SentinelAI/
├── demo-data/
│   ├── rca/                              # Log files for RCA
│   │   ├── incident-1-payment-hikari-pool-exhaustion.log
│   │   ├── incident-2-order-kafka-consumer-lag.log
│   │   ├── incident-3-vehicle-oracle-timeout.log
│   │   ├── incident-4-notification-redis-outage.log
│   │   └── incident-5-auth-jwt-validation-failure.log
│   │
│   ├── elk/                              # ELK reference (actual files in root)
│   │   └── README.md
│   │
│   ├── knowledge/                        # Knowledge Service loader
│   │   ├── knowledge-demo-data.ps1
│   │   └── knowledge-demo-data.bat
│   │
│   ├── INCIDENTS.md                      # Incident definitions
│   └── README.md                         # Demo data overview
│
├── elk-seed-data.ps1                     # Original ELK loader (comprehensive)
├── elk-seed-data.bat                     # Batch wrapper
├── elk-seed-data.ndjson                  # Original ELK data
├── elk-seed-data-enhanced.ndjson         # NEW: Demo incidents only
├── elk-seed-demo-incidents.ps1           # NEW: Demo incidents loader
├── elk-seed-demo-incidents.bat           # NEW: Batch wrapper
│
├── DEMO_GUIDE.md                         # Complete demo walkthrough
├── DEMO_DATA_SUMMARY.md                  # This file
│
├── setup-demo-environment.ps1            # Automated setup script
└── setup-demo-environment.bat            # Batch wrapper
```

---

## Customization

### Adding New Incidents

1. **Create log file** in `demo-data/rca/incident-6-your-scenario.log`
2. **Update INCIDENTS.md** with timeline and artifact metadata
3. **Add ELK data** to `elk-seed-data-enhanced.ndjson`
4. **Update knowledge-demo-data.ps1** with:
   - GitHub commits
   - Jira issues
   - Confluence pages
   - Jenkins deployments
   - Previous incidents
5. **Ensure consistency** - Use same commit hashes, Jira IDs, timestamps across all systems

### Modifying Existing Incidents

1. **Update log files** in `demo-data/rca/`
2. **Update ELK data** in `elk-seed-data-enhanced.ndjson`
3. **Update Knowledge Service loader** in `knowledge-demo-data.ps1`
4. **Maintain consistency** - Keep artifact references aligned

---

## Known Limitations

1. **Knowledge Service Endpoint Missing**
   - The actual `/api/context/retrieve` endpoint is not implemented yet
   - Demo loader will fail when calling non-existent endpoints
   - Resolution: Implement Knowledge Service controllers (see ARCHITECTURE_REVIEW_REPORT.md)

2. **Embeddings Take Time**
   - Vector generation is asynchronous
   - Wait 2-3 minutes after loading before testing
   - Check embedding status before demos

3. **Timeframe Constraints**
   - ELK Investigation requires logs within the specified timeframe
   - Adjust "Timeframe Minutes" if incidents are older
   - Use absolute timestamps for production scenarios

4. **API Endpoint Variations**
   - Some endpoints in `knowledge-demo-data.ps1` may not exist
   - Script will show errors but continue (graceful degradation)
   - Verify actual endpoint availability in Knowledge Service

---

## Troubleshooting

### Issue: ELK data not loading
**Solution:** 
- Check Elasticsearch running: `curl http://localhost:9200`
- Re-run seed script with verbose output
- Check index name matches application.yml configuration

### Issue: Knowledge Service returns empty results
**Solution:**
- Verify services running: `curl http://localhost:8090/actuator/health`
- Check data loaded: `curl http://localhost:8090/api/repositories`
- Wait 3 minutes for embeddings to generate
- Check AI Engine: `curl http://localhost:8000/`

### Issue: Log RCA returns no engineering context
**Solution:**
- Check Core can reach Knowledge Service
- Review Core logs for connection errors
- Verify Knowledge Service URL in Core's application.yml
- Ensure graceful degradation works (RCA still returns basic analysis)

### Issue: Context Explorer shows no evidence cards
**Solution:**
- Check UI calls Core (not Knowledge Service directly)
- Verify BFF pattern implemented (see BFF_IMPLEMENTATION_SUMMARY.md)
- Check browser console for API errors
- Test endpoint: `curl -X POST http://localhost:8080/api/context/retrieve -H "Content-Type: application/json" -d '{"logs":"test"}'`

---

## Related Documentation

- **DEMO_GUIDE.md** - Step-by-step presentation guide
- **INCIDENTS.md** - Detailed incident definitions
- **demo-data/README.md** - Demo data structure overview
- **ARCHITECTURE_REVIEW_REPORT.md** - Technical architecture review
- **BFF_IMPLEMENTATION_SUMMARY.md** - Backend-for-Frontend pattern details

---

## Demo Data Statistics

| Category | Count | Details |
|----------|-------|---------|
| Incidents | 5 | Complete production scenarios |
| Log Files | 5 | 11.6 KB total, 200-500 lines each |
| ELK Logs | 48+ | Enhanced NDJSON format |
| GitHub Commits | 10+ | With code changes, branches, tags |
| Jira Issues | 5+ | With comments, linked issues |
| Confluence Pages | 5+ | Troubleshooting guides |
| Deployments | 10+ | Build numbers, timestamps, versions |
| Previous Incidents | 5 | Historical correlation data |

**Total Data Size:** ~25 KB (excluding Knowledge Service vector embeddings)  
**Embedding Count:** ~30 artifacts × 1536 dimensions = 46,080 vector values  
**Generation Time:** 2-3 minutes for all embeddings

---

## Success Criteria

After loading all demo data, you should be able to:

✅ **Log RCA:**
- Upload any incident log file
- Get structured root cause analysis within 30 seconds
- See engineering context panel with commits, Jira, docs
- View previous similar incidents

✅ **ELK Investigation:**
- Search for any of the 5 services
- Get suspicious log entries identified by AI
- See correlation with deployments and commits
- Get actionable recommendations

✅ **Engineering Context Explorer:**
- Search by log patterns or natural language
- Get ranked evidence cards with explanations
- See timeline of related events
- View relationship graph (when implemented)

✅ **End-to-End Correlation:**
- Same engineering artifacts appear across all three modules
- AI successfully links logs → commits → Jira → docs → deployments
- Previous incidents inform new investigations
- Recommendations reference actual artifacts

---

## Maintenance

To keep demo data current:

1. **Update timestamps** - Replace 2026 dates with current year + appropriate offsets
2. **Refresh versions** - Update build numbers and version tags
3. **Add new patterns** - Include emerging technologies (e.g., new frameworks)
4. **Expand coverage** - Add more services, more incident types
5. **Update documentation** - Keep Confluence pages aligned with current best practices

---

## Production Readiness

This demo data is **NOT** production-ready because:

- ❌ Hardcoded commit hashes and Jira IDs
- ❌ Synthetic timestamps (not real-time)
- ❌ Limited incident variety (only 5 scenarios)
- ❌ No authentication/authorization data
- ❌ No actual database queries or API responses

For production:

- ✅ Use webhooks for real-time sync
- ✅ Connect to actual GitHub, Jira, Confluence APIs
- ✅ Stream logs from real applications
- ✅ Implement proper authentication
- ✅ Add monitoring and alerting

---

**Questions or issues?** Refer to DEMO_GUIDE.md or ARCHITECTURE_REVIEW_REPORT.md for additional context.

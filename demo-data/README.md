# SentinelAI Demo Data

This directory contains realistic enterprise demo data for all SentinelAI V2 modules.

## Directory Structure

```
demo-data/
├── rca/                              # Log files for RCA demo
│   ├── incident-1-payment-hikari-pool-exhaustion.log
│   ├── incident-2-order-kafka-consumer-lag.log
│   ├── incident-3-vehicle-oracle-timeout.log
│   ├── incident-4-notification-redis-outage.log
│   └── incident-5-auth-jwt-validation-failure.log
│
├── elk/                              # ELK seed data and scripts
│   ├── elk-seed-data-enhanced.ndjson
│   ├── elk-seed-demo-incidents.ps1
│   ├── elk-seed-demo-incidents.bat
│   └── README.md
│
├── knowledge/                        # Knowledge Service data loader
│   ├── knowledge-demo-data.bat
│   └── knowledge-demo-data.ps1
│
├── DEMO_GUIDE.md                     # Complete demo walkthrough
├── DEMO_DATA_SUMMARY.md              # Comprehensive data overview
├── INCIDENTS.md                      # Incident definitions and metadata
├── README.md                         # This file
├── setup-demo-environment.ps1        # Automated setup script
└── setup-demo-environment.bat        # Batch wrapper
```

## Quick Start

### 1. Load ELK Data

From the **root directory**:

```powershell
.\elk-seed-data.ps1
```

This seeds Elasticsearch with log data for ELK Investigation demos.

### 2. Load Knowledge Service Data

From the **demo-data/knowledge** directory:

```powershell
cd demo-data\knowledge
.\knowledge-demo-data.bat
```

Or run the PowerShell script directly:

```powershell
.\knowledge-demo-data.ps1
```

This populates the Knowledge Service with:
- GitHub commits
- Jira issues
- Confluence pages
- Jenkins deployments
- Previous incidents

### 3. Use RCA Log Files

Upload any log file from `rca/` through the Log RCA UI:

- Go to http://localhost:3000
- Click "Log RCA" tab
- Upload a log file
- Click "Analyze Log"

## Demo Incidents

All demo data is based on 5 realistic production incidents:

1. **Payment Service HikariCP Pool Exhaustion** (2026-07-20)
   - File: `incident-1-payment-hikari-pool-exhaustion.log`
   - Root cause: Database timeout increased without pool size adjustment
   - Related: Commit abc123def456, Jira PAY-421

2. **Order Service Kafka Consumer Lag** (2026-07-18)
   - File: `incident-2-order-kafka-consumer-lag.log`
   - Root cause: Consumer group rebalancing during deployment
   - Related: Commit 789ghi012jkl, Jira ORD-334

3. **Vehicle Registration Oracle Timeout** (2026-07-15)
   - File: `incident-3-vehicle-oracle-timeout.log`
   - Root cause: Oracle query plan changed to full table scan
   - Related: Commit mno345pqr678, Jira VEH-567

4. **Notification Service Redis Outage** (2026-07-12)
   - File: `incident-4-notification-redis-outage.log`
   - Root cause: Redis cluster failover due to memory pressure
   - Related: Commit stu901vwx234, Jira NOT-223

5. **Authentication Service JWT Validation Failure** (2026-07-10)
   - File: `incident-5-auth-jwt-validation-failure.log`
   - Root cause: JWT key rotation sync failure
   - Related: Commit yza567bcd890, Jira AUTH-890

See **INCIDENTS.md** for detailed timeline and artifact mapping.

## Data Consistency

All data is **semantically connected** across systems:

- Log files reference specific commit hashes
- Jira issues link to commits and releases
- Confluence pages document troubleshooting for each issue
- Jenkins deployments match commit hashes and timestamps
- Previous incidents provide historical context

This enables SentinelAI to successfully correlate information across systems.

## Verification

After loading data, verify:

### ELK Data
```powershell
curl http://localhost:9200/sentinelai-logs/_count
```
Should show thousands of log documents.

### Knowledge Service Data
```powershell
# Check GitHub commits
curl http://localhost:8090/api/repositories

# Check Jira issues
curl http://localhost:8090/api/jira/issues

# Check Confluence pages
curl http://localhost:8090/api/confluence/pages
```

Each should return demo data.

## Embedding Generation

After loading Knowledge Service data, **embeddings are generated automatically**:

- The Knowledge Service calls the AI Engine's `/api/embeddings` endpoint
- Embedding generation takes 1-2 minutes for all demo data
- Vectors are stored in PostgreSQL for semantic search

**Wait 2-3 minutes** after data load before running demos to ensure embeddings are ready.

## Troubleshooting

### No data loaded
- Ensure Knowledge Service is running: http://localhost:8090/actuator/health
- Check PowerShell execution policy: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`

### Embeddings not generating
- Ensure AI Engine is running: http://localhost:8000/
- Check AI Engine logs for errors
- Test embedding endpoint: `curl -X POST http://localhost:8000/api/embeddings -H "Content-Type: application/json" -d '{"text":"test"}'`

### ELK data missing
- Check Elasticsearch running: `curl http://localhost:9200`
- Re-run: `cd elk && .\elk-seed-demo-incidents.ps1`

## Customization

To add your own demo scenarios:

1. **Add log file** to `rca/`
2. **Update INCIDENTS.md** with incident metadata
3. **Update knowledge/knowledge-demo-data.ps1** with corresponding GitHub/Jira/Confluence data
4. **Update elk/elk-seed-data-enhanced.ndjson** with ELK logs
5. **Ensure consistency** - use same commit hashes, Jira IDs, timestamps

## Demo Guide

See **DEMO_GUIDE.md** in this directory for step-by-step presentation instructions.

## Support

For questions or issues, refer to:
- Main README.md
- DEMO_GUIDE.md
- ARCHITECTURE_REVIEW_REPORT.md
- BFF_IMPLEMENTATION_SUMMARY.md

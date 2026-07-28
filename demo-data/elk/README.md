# ELK Demo Data - Load Instructions

## Overview

This folder contains scripts to load demo incident data into Elasticsearch with **current timestamps**. Every time you run the loader, it generates logs with timestamps relative to the current time, so they always appear in "last 24 hours" searches.

## Quick Start

### Windows

```bash
cd demo-data\elk
.\elk-seed-demo-incidents.bat
```

### PowerShell

```powershell
cd demo-data\elk
.\elk-seed-demo-incidents-fixed.ps1
```

## What Gets Loaded

The script automatically:
1. **Generates** fresh log data with current timestamps
2. **Deletes** the existing `sentinelai-logs` index (if it exists)
3. **Creates** a new index with proper mappings
4. **Loads** 39 log entries across 5 demo incidents
5. **Verifies** the data was loaded correctly

## Demo Incident Timeline (Relative to Now)

| Time Ago | Incident | Service | Jira |
|----------|----------|---------|------|
| 30 minutes | HikariCP Pool Exhaustion | payment-service | PAY-421 |
| 2 hours | Kafka Consumer Lag | order-service | ORD-334 |
| 4 hours | Oracle Timeout | vehicle-registration-service | VEH-567 |
| 8 hours | Redis Outage | notification-service | NOT-223 |
| 12 hours | JWT Validation Failure | auth-service | AUTH-890 |

## Testing in UI

### Payment Service Incident (Most Recent)
- **Service:** `payment-service`
- **Severity:** `ERROR`
- **Time Range:** Last 24 hours ✅
- **Expected:** ~5 ERROR logs about HikariCP connection pool exhaustion

### Order Service Incident
- **Service:** `order-service`
- **Severity:** `WARN` or `ERROR`
- **Time Range:** Last 24 hours ✅
- **Expected:** ~4 logs about Kafka consumer lag

### All Incidents
- **Service:** (leave empty)
- **Severity:** `ERROR`
- **Time Range:** Last 24 hours ✅
- **Expected:** Multiple incidents across all services

## Files

| File | Purpose |
|------|---------|
| `elk-seed-demo-incidents.bat` | Windows batch wrapper |
| `elk-seed-demo-incidents-fixed.ps1` | Main PowerShell script |
| `generate-elk-demo-data.ps1` | Generates NDJSON with current timestamps |
| `elk-seed-data-enhanced.ndjson` | Generated file (auto-created) |

## Verify Data Loaded

### Check Document Count
```powershell
Invoke-RestMethod -Uri "http://localhost:9200/sentinelai-logs/_count"
# Should show: count = 39
```

### See Recent Logs
```powershell
$query = '{"size":5,"sort":[{"timestamp":"desc"}]}'
Invoke-RestMethod -Uri "http://localhost:9200/sentinelai-logs/_search" `
  -Method Post -ContentType "application/json" -Body $query `
  | Select-Object -ExpandProperty hits | Select-Object -ExpandProperty hits
```

### Search for Payment Errors
```powershell
$query = '{"query":{"bool":{"filter":[{"term":{"service":"payment-service"}},{"term":{"severity":"ERROR"}}]}}}'
Invoke-RestMethod -Uri "http://localhost:9200/sentinelai-logs/_search" `
  -Method Post -ContentType "application/json" -Body $query
```

## Troubleshooting

### Elasticsearch Not Running
```
X Cannot reach Elasticsearch at http://localhost:9200
```

**Solution:** Start Elasticsearch:
```bash
docker-compose -f elk-docker-compose.yml up -d
```

### 400 Bad Request Error
This was fixed! The issue was NDJSON format required a final newline character.

### Old Timestamps
This is fixed! The script now generates timestamps relative to the current time every time it runs.

## Technical Details

### NDJSON Format
Elasticsearch bulk API requires:
- Each action-document pair on separate lines
- Lines separated by `\n` (Unix line endings)
- File must end with `\n`

Example:
```
{"index":{"_index":"sentinelai-logs"}}
{"timestamp":"2026-07-23T14:30:00.000Z","service":"payment-service",...}
{"index":{"_index":"sentinelai-logs"}}
{"timestamp":"2026-07-23T14:31:00.000Z","service":"order-service",...}

```

### Index Mapping
```json
{
  "mappings": {
    "properties": {
      "timestamp": { "type": "date" },
      "service": { "type": "keyword" },
      "severity": { "type": "keyword" },
      "message": { "type": "text" },
      "traceId": { "type": "keyword" },
      "environment": { "type": "keyword" }
    }
  }
}
```

## Integration with Knowledge Service

The demo logs reference Jira issues (PAY-421, ORD-334) that are also loaded into the Knowledge Service. This enables the **Engineering Context Explorer** to correlate logs with:
- GitHub commits
- Jira issues
- Confluence documentation
- Jenkins deployments

See `demo-data/knowledge/README.md` for loading Knowledge Service data.

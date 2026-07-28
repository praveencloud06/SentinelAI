# Enterprise Demo Incidents

This document defines the 5 production incidents used across all SentinelAI demo data.
All data (logs, commits, Jira, Confluence, deployments) references these incidents consistently.

---

## Incident 1: Payment Service Database Connection Pool Exhaustion

**Service:** payment-service  
**Environment:** production  
**Date:** 2026-07-20 14:30:00 UTC  
**Duration:** 45 minutes  
**Severity:** CRITICAL

### Root Cause
HikariCP connection pool exhausted due to increased transaction timeout from 5s to 30s in v2.1.5 deployment.

### Timeline
- 14:15 - Deployment of payment-service v2.1.5
- 14:30 - First connection pool timeout errors
- 14:45 - Payment processing failures spike to 85%
- 15:00 - Rollback initiated
- 15:15 - Service recovered

### Related Artifacts
- **Commit:** `abc123def456` - "Increase database timeout for long-running transactions"
- **Jira:** PAY-421 - "Payment processing timeout errors"
- **PR:** #892 - "Configure HikariCP pool size for high load"
- **Deployment:** Build #1245, Production, 2026-07-20 14:15
- **Release:** v2.1.5
- **Confluence:** "Payment Service Database Configuration Guide"
- **Previous Incident:** INC-2024-089 - Similar Hikari pool exhaustion (2024-03-15)

---

## Incident 2: Order Service Kafka Consumer Lag

**Service:** order-service  
**Environment:** production  
**Date:** 2026-07-18 09:15:00 UTC  
**Duration:** 2 hours  
**Severity:** HIGH

### Root Cause
Kafka consumer group rebalancing caused by pod restart during deployment. Consumer lag increased to 50K messages.

### Timeline
- 09:00 - Deployment of order-service v3.2.1
- 09:15 - Consumer lag starts increasing
- 09:45 - Order processing delay reaches 15 minutes
- 10:30 - Manual consumer reset
- 11:15 - Lag cleared, service normal

### Related Artifacts
- **Commit:** `789ghi012jkl` - "Update Kafka consumer configuration for reliability"
- **Jira:** ORD-334 - "Kafka consumer lag after deployment"
- **PR:** #776 - "Implement Kafka consumer graceful shutdown"
- **Deployment:** Build #2108, Production, 2026-07-18 09:00
- **Release:** v3.2.1
- **Confluence:** "Order Service Kafka Consumer Troubleshooting"
- **Previous Incident:** INC-2025-156 - Kafka rebalancing storm (2025-11-22)

---

## Incident 3: Vehicle Registration Oracle Timeout

**Service:** vehicle-registration-service  
**Environment:** production  
**Date:** 2026-07-15 16:00:00 UTC  
**Duration:** 1 hour  
**Severity:** CRITICAL

### Root Cause
Oracle database query plan changed after statistics refresh, causing full table scans on VEHICLE_REGISTRATION table (45M rows).

### Timeline
- 15:30 - Oracle automatic statistics job runs
- 16:00 - Query performance degrades from 200ms to 45s
- 16:15 - Registration processing failures
- 16:30 - DBA identifies bad execution plan
- 16:45 - Manual hint added, plan forced
- 17:00 - Service recovered

### Related Artifacts
- **Commit:** `mno345pqr678` - "Add query hints for registration lookup"
- **Jira:** VEH-567 - "Registration lookup timeout in production"
- **PR:** #1024 - "Optimize vehicle registration query performance"
- **Deployment:** Build #3456, Production, 2026-07-15 16:45
- **Release:** v1.9.3
- **Confluence:** "Vehicle Registration Database Performance Tuning"
- **Previous Incident:** INC-2026-012 - Oracle statistics issue (2026-02-08)

---

## Incident 4: Notification Service Redis Outage

**Service:** notification-service  
**Environment:** production  
**Date:** 2026-07-12 11:30:00 UTC  
**Duration:** 30 minutes  
**Severity:** HIGH

### Root Cause
Redis cluster failover triggered by memory pressure. Primary node evicted active sessions, causing authentication failures.

### Timeline
- 11:20 - Redis memory usage reaches 95%
- 11:30 - Automatic failover triggered
- 11:32 - Notification delivery failures
- 11:35 - Session cache cleared
- 11:45 - Memory increased, service restored
- 12:00 - All notifications re-queued

### Related Artifacts
- **Commit:** `stu901vwx234` - "Implement Redis connection retry with exponential backoff"
- **Jira:** NOT-223 - "Notification delivery failures during Redis failover"
- **PR:** #654 - "Add Redis circuit breaker"
- **Deployment:** Build #1876, Production, 2026-07-12 11:45
- **Release:** v2.7.1
- **Confluence:** "Notification Service Redis Operations Guide"
- **Previous Incident:** INC-2025-234 - Redis OOM (2025-08-19)

---

## Incident 5: Authentication Service JWT Validation Failure

**Service:** auth-service  
**Environment:** production  
**Date:** 2026-07-10 08:00:00 UTC  
**Duration:** 15 minutes  
**Severity:** CRITICAL

### Root Cause
JWT signing key rotation script failed to propagate new public key to all auth-service instances. 50% of instances had stale keys.

### Timeline
- 07:55 - Key rotation cron job executes
- 08:00 - JWT validation failures begin
- 08:05 - 50% of authentication requests failing
- 08:08 - Key sync identified as root cause
- 08:10 - Manual key sync triggered
- 08:15 - All instances synchronized

### Related Artifacts
- **Commit:** `yza567bcd890` - "Fix JWT key rotation synchronization"
- **Jira:** AUTH-890 - "JWT validation intermittent failures"
- **PR:** #445 - "Implement distributed key rotation with Consul"
- **Deployment:** Build #4231, Production, 2026-07-10 08:10
- **Release:** v4.1.2
- **Confluence:** "Authentication Service JWT Key Management"
- **Previous Incident:** INC-2025-345 - JWT clock skew (2025-12-03)

---

## Data Consistency Rules

All demo data must follow these rules:

1. **Service Names:** Use exact names above (e.g., `payment-service`)
2. **Timestamps:** All related events within ±2 hours of incident time
3. **Commit IDs:** Use exact commit hashes above
4. **Jira IDs:** Use exact ticket numbers above
5. **Release Versions:** Use exact versions above
6. **Build Numbers:** Use exact build numbers above

This ensures semantic search can successfully correlate all artifacts.

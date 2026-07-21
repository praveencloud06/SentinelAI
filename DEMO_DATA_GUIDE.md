# SentinelAI Demo Data Guide

## Seed ELK Logs

Run:

```bat
elk-seed-data.bat
```

This recreates the `logs-application` Elasticsearch index and loads hundreds of logs across DEV, QA, UAT, and PROD for:

- payment-service
- vehicle-invoicing
- connected-vehicle-ota
- auth-service
- notification-service
- order-service
- inventory-service
- customer-service
- api-gateway
- kafka-consumer-payment
- camunda-workflow
- database-layer

## Seed Knowledge Service RCA Documents

The SQL seed lives in:

```text
SentinelAI-Knowledge-Service/src/main/resources/db/migration/V2__demo_rca_knowledge_documents.sql
```

It creates `rca_knowledge_documents` and inserts structured RCA documents for all demo scenarios across DEV, QA, UAT, and PROD.

Flyway is currently disabled in `application.yml`. To run through Flyway, set:

```yaml
spring:
  flyway:
    enabled: true
```

Or execute the SQL manually against the Knowledge Service database.

## ELK Demo Searches

Use these in Kibana or the ELK Investigation UI:

```text
service:payment-service AND environment:PROD AND message:"connection timed out"
```

Expected theme: payment DB connection timeout/RDS saturation.

```text
service:kafka-consumer-payment AND scenario:"Kafka Consumer Lag"
```

Expected theme: consumer lag caused by downstream DB latency or insufficient concurrency.

```text
message:"CrashLoopBackOff" OR scenario:"Pod CrashLoopBackOff"
```

Expected theme: OTA pod startup failure due to missing signing secret.

```text
message:"HTTP 429" AND service:api-gateway
```

Expected theme: partner/dealer portal rate limit exceeded.

```text
message:"Duplicate invoice" OR message:"VIN WDB123456789"
```

Expected theme: duplicate invoice/idempotency issue in vehicle invoicing.

```text
message:"SSLHandshakeException" OR message:"PKIX path building failed"
```

Expected theme: partner certificate chain missing from Java truststore.

## RCA UI Sample Inputs

Paste one of these into the RCA UI log input.

### Payment DB Timeout

```text
2026-07-17T09:42:15.124Z [PROD] [ERROR] payment-service traceId=trc-paydb-001 SQLTransientConnectionException: connection timed out waiting for paymentdb:5432
org.postgresql.util.PSQLException: Connection timed out
    at com.sentinelai.payment.repository.PaymentRepository.save(PaymentRepository.java:147)
    at com.sentinelai.payment.PaymentProcessor.process(PaymentProcessor.java:88)
HikariPool-1 active=20 idle=0 waiting=42
```

Expected RCA:

- Issue: Payment service cannot acquire DB connections.
- Root Cause: RDS/database endpoint saturation or connectivity issue causing Hikari pool exhaustion.
- Impacted Service: payment-service.
- Recommended Fix: Check RDS health and connection count, reduce checkout concurrency, tune pool/query timeouts, restart affected pods only after DB recovers.

### Kafka Consumer Lag

```text
2026-07-17T09:45:18.883Z [PROD] [ERROR] kafka-consumer-payment Consumer group payment-ledger lag is 183420 on topic payments.events partition 7
records-lag-max=183420 processingTimeMs=4210 dbLatencyMs=3800
```

Expected RCA:

- Issue: Payment ledger projection is delayed.
- Root Cause: Consumer throughput is lower than producer rate, likely due to slow downstream database writes.
- Impacted Service: kafka-consumer-payment / payment ledger.
- Recommended Fix: Increase consumer concurrency, tune slow DB queries, monitor lag by partition, replay after stabilization.

### OTA CrashLoopBackOff

```text
2026-07-17T09:50:41.001Z [PROD] [ERROR] connected-vehicle-ota Pod connected-vehicle-ota enters CrashLoopBackOff after config bootstrap
IllegalStateException: Missing required secret ota-signing-key
Last State: Terminated Exit Code: 1 Restart Count: 12
```

Expected RCA:

- Issue: Connected Vehicle OTA service is repeatedly crashing.
- Root Cause: Required OTA signing secret is missing or not mounted in PROD.
- Impacted Service: connected-vehicle-ota.
- Recommended Fix: Restore the secret, restart rollout, verify readiness and OTA signing endpoint.

### Gateway Rate Limit

```text
2026-07-17T09:55:03.443Z [PROD] [WARN] api-gateway HTTP 429 Too Many Requests: rate limit exceeded for partner dealer-portal
route=/orders partnerId=dealer-portal limit=1200/min current=2840/min
```

Expected RCA:

- Issue: Dealer portal traffic is throttled at the API gateway.
- Root Cause: Partner exceeded configured burst limit during batch upload.
- Impacted Service: api-gateway / order ingestion.
- Recommended Fix: Coordinate retry schedule, use exponential backoff, temporarily raise limits only with approval.

### Duplicate Invoice

```text
2026-07-17T10:01:31.121Z [PROD] [ERROR] vehicle-invoicing Duplicate invoice detected for VIN WDB123456789 and period 2026-07
invoiceId=INV-202607-8821 previousInvoiceId=INV-202607-8762 idempotencyKey=WDB123456789
```

Expected RCA:

- Issue: Duplicate invoice generated for the same VIN and billing period.
- Root Cause: Retry idempotency key does not include billing period.
- Impacted Service: vehicle-invoicing.
- Recommended Fix: Void duplicate invoice, patch idempotency key, add unique constraint by VIN and period.

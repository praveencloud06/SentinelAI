@echo off
:: ============================================================
:: SentinelAI – Seed Elasticsearch with sample log documents
::
:: Timestamps are generated at runtime so documents always fall
:: within the last 30 minutes and are immediately queryable.
::
:: Prerequisites:
::   - Elasticsearch is running on localhost:9200
::   - curl and powershell are available (built into Windows 10+/11)
::
:: Usage:
::   elk-seed-data.bat
:: ============================================================

setlocal EnableDelayedExpansion

set ES_URL=http://localhost:9200
set INDEX=logs-application

echo.
echo [1/4] Checking Elasticsearch health...
curl -sf "%ES_URL%/_cluster/health" >nul 2>&1
if errorlevel 1 (
    echo  ERROR: Cannot reach Elasticsearch at %ES_URL%
    echo  Make sure Elasticsearch is running.
    exit /b 1
)
echo  OK

echo.
echo [2/4] Deleting existing index (if present) for a clean reload...
curl -s -X DELETE "%ES_URL%/%INDEX%" >nul
echo  Done

echo.
echo [3/4] Creating index with field mappings...
curl -s -X PUT "%ES_URL%/%INDEX%" ^
  -H "Content-Type: application/json" ^
  -d "{\"mappings\":{\"properties\":{\"timestamp\":{\"type\":\"date\"},\"service\":{\"type\":\"keyword\"},\"severity\":{\"type\":\"keyword\"},\"message\":{\"type\":\"text\"},\"traceId\":{\"type\":\"keyword\"},\"environment\":{\"type\":\"keyword\"}}}}" ^
  >nul
echo  Done

echo.
echo [4/4] Indexing sample documents with current timestamps...

:: Use PowerShell to compute timestamps relative to now so docs always
:: land within the active query window (last 30-60 minutes).
powershell -NoProfile -Command ^
  "$now = [System.DateTime]::UtcNow; $fmt = 'yyyy-MM-ddTHH:mm:ss.fffZ'; $docs = @(" ^
  "  @{t=$now.AddMinutes(-28);svc='payment-service';sev='ERROR';msg='Kafka timeout while processing invoice INV-8821 - consumer group lag exceeded 5000';tid='abc001'}," ^
  "  @{t=$now.AddMinutes(-26);svc='payment-service';sev='ERROR';msg='Database connection pool exhausted - all 20 connections in use (pool: payment-db-pool)';tid='abc002'}," ^
  "  @{t=$now.AddMinutes(-24);svc='payment-service';sev='ERROR';msg='Failed to publish event PAYMENT_PROCESSED to topic payments.events - broker unavailable';tid='abc003'}," ^
  "  @{t=$now.AddMinutes(-22);svc='payment-service';sev='WARN'; msg='Retry attempt 3/5 for Kafka producer - backing off 2000ms';tid='abc003'}," ^
  "  @{t=$now.AddMinutes(-20);svc='payment-service';sev='ERROR';msg='NullPointerException in PaymentProcessor.processInvoice() at line 147 - invoice object is null';tid='abc004'}," ^
  "  @{t=$now.AddMinutes(-18);svc='payment-service';sev='ERROR';msg='Circuit breaker OPEN for downstream service billing-service after 10 consecutive failures';tid='abc005'}," ^
  "  @{t=$now.AddMinutes(-16);svc='payment-service';sev='WARN'; msg='Response time 4520ms exceeds SLA threshold of 2000ms for endpoint POST /payments/process';tid='abc006'}," ^
  "  @{t=$now.AddMinutes(-14);svc='payment-service';sev='ERROR';msg='Transaction rollback triggered - constraint violation on table payment_transactions (unique idx: txn_ref)';tid='abc007'}," ^
  "  @{t=$now.AddMinutes(-12);svc='payment-service';sev='INFO'; msg='Health check passed - DB OK, Kafka DEGRADED, Cache OK';tid='abc008'}," ^
  "  @{t=$now.AddMinutes(-10);svc='payment-service';sev='ERROR';msg='Kafka consumer poll returned empty for 30s - possible broker partition rebalance in progress';tid='abc009'}," ^
  "  @{t=$now.AddMinutes(-25);svc='order-service';   sev='ERROR';msg='Failed to call payment-service: Connection refused at http://payment-service:8081/payments/process';tid='def001'}," ^
  "  @{t=$now.AddMinutes(-23);svc='order-service';   sev='WARN'; msg='Order ORD-4492 stuck in PENDING_PAYMENT state for 90 seconds - scheduling for retry';tid='def002'}," ^
  "  @{t=$now.AddMinutes(-21);svc='order-service';   sev='ERROR';msg='Max retry attempts reached for order ORD-4492 - marking as FAILED';tid='def002'}," ^
  "  @{t=$now.AddMinutes(-15);svc='auth-service';    sev='WARN'; msg='JWT validation failed - token expired for user user@example.com';tid='ghi001'}," ^
  "  @{t=$now.AddMinutes(-13);svc='auth-service';    sev='INFO'; msg='Token refresh issued for user user@example.com';tid='ghi002'}" ^
  "); foreach ($d in $docs) {" ^
  "  $body = '{\"timestamp\":\"' + $d.t.ToString($fmt) + '\",\"service\":\"' + $d.svc + '\",\"severity\":\"' + $d.sev + '\",\"message\":\"' + $d.msg + '\",\"traceId\":\"' + $d.tid + '\",\"environment\":\"local\"}'; " ^
  "  Invoke-RestMethod -Uri 'http://localhost:9200/%INDEX%/_doc' -Method POST -ContentType 'application/json' -Body $body | Out-Null; " ^
  "  Write-Host ('  indexed: [' + $d.sev + '] ' + $d.svc + ' - ' + $d.msg.Substring(0,[Math]::Min(60,$d.msg.Length))) " ^
  "}"

echo.
echo [verify] Document count in %INDEX%:
curl -s -UseBasicParsing "%ES_URL%/%INDEX%/_count"
echo.
echo [verify] Most recent timestamp in index:
powershell -NoProfile -Command "try { (Invoke-WebRequest -UseBasicParsing 'http://localhost:9200/%INDEX%/_search?size=1&sort=timestamp:desc').Content | ConvertFrom-Json | Select-Object -ExpandProperty hits | Select-Object -ExpandProperty hits | ForEach-Object { Write-Host ('  timestamp: ' + $_._source.timestamp + '  service: ' + $_._source.service) } } catch { Write-Host '  Could not fetch' }"
echo.

echo.
echo ============================================================
echo  Sample data loaded with CURRENT timestamps.
echo  All 15 documents fall within the last 30 minutes.
echo.
echo  Test immediately:
echo    service  = payment-service
echo    severity = ERROR
echo    timeframe = 30 minutes
echo.
echo  Kibana: http://localhost:5601
echo  Index:  %INDEX%
echo ============================================================

endlocal
echo  Index:  %INDEX%
echo ============================================================

endlocal

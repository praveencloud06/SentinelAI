param(
    [string]$EsUrl = "http://localhost:9200",
    [string]$Index = "logs-application"
)

$ErrorActionPreference = "Stop"

function New-TraceId([string]$seed) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($seed)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    $hash = ($sha.ComputeHash($bytes) | ForEach-Object { $_.ToString("x2") }) -join ""
    return "trc-" + $hash.Substring(0, 12)
}

function New-JavaStack([string]$exception, [string]$service, [string]$className) {
@"
$exception
    at com.sentinelai.$($service.Replace("-", ".")).$className.handle($className.java:147)
    at com.sentinelai.platform.retry.RetryExecutor.execute(RetryExecutor.java:42)
    at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1089)
"@
}

function New-Log([datetime]$time, [string]$service, [string]$environment, [string]$severity, [string]$message, [string]$scenario, [string]$component, [string]$seed, [string]$stackTrace = "") {
    [ordered]@{
        timestamp   = $time.ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        service     = $service
        environment = $environment
        severity    = $severity
        component   = $component
        scenario    = $scenario
        traceId     = New-TraceId "$service|$environment|$scenario|$seed"
        host        = "$service-$($environment.ToLower())-$((Get-Random -Minimum 1 -Maximum 8))"
        region      = @("us-east-1", "us-east-2", "eu-west-1", "ap-south-1") | Get-Random
        message     = $message
        stackTrace  = $stackTrace
    }
}

function Add-ScenarioLogs([System.Collections.Generic.List[object]]$logs, [hashtable]$scenario, [string]$environment, [datetime]$now, [int]$offset) {
    $service = $scenario.service
    $severity = if ($environment -eq "PROD") { "ERROR" } else { "WARN" }
    $logs.Add((New-Log $now.AddMinutes(-$offset) $service $environment "INFO" "[$environment] $service correlation opened for $($scenario.name); dependency snapshot captured" $scenario.name $scenario.component "$environment-start"))
    $logs.Add((New-Log $now.AddMinutes(-($offset + 1)) $service $environment $severity "[$environment] $service $($scenario.primary)" $scenario.name $scenario.component "$environment-primary" $scenario.stack))
    $logs.Add((New-Log $now.AddMinutes(-($offset + 2)) $service $environment "WARN" "[$environment] $service mitigation attempt: $($scenario.mitigation)" $scenario.name $scenario.component "$environment-mitigation"))
    $logs.Add((New-Log $now.AddMinutes(-($offset + 3)) $service $environment $severity "[$environment] $service impact observed: $($scenario.impact)" $scenario.name $scenario.component "$environment-impact"))
}

Write-Host ""
Write-Host "[1/5] Checking Elasticsearch at $EsUrl ..."
try {
    Invoke-RestMethod -Uri "$EsUrl/_cluster/health" -Method Get | Out-Null
    Write-Host "  OK"
} catch {
    Write-Host "  ERROR: Cannot reach Elasticsearch at $EsUrl"
    Write-Host "  Start ELK, then run elk-seed-data.bat again."
    exit 1
}

Write-Host ""
Write-Host "[2/5] Recreating index $Index ..."
try { Invoke-RestMethod -Uri "$EsUrl/$Index" -Method Delete | Out-Null } catch {}

$mapping = @{
    mappings = @{
        properties = @{
            timestamp   = @{ type = "date" }
            service     = @{ type = "keyword" }
            environment = @{ type = "keyword" }
            severity    = @{ type = "keyword" }
            component   = @{ type = "keyword" }
            scenario    = @{ type = "keyword" }
            traceId     = @{ type = "keyword" }
            host        = @{ type = "keyword" }
            region      = @{ type = "keyword" }
            message     = @{ type = "text" }
            stackTrace  = @{ type = "text" }
        }
    }
} | ConvertTo-Json -Depth 10
Invoke-RestMethod -Uri "$EsUrl/$Index" -Method Put -ContentType "application/json" -Body $mapping | Out-Null
Write-Host "  Done"

Write-Host ""
Write-Host "[3/5] Generating enterprise demo logs ..."

$now = [datetime]::UtcNow
$environments = @("DEV", "QA", "UAT", "PROD")
$services = @(
    "payment-service",
    "vehicle-invoicing",
    "connected-vehicle-ota",
    "auth-service",
    "notification-service",
    "order-service",
    "inventory-service",
    "customer-service",
    "api-gateway",
    "kafka-consumer-payment",
    "camunda-workflow",
    "database-layer"
)

$scenarios = @(
    @{ name="Database Connection Timeout"; service="payment-service"; component="database"; primary="SQLTransientConnectionException: connection timed out waiting for paymentdb:5432"; mitigation="reduce checkout concurrency and verify RDS endpoint"; impact="payments remain in AUTH_PENDING and checkout p95 breached"; stack=(New-JavaStack "org.postgresql.util.PSQLException: Connection timed out" "payment-service" "PaymentRepository") },
    @{ name="Connection Pool Exhausted"; service="order-service"; component="database"; primary="HikariPool-1 - Connection is not available, request timed out after 30000ms"; mitigation="drain long running queries and reduce request concurrency"; impact="orders stuck in PENDING_PAYMENT and request threads blocked"; stack=(New-JavaStack "java.sql.SQLTransientConnectionException: HikariPool exhausted" "order-service" "OrderRepository") },
    @{ name="Deadlock Detected"; service="vehicle-invoicing"; component="database"; primary="ERROR: deadlock detected while updating invoice_header for VIN WDB123456789"; mitigation="retry invoice transaction with backoff"; impact="invoice finalization rolled back for batch INV-BATCH-447"; stack=(New-JavaStack "org.postgresql.util.PSQLException: ERROR: deadlock detected" "vehicle-invoicing" "InvoiceFinalizer") },
    @{ name="Slow SQL"; service="customer-service"; component="database"; primary="Slow query detected SELECT * FROM customer_profile WHERE lower(email)=? took 8742ms"; mitigation="capture EXPLAIN ANALYZE and pg_stat_statements"; impact="customer profile endpoint returned intermittent 502 through gateway"; stack="" },
    @{ name="Lock Wait Timeout"; service="inventory-service"; component="database"; primary="Lock wait timeout exceeded while reserving stock for SKU SKU-7741"; mitigation="identify and kill blocking transaction after validation"; impact="checkout inventory reservation delayed for high demand SKU"; stack=(New-JavaStack "jakarta.persistence.LockTimeoutException: lock wait timeout" "inventory-service" "StockReservation") },
    @{ name="Database Disk Full"; service="database-layer"; component="storage"; primary="ERROR: could not extend file base/pgsql_tmp: No space left on device"; mitigation="purge expired audit partitions and extend volume"; impact="write transactions failed across order and payment schemas"; stack="" },
    @{ name="Transaction Rollback"; service="payment-service"; component="database"; primary="Transaction rolled back because duplicate key violates unique constraint ux_payment_reference"; mitigation="reconcile idempotency table"; impact="payment callback replay created duplicate insert attempt"; stack=(New-JavaStack "org.springframework.dao.DuplicateKeyException: duplicate payment reference" "payment-service" "PaymentCallbackHandler") },
    @{ name="Kafka Consumer Lag"; service="kafka-consumer-payment"; component="kafka"; primary="Consumer group payment-ledger lag is 183420 on topic payments.events partition 7"; mitigation="increase consumer concurrency from 4 to 8"; impact="ledger projection delayed and reconciliation stale"; stack="" },
    @{ name="Kafka Broker Unavailable"; service="notification-service"; component="kafka"; primary="TimeoutException: Topic notifications.events not present in metadata after 60000 ms"; mitigation="validate bootstrap broker DNS and listener config"; impact="notification events queued locally and retries increasing"; stack=(New-JavaStack "org.apache.kafka.common.errors.TimeoutException: broker unavailable" "notification-service" "NotificationProducer") },
    @{ name="Kafka Topic Not Found"; service="connected-vehicle-ota"; component="kafka"; primary="UNKNOWN_TOPIC_OR_PARTITION: ota.package.status.v2 topic not found"; mitigation="create topic with expected partition count"; impact="vehicle OTA status events not published"; stack="" },
    @{ name="Kafka Serialization Exception"; service="order-service"; component="kafka"; primary="SerializationException: Error deserializing Avro message for schema order-created-v5"; mitigation="route incompatible messages to DLQ"; impact="order-created stream paused for affected partition"; stack=(New-JavaStack "org.apache.kafka.common.errors.SerializationException: Avro schema mismatch" "order-service" "OrderCreatedConsumer") },
    @{ name="Offset Commit Failure"; service="inventory-service"; component="kafka"; primary="CommitFailedException: Offset commit cannot be completed since the group has already rebalanced"; mitigation="reduce poll batch size"; impact="inventory reservation messages reprocessed"; stack="" },
    @{ name="Rebalance Failure"; service="camunda-workflow"; component="kafka"; primary="Member workflow-worker-3 failed to rejoin group camunda-jobs before rebalance timeout"; mitigation="pause rolling deployment"; impact="workflow job acquisition delayed"; stack="" },
    @{ name="Bean Initialization Failure"; service="auth-service"; component="spring"; primary="BeanCreationException: Error creating bean with name 'jwtDecoder'"; mitigation="check issuer URI secret and active profile"; impact="auth-service pods failed readiness"; stack=(New-JavaStack "org.springframework.beans.factory.BeanCreationException: jwtDecoder" "auth-service" "SecurityConfig") },
    @{ name="NullPointerException"; service="payment-service"; component="spring"; primary="NullPointerException: Cannot invoke Money.amount() because payment.getAmount() is null"; mitigation="reject invalid payloads at controller"; impact="payment process endpoint returned HTTP 500"; stack=(New-JavaStack "java.lang.NullPointerException: payment amount is null" "payment-service" "PaymentProcessor") },
    @{ name="OutOfMemoryError"; service="api-gateway"; component="jvm"; primary="java.lang.OutOfMemoryError: Java heap space while buffering upstream response"; mitigation="disable response buffering for export route"; impact="gateway pod restarted and client connections reset"; stack=(New-JavaStack "java.lang.OutOfMemoryError: Java heap space" "api-gateway" "ExportRouteFilter") },
    @{ name="StackOverflowError"; service="customer-service"; component="spring"; primary="StackOverflowError at CustomerMapper.toDto due to recursive account relationship"; mitigation="break recursive DTO mapping"; impact="customer profile API returned HTTP 500"; stack=(New-JavaStack "java.lang.StackOverflowError" "customer-service" "CustomerMapper") },
    @{ name="Missing Configuration"; service="notification-service"; component="config"; primary="IllegalStateException: Missing required property smtp.api-key"; mitigation="restore Kubernetes secret key"; impact="notification-service startup failed"; stack=(New-JavaStack "java.lang.IllegalStateException: Missing required property smtp.api-key" "notification-service" "SmtpClientConfig") },
    @{ name="Circular Dependency"; service="order-service"; component="spring"; primary="BeanCurrentlyInCreationException: Requested bean is currently in creation: orderSagaService"; mitigation="roll back saga bean change"; impact="order-service release failed startup"; stack=(New-JavaStack "org.springframework.beans.factory.BeanCurrentlyInCreationException: orderSagaService" "order-service" "OrderSagaConfig") },
    @{ name="Pod CrashLoopBackOff"; service="connected-vehicle-ota"; component="kubernetes"; primary="Pod connected-vehicle-ota enters CrashLoopBackOff after config bootstrap"; mitigation="restore OTA signing secret"; impact="OTA package metadata API unavailable"; stack="" },
    @{ name="OOMKilled"; service="camunda-workflow"; component="kubernetes"; primary="Last State: Terminated Reason: OOMKilled Exit Code: 137"; mitigation="reduce workflow variable fetch size"; impact="workflow workers restarted during bulk completion"; stack="" },
    @{ name="Readiness Probe Failed"; service="inventory-service"; component="kubernetes"; primary="Readiness probe failed: HTTP probe failed with statuscode: 503"; mitigation="check DB dependency readiness"; impact="inventory pods removed from service endpoints"; stack="" },
    @{ name="Liveness Probe Failed"; service="api-gateway"; component="kubernetes"; primary="Liveness probe failed: Get /actuator/health/liveness context deadline exceeded"; mitigation="scale gateway replicas"; impact="gateway restarted under peak traffic"; stack="" },
    @{ name="Node Pressure"; service="database-layer"; component="kubernetes"; primary="Node condition MemoryPressure=True; evicting pod database-maintenance-job"; mitigation="reschedule maintenance jobs"; impact="database maintenance missed window"; stack="" },
    @{ name="Image Pull Failure"; service="customer-service"; component="kubernetes"; primary="ImagePullBackOff: failed to pull image registry/customer-service:2026.07.16"; mitigation="publish missing container tag"; impact="customer-service release stuck"; stack="" },
    @{ name="HTTP 400"; service="api-gateway"; component="rest"; primary="HTTP 400 Bad Request for POST /orders: missing required field customerId"; mitigation="return schema validation response"; impact="dealer portal upload rejected"; stack="" },
    @{ name="HTTP 401"; service="auth-service"; component="security"; primary="HTTP 401 Unauthorized: JWT expired at 2026-07-17T08:44:00Z"; mitigation="force token refresh"; impact="users redirected to login"; stack="" },
    @{ name="HTTP 403"; service="customer-service"; component="security"; primary="HTTP 403 Forbidden: user lacks CUSTOMER_WRITE authority"; mitigation="restore IAM role mapping"; impact="customer updates denied"; stack="" },
    @{ name="HTTP 404"; service="vehicle-invoicing"; component="business"; primary="HTTP 404 Not Found: VIN WDB123456789 was not found for invoice generation"; mitigation="replay vehicle master event"; impact="invoice generation failed for missing VIN"; stack="" },
    @{ name="HTTP 429"; service="api-gateway"; component="rest"; primary="HTTP 429 Too Many Requests: rate limit exceeded for partner dealer-portal"; mitigation="coordinate partner retry schedule"; impact="batch order upload throttled"; stack="" },
    @{ name="HTTP 500"; service="order-service"; component="rest"; primary="HTTP 500 Internal Server Error: NullPointerException in OrderPricingService"; mitigation="patch null-safe campaign metadata handling"; impact="order submission failed"; stack=(New-JavaStack "java.lang.NullPointerException: campaign discount metadata missing" "order-service" "OrderPricingService") },
    @{ name="HTTP 502"; service="api-gateway"; component="rest"; primary="HTTP 502 Bad Gateway from upstream payment-service"; mitigation="drain unhealthy payment pods"; impact="payment route unavailable through gateway"; stack="" },
    @{ name="HTTP 503"; service="notification-service"; component="rest"; primary="HTTP 503 Service Unavailable: SMTP provider circuit breaker open"; mitigation="route to backup notification provider"; impact="email and SMS delayed"; stack="" },
    @{ name="Gateway Timeout"; service="api-gateway"; component="rest"; primary="HTTP 504 Gateway Timeout while calling /vehicles/activate"; mitigation="align timeout budget with certificate provisioning"; impact="vehicle activation requests retried"; stack="" },
    @{ name="Workflow Timeout"; service="camunda-workflow"; component="camunda"; primary="Camunda incident: workflow payment-capture timed out waiting for task CapturePayment"; mitigation="restart payment capture worker"; impact="process instances stuck before fulfillment"; stack="" },
    @{ name="Job Worker Unavailable"; service="camunda-workflow"; component="camunda"; primary="No job worker available for type invoice-generation for 300s"; mitigation="scale invoice worker deployment"; impact="invoice workflow jobs accumulated"; stack="" },
    @{ name="BPMN Deployment Failure"; service="camunda-workflow"; component="camunda"; primary="BPMN deployment failed: duplicate process id order-fulfillment with incompatible version tag"; mitigation="rename process definition version"; impact="new workflow version unavailable"; stack="" },
    @{ name="Incident Created"; service="camunda-workflow"; component="camunda"; primary="Incident created for process order-fulfillment: retries exhausted on ChargePaymentTask"; mitigation="resolve downstream payment errors"; impact="orders stuck before fulfillment"; stack="" },
    @{ name="Zeebe Gateway Unavailable"; service="camunda-workflow"; component="camunda"; primary="ClientStatusException: UNAVAILABLE: io exception connecting to Zeebe gateway"; mitigation="restore Zeebe gateway endpoints"; impact="workers cannot activate jobs"; stack="" },
    @{ name="Secrets Manager Unavailable"; service="auth-service"; component="aws"; primary="SdkClientException: Unable to execute HTTP request to secretsmanager.us-east-1.amazonaws.com"; mitigation="check VPC endpoint route"; impact="auth-service cannot load secrets"; stack=(New-JavaStack "software.amazon.awssdk.core.exception.SdkClientException: Secrets Manager unavailable" "auth-service" "SecretLoader") },
    @{ name="RDS Timeout"; service="database-layer"; component="aws"; primary="Communications link failure: The last packet sent successfully to the server was 30000 ms ago"; mitigation="reduce connection pressure"; impact="database calls timed out"; stack="" },
    @{ name="S3 Access Denied"; service="connected-vehicle-ota"; component="aws"; primary="AccessDenied: s3:GetObject denied for bucket ota-package-prod key firmware/2026.07.1.bin"; mitigation="fix IAM policy and KMS permission"; impact="OTA package download failed"; stack="" },
    @{ name="Lambda Timeout"; service="notification-service"; component="aws"; primary="Task timed out after 30.00 seconds while invoking notification-enrichment lambda"; mitigation="send timed-out messages to retry queue"; impact="notification enrichment queue backed up"; stack="" },
    @{ name="DNS Lookup Failed"; service="api-gateway"; component="infra"; primary="UnknownHostException: payment-service.svc.cluster.local"; mitigation="validate Kubernetes service and CoreDNS"; impact="gateway cannot route payment traffic"; stack=(New-JavaStack "java.net.UnknownHostException: payment-service.svc.cluster.local" "api-gateway" "PaymentRouteClient") },
    @{ name="SSL Handshake Failed"; service="customer-service"; component="infra"; primary="SSLHandshakeException: PKIX path building failed for crm.partner.local"; mitigation="import partner CA chain"; impact="customer CRM sync stopped"; stack=(New-JavaStack "javax.net.ssl.SSLHandshakeException: PKIX path building failed" "customer-service" "CrmClient") },
    @{ name="Certificate Expired"; service="api-gateway"; component="infra"; primary="certificate has expired for api.sentinelai.enterprise.local"; mitigation="renew ingress TLS certificate"; impact="clients rejected TLS connection"; stack="" },
    @{ name="Network Timeout"; service="order-service"; component="infra"; primary="SocketTimeoutException: Read timed out calling inventory-service /reserve"; mitigation="check service mesh retries"; impact="orders remain pending inventory reservation"; stack=(New-JavaStack "java.net.SocketTimeoutException: Read timed out" "order-service" "InventoryClient") },
    @{ name="High CPU"; service="api-gateway"; component="infra"; primary="CPU usage 96% for 10m; request filter latency degraded"; mitigation="enable token introspection cache"; impact="gateway p95 latency breached"; stack="" },
    @{ name="High Memory"; service="customer-service"; component="infra"; primary="Memory usage 91% heap after customer export job started"; mitigation="stream export results"; impact="GC pauses increased"; stack="" },
    @{ name="Duplicate Invoice"; service="vehicle-invoicing"; component="business"; primary="Duplicate invoice detected for VIN WDB123456789 and period 2026-07"; mitigation="void duplicate invoice and patch idempotency key"; impact="customer invoice reconciliation flagged conflict"; stack="" },
    @{ name="Order Processing Failure"; service="order-service"; component="business"; primary="Order ORD-90421 failed in PENDING_PAYMENT after max retry attempts"; mitigation="reconcile late payment callback"; impact="customer checkout failed"; stack="" },
    @{ name="Vehicle Activation Failed"; service="connected-vehicle-ota"; component="business"; primary="Vehicle activation failed: certificate provisioning returned 504 for VIN 1HGCM82633A004352"; mitigation="retry activation idempotently"; impact="mobile app could not pair vehicle"; stack="" },
    @{ name="OTA Package Download Failed"; service="connected-vehicle-ota"; component="business"; primary="OTA package download failed: checksum mismatch for firmware package FWP-2026-07"; mitigation="republish package and manifest atomically"; impact="vehicles rejected firmware"; stack="" },
    @{ name="VIN Not Found"; service="customer-service"; component="business"; primary="VIN 1HGCM82633A004352 not found in vehicle master during customer lookup"; mitigation="replay missing vehicle master event"; impact="customer support could not retrieve vehicle"; stack="" }
)

$logs = [System.Collections.Generic.List[object]]::new()
$baselineTemplates = @(
    "processed request successfully in {0}ms",
    "health check passed: db=OK kafka=OK cache=OK",
    "configuration refresh completed for tenant default",
    "published business metric count={0}",
    "completed reconciliation batch size={0}",
    "cache hit ratio {0}% for rolling window",
    "dependency latency p95={0}ms within SLA",
    "accepted request route=/enterprise/demo"
)

$n = 0
foreach ($environment in $environments) {
    foreach ($service in $services) {
        foreach ($severity in @("INFO", "INFO", "WARN")) {
            $value = Get-Random -Minimum 42 -Maximum 950
            $message = "[$environment] $service " + ($baselineTemplates[$n % $baselineTemplates.Count] -f $value)
            if ($severity -eq "WARN") {
                $message = "[$environment] $service latency approaching threshold; p95=$value ms threshold=1000 ms"
            }
            $logs.Add((New-Log $now.AddMinutes(-(Get-Random -Minimum 5 -Maximum 350)) $service $environment $severity $message "Baseline" "runtime" "baseline-$n"))
            $n++
        }
    }
}

$offset = 8
foreach ($environment in $environments) {
    foreach ($scenario in $scenarios) {
        Add-ScenarioLogs $logs $scenario $environment $now $offset
        $offset += 4
    }
}

Write-Host "  Generated $($logs.Count) documents"

Write-Host ""
Write-Host "[4/5] Bulk indexing documents ..."
$bulkLines = [System.Collections.Generic.List[string]]::new()
foreach ($doc in $logs) {
    $bulkLines.Add((@{ index = @{ _index = $Index } } | ConvertTo-Json -Compress))
    $bulkLines.Add(($doc | ConvertTo-Json -Depth 8 -Compress))
}
$bulkBody = ($bulkLines -join "`n") + "`n"
$bulkResponse = Invoke-RestMethod -Uri "$EsUrl/_bulk?refresh=true" -Method Post -ContentType "application/x-ndjson" -Body $bulkBody
if ($bulkResponse.errors) {
    Write-Host "  ERROR: Bulk index completed with item errors"
    exit 1
}
Write-Host "  Indexed $($logs.Count) documents"

Write-Host ""
Write-Host "[5/5] Verification ..."
$count = Invoke-RestMethod -Uri "$EsUrl/$Index/_count" -Method Get
Write-Host "  Document count: $($count.count)"
$sampleQuery = @{
    size = 5
    sort = @(@{ timestamp = @{ order = "desc" } })
    query = @{ bool = @{ filter = @(@{ terms = @{ severity = @("ERROR", "WARN") } }) } }
} | ConvertTo-Json -Depth 10
$samples = Invoke-RestMethod -Uri "$EsUrl/$Index/_search" -Method Post -ContentType "application/json" -Body $sampleQuery
foreach ($hit in $samples.hits.hits) {
    $src = $hit._source
    Write-Host ("  {0} [{1}] {2}/{3}: {4}" -f $src.timestamp, $src.severity, $src.environment, $src.service, $src.scenario)
}

Write-Host ""
Write-Host "============================================================"
Write-Host " SentinelAI ELK demo dataset loaded."
Write-Host " Index:  $Index"
Write-Host " Kibana: http://localhost:5601"
Write-Host " Try: CrashLoopBackOff, HTTP 429, JWT expired,"
Write-Host "      Kafka Consumer Lag, Duplicate Invoice, SSLHandshakeException"
Write-Host "============================================================"

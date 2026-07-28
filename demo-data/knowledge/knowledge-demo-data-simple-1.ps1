# ============================================================================
# SentinelAI Knowledge Service Demo Data Loader (Simplified)
# ============================================================================

$KNOWLEDGE_SERVICE_URL = "http://localhost:8090"
$ErrorActionPreference = "Continue"

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "SentinelAI Knowledge Service Demo Data Loader" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================================
# Helper Functions
# ============================================================================

function Invoke-KnowledgeAPI {
    param(
        [string]$Endpoint,
        [string]$Method = "POST",
        [object]$Body
    )
    
    $url = "$KNOWLEDGE_SERVICE_URL$Endpoint"
    Write-Host "  -> $Method $Endpoint" -ForegroundColor Gray
    
    try {
        if ($Method -eq "POST") {
            $response = Invoke-RestMethod -Uri $url -Method $Method `
                -ContentType "application/json" `
                -Body ($Body | ConvertTo-Json -Depth 10) `
                -TimeoutSec 60
        } else {
            $response = Invoke-RestMethod -Uri $url -Method $Method -TimeoutSec 60
        }
        Write-Host "    OK Success" -ForegroundColor Green
        return $response
    }
    catch {
        Write-Host "    X Failed: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

function Test-KnowledgeServiceHealth {
    Write-Host "Checking Knowledge Service health..." -ForegroundColor Yellow
    try {
        $health = Invoke-RestMethod -Uri "$KNOWLEDGE_SERVICE_URL/actuator/health" -TimeoutSec 5
        if ($health.status -eq "UP") {
            Write-Host "OK Knowledge Service is running" -ForegroundColor Green
            return $true
        }
    }
    catch {
        Write-Host "X Knowledge Service is not accessible at $KNOWLEDGE_SERVICE_URL" -ForegroundColor Red
        Write-Host "  Please start the Knowledge Service first." -ForegroundColor Red
        return $false
    }
    return $false
}

# ============================================================================
# Load Data
# ============================================================================

Write-Host ""
Write-Host "Starting demo data load..." -ForegroundColor Yellow
Write-Host ""

# Check Knowledge Service health
if (-not (Test-KnowledgeServiceHealth)) {
    Write-Host ""
    Write-Host "Cannot proceed without Knowledge Service running." -ForegroundColor Red
    Write-Host "Please start the Knowledge Service and try again." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Knowledge Service is ready. Beginning data load..." -ForegroundColor Green
Write-Host ""

# ============================================================================
# Incident 1: Payment Service
# ============================================================================

Write-Host ""
Write-Host "Loading Incident 1: Payment Service HikariCP Pool Exhaustion" -ForegroundColor Cyan
Write-Host "=============================================================" -ForegroundColor Cyan

Write-Host "Creating GitHub commits..." -ForegroundColor Yellow

$commit1 = @{
    repository = "sentinelai/payment-service"
    commitHash = "abc123def456"
    author = "john.developer@company.com"
    timestamp = "2026-07-28T10:15:00Z"
    message = "Increase HikariCP maximumPoolSize from 20 to 40.
Reduce connectionTimeout from 30000ms to 5000ms.
Added leakDetectionThreshold.
Fixed database connections not being released after payment transaction rollback.
Optimized PaymentRepository transaction handling."
    filesChanged = @("src/main/resources/application.yml", "src/main/java/com/sentinel/payment/config/DatabaseConfig.java")
    additions = 12
    deletions = 3
    branch = "feature/increase-db-timeout"
    tags = @("v2.1.5")
}
Invoke-KnowledgeAPI -Endpoint "/api/webhooks/GITHUB" -Body $commit1

Write-Host "Creating Jira issues..." -ForegroundColor Yellow

$jira1 = @{
    issueKey = "PAY-421"
    type = "Bug"
    summary = "Production Incident: HikariCP connection pool exhausted after v2.1.5 deployment"
    description = "Following production deployment v2.1.5, payment-service started returning HTTP 500.

Symptoms

• HikariPool-1 exhausted
• SQLTransientConnectionException
• Connection timeout after 30000ms
• Average API latency increased from 450ms to 24 seconds
• Payment transactions failing
• CPU normal
• Database healthy

Root Cause

PaymentRepository introduced a transaction that did not close JDBC connections during rollback.

Impact

Approximately 35% payment failures.

Resolution

Rollback deployment.
Increase pool size.
Fix connection handling."
    priority = "Critical"
    status = "Resolved"
    assignee = "john.developer@company.com"
    reporter = "ops.team@company.com"
    created = "2026-07-28T11:15:00Z"
    updated = "2026-07-28T12:30:00Z"
    resolved = "2026-07-28T13:15:00Z""
    fixVersions = @("v2.1.4-hotfix")
    labels = @("hikaricp", "database", "timeout", "production-incident")
    components = @("payment-service", "database-layer")
}
Invoke-KnowledgeAPI -Endpoint "/api/webhooks/JIRA" -Body $jira1

Write-Host "Creating Confluence pages..." -ForegroundColor Yellow

$confluenceContent = "Payment Service Database Configuration`n`nHikariCP Connection Pool Settings`n- maxPoolSize: 20`n- minIdle: 10`n- connectionTimeout: 5000ms`n`nTroubleshooting: Check active connections vs pool size, identify long-running queries, review recent timeout configuration changes."

$confluence1 = @{
    pageId = "CONF-123456"
    space = "ENG"
    title = "Payment Service Database Configuration Guide"
    content = $confluenceContent
    author = "platform.team@company.com"
    created = "2026-05-15T10:00:00Z"
    updated = "026-07-28T09:30:00Z"
    labels = @("database", "hikaricp", "configuration", "payment-service")
}
Invoke-KnowledgeAPI -Endpoint "/api/webhooks/CONFLUENCE" -Body $confluence1

Write-Host "Creating Jenkins deployments..." -ForegroundColor Yellow

$deployment1 = @{
    buildNumber = 1245
    jobName = "payment-service-deploy-prod"
    service = "payment-service"
    environment = "production"
    version = "v2.1.5"
    commitHash = "abc123def456"
    status = "SUCCESS"
    timestamp = "2026-07-28T11:00:00Z"
    duration = 245
    triggeredBy = "john.developer@company.com"
}
Invoke-KnowledgeAPI -Endpoint "/api/webhooks/JENKINS" -Body $deployment1

Write-Host "OK Incident 1 data loaded" -ForegroundColor Green

# ============================================================================
# Incident 2: Order Service
# ============================================================================

Write-Host ""
Write-Host "Loading Incident 2: Order Service Kafka Consumer Lag" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan

Write-Host "Creating GitHub commits..." -ForegroundColor Yellow

$commit2 = @{
    repository = "sentinelai/order-service"
    commitHash = "789ghi012jkl"
    author = "kafka.expert@company.com"
    timestamp = "2026-07-26T08:30:00Z"

    message = "Fix Kafka consumer lag by optimizing batch processing, rebalance handling, and offset commits"

    description = @"
Production Fix

Issue
- Kafka consumer lag increased to 50,000+ messages after deployment.
- Consumer group rebalancing caused frequent partition revocations.
- Duplicate message processing observed.
- Auto commit resulted in delayed acknowledgements.

Changes
- Increased max.poll.records from 100 to 500.
- Increased consumer concurrency from 3 to 8.
- Switched to manual offset commits.
- Optimized batch processing.
- Added retry with exponential backoff.
- Added consumer lag metrics.
- Improved rebalance listener handling.
- Reduced database writes by batching order updates.

Expected Result
- Consumer lag reduced below 200 messages.
- Processing throughput improved by approximately 60%.
- Stable consumer group after deployment.
- No duplicate order processing.
"@

    filesChanged = @(
        "src/main/resources/application.yml",
        "src/main/java/com/sentinel/order/consumer/OrderEventConsumer.java",
        "src/main/java/com/sentinel/order/config/KafkaConsumerConfig.java",
        "src/main/java/com/sentinel/order/service/OrderProcessingService.java",
        "src/main/java/com/sentinel/order/service/RetryHandler.java",
        "src/main/java/com/sentinel/order/monitoring/KafkaMetrics.java",
        "src/main/java/com/sentinel/order/config/ThreadPoolConfiguration.java"
    )

    additions = 156
    deletions = 38

    branch = "feature/kafka-consumer-improvements"

    tags = @(
        "v3.2.1",
        "kafka",
        "consumer-lag",
        "performance",
        "offset-management",
        "rebalance",
        "production-fix"
    )
}
Invoke-KnowledgeAPI -Endpoint "/api/webhooks/GITHUB" -Body $commit2

Write-Host "Creating Jira issues..." -ForegroundColor Yellow

$jira2 = @{
    issueKey = "ORD-334"
    type = "Bug"

    summary = "Production Incident: Kafka consumer lag and delayed order processing after v3.2.1 deployment"

    description = "Incident Summary:`nFollowing the production deployment of order-service v3.2.1, Kafka consumer lag increased rapidly, resulting in delayed order processing and event backlogs.`n`nBusiness Impact:`n- Approximately 52,000 messages accumulated in Kafka.`n- Order confirmation delayed by up to 25 minutes.`n- Customer notifications delayed.`n- Downstream inventory synchronization impacted.`n`nRoot Cause:`nRecent deployment introduced inefficient batch processing and low consumer concurrency, causing delayed offset commits and repeated Kafka consumer group rebalancing.`n`nResolution:`n- Increased consumer concurrency.`n- Optimized batch processing.`n- Switched to manual offset commits.`n- Tuned max.poll.records.`n- Restarted consumer group.`n`nRelated Deployment: order-service v3.2.1`nRelated Commit: 789ghi012jkl"

    priority = "High"
    status = "Resolved"

    assignee = "kafka.expert@company.com"
    reporter = "ops.team@company.com"

    created = "2026-07-26T09:30:00Z"
    updated = "2026-07-26T11:20:00Z"
    resolved = "2026-07-26T11:20:00Z"

    fixVersions = @(
        "v3.2.2"
    )

    labels = @(
        "kafka",
        "consumer-lag",
        "consumer-group",
        "offset-commit",
        "rebalance",
        "performance",
        "order-service",
        "production-incident"
    )

    components = @(
        "order-service",
        "kafka-consumer",
        "event-processing",
        "messaging",
        "consumer-group"
    )
}
Invoke-KnowledgeAPI -Endpoint "/api/webhooks/JIRA" -Body $jira2

Write-Host "OK Incident 2 data loaded" -ForegroundColor Green

# ============================================================================
# Summary
# ============================================================================

Write-Host ""
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "Demo Data Load Complete!" -ForegroundColor Green
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "  - 2 production incidents loaded" -ForegroundColor White
Write-Host "  - GitHub commits, Jira issues synced" -ForegroundColor White
Write-Host "  - Jenkins deployments recorded" -ForegroundColor White
Write-Host ""
Write-Host "You can now test:" -ForegroundColor Yellow
Write-Host "  1. Log RCA with incident log files" -ForegroundColor White
Write-Host "  2. ELK Investigation" -ForegroundColor White
Write-Host "  3. Engineering Context Explorer" -ForegroundColor White
Write-Host ""

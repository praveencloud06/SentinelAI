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
    message = "Increase database timeout for long-running transactions"
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
    summary = "Payment processing timeout errors"
    description = "Multiple payment transactions failing with HikariCP connection pool exhaustion after v2.1.5 deployment."
    priority = "Critical"
    status = "Resolved"
    assignee = "john.developer@company.com"
    reporter = "ops.team@company.com"
    created = "2026-07-20T14:35:00Z"
    updated = "2026-07-28T12:30:00Z"
    resolved = "2026-07-28T13:15:00Z"
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
    updated = "2026-07-28T09:30:00Z"
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
    message = "Update Kafka consumer configuration for reliability"
    filesChanged = @("src/main/resources/application.yml", "src/main/java/com/sentinel/order/consumer/OrderEventConsumer.java")
    additions = 18
    deletions = 7
    branch = "feature/kafka-consumer-improvements"
    tags = @("v3.2.1")
}
Invoke-KnowledgeAPI -Endpoint "/api/webhooks/GITHUB" -Body $commit2

Write-Host "Creating Jira issues..." -ForegroundColor Yellow

$jira2 = @{
    issueKey = "ORD-334"
    type = "Bug"
    summary = "Kafka consumer lag after deployment"
    description = "Order service Kafka consumer lag increased to 50K messages after v3.2.1 deployment."
    priority = "High"
    status = "Resolved"
    assignee = "kafka.expert@company.com"
    reporter = "ops.team@company.com"
    created = "2026-07-26T09:30:00Z"
    updated = "2026-07-26T11:20:00Z"
    resolved = "2026-07-26T11:20:00Z"
    fixVersions = @("v3.2.2")
    labels = @("kafka", "consumer-lag", "rebalancing", "production-incident")
    components = @("order-service", "kafka-consumer")
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

# ============================================================================
# SentinelAI Knowledge Service Demo Data Loader
# ============================================================================
# This script populates the Knowledge Service with realistic enterprise data
# covering 5 production incidents. All data is interconnected and semantically
# related to enable successful AI correlation.
#
# Usage: .\knowledge-demo-data.ps1
# Requires: Knowledge Service running on http://localhost:8090
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
    Write-Host "  → $Method $Endpoint" -ForegroundColor Gray
    
    try {
        if ($Method -eq "POST") {
            $response = Invoke-RestMethod -Uri $url -Method $Method `
                -ContentType "application/json" `
                -Body ($Body | ConvertTo-Json -Depth 10) `
                -TimeoutSec 60
        } else {
            $response = Invoke-RestMethod -Uri $url -Method $Method -TimeoutSec 60
        }
        Write-Host "    ✓ Success" -ForegroundColor Green
        return $response
    }
    catch {
        Write-Host "    ✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

function Test-KnowledgeServiceHealth {
    Write-Host "Checking Knowledge Service health..." -ForegroundColor Yellow
    try {
        $health = Invoke-RestMethod -Uri "$KNOWLEDGE_SERVICE_URL/actuator/health" -TimeoutSec 5
        if ($health.status -eq "UP") {
            Write-Host "✓ Knowledge Service is running" -ForegroundColor Green
            return $true
        }
    }
    catch {
        Write-Host "✗ Knowledge Service is not accessible at $KNOWLEDGE_SERVICE_URL" -ForegroundColor Red
        Write-Host "  Please start the Knowledge Service first." -ForegroundColor Red
        return $false
    }
    return $false
}

# ============================================================================
# Incident 1: Payment Service HikariCP Pool Exhaustion
# ============================================================================

function Load-Incident1-PaymentService {
    Write-Host ""
    Write-Host "Loading Incident 1: Payment Service Database Connection Pool Exhaustion" -ForegroundColor Cyan
    Write-Host "=========================================================================" -ForegroundColor Cyan
    
    # GitHub Commits
    Write-Host "Creating GitHub commits..." -ForegroundColor Yellow
    
    $commit1 = @{
        repository = "sentinelai/payment-service"
        commitHash = "abc123def456"
        author = "john.developer@company.com"
        timestamp = "2026-07-20T13:45:00Z"
        message = "Increase database timeout for long-running transactions"
        filesChanged = @(
            "src/main/resources/application.yml"
            "src/main/java/com/sentinel/payment/config/DatabaseConfig.java"
        )
        additions = 12
        deletions = 3
        branch = "feature/increase-db-timeout"
        tags = @("v2.1.5")
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/github" -Body $commit1
    
    $commit2 = @{
        repository = "sentinelai/payment-service"
        commitHash = "def456ghi789"
        author = "jane.engineer@company.com"
        timestamp = "2026-07-20T16:30:00Z"
        message = "Rollback: Revert database timeout changes"
        filesChanged = @(
            "src/main/resources/application.yml"
        )
        additions = 3
        deletions = 12
        branch = "hotfix/revert-timeout"
        tags = @("v2.1.4-hotfix")
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/github" -Body $commit2
    
    # Jira Issues
    Write-Host "Creating Jira issues..." -ForegroundColor Yellow
    
    $jira1 = @{
        issueKey = "PAY-421"
        type = "Bug"
        summary = "Payment processing timeout errors"
        description = "Multiple payment transactions failing with HikariCP connection pool exhaustion after v2.1.5 deployment. Connection pool exhausted, active connections: 20/20. This appears related to the database timeout configuration change in commit abc123def456."
        priority = "Critical"
        status = "Resolved"
        assignee = "john.developer@company.com"
        reporter = "ops.team@company.com"
        created = "2026-07-20T14:35:00Z"
        updated = "2026-07-20T15:20:00Z"
        resolved = "2026-07-20T15:20:00Z"
        fixVersions = @("v2.1.4-hotfix")
        labels = @("hikaricp", "database", "timeout", "production-incident")
        components = @("payment-service", "database-layer")
        linkedIssues = @("PAY-422")
        comments = @(
            @{
                author = "dba.team@company.com"
                body = "Database connection pool exhausted. Recommend reverting timeout changes."
                created = "2026-07-20T14:50:00Z"
            }
            @{
                author = "john.developer@company.com"
                body = "Rolled back to v2.1.4. Service recovered. Will investigate proper pool sizing."
                created = "2026-07-20T15:15:00Z"
            }
        )
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/jira" -Body $jira1
    
    # Confluence Documentation
    Write-Host "Creating Confluence pages..." -ForegroundColor Yellow
    
    $confluence1 = @{
        pageId = "CONF-123456"
        space = "ENG"
        title = "Payment Service Database Configuration Guide"
        content = @"
# Payment Service Database Configuration

## HikariCP Connection Pool Settings

### Production Configuration
- maxPoolSize: 20
- minIdle: 10
- connectionTimeout: 5000ms
- maxLifetime: 1800000ms

### Important Notes
- Do NOT increase connectionTimeout above 5 seconds without increasing pool size
- Connection pool exhaustion occurs when long-running transactions hold connections
- Monitor active connection count via /actuator/metrics/hikaricp.connections.active

## Troubleshooting Connection Pool Issues

### Symptoms
- SQLTransientConnectionException: Connection is not available
- High connection wait times
- Thread starvation warnings

### Resolution Steps
1. Check active connections vs pool size
2. Identify long-running queries in database
3. Review recent timeout configuration changes
4. Consider increasing pool size OR reducing timeout

### Related Issues
- PAY-421: Connection pool exhaustion after timeout increase
- INC-2024-089: Similar Hikari pool exhaustion (2024-03-15)

## References
- Commit abc123def456: Timeout configuration change
- Runbook: Database Connection Pool Troubleshooting
"@
        author = "platform.team@company.com"
        created = "2026-05-15T10:00:00Z"
        updated = "2026-07-20T16:00:00Z"
        labels = @("database", "hikaricp", "configuration", "payment-service")
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/confluence" -Body $confluence1
    
    # Jenkins Deployments
    Write-Host "Creating Jenkins deployments..." -ForegroundColor Yellow
    
    $deployment1 = @{
        buildNumber = 1245
        jobName = "payment-service-deploy-prod"
        service = "payment-service"
        environment = "production"
        version = "v2.1.5"
        commitHash = "abc123def456"
        status = "SUCCESS"
        timestamp = "2026-07-20T14:15:00Z"
        duration = 245
        triggeredBy = "john.developer@company.com"
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/jenkins" -Body $deployment1
    
    $deployment2 = @{
        buildNumber = 1246
        jobName = "payment-service-deploy-prod"
        service = "payment-service"
        environment = "production"
        version = "v2.1.4-hotfix"
        commitHash = "def456ghi789"
        status = "SUCCESS"
        timestamp = "2026-07-20T15:00:00Z"
        duration = 198
        triggeredBy = "ops.team@company.com"
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/jenkins" -Body $deployment2
    
    # Previous Incident
    Write-Host "Creating previous incident..." -ForegroundColor Yellow
    
    $incident1 = @{
        incidentId = "INC-2024-089"
        title = "Payment Service HikariCP Connection Pool Exhaustion"
        description = "Production outage caused by HikariCP connection pool exhaustion. All 20 connections in pool were active, preventing new payment transactions. Root cause was increased database query timeout without corresponding pool size increase."
        service = "payment-service"
        severity = "Critical"
        occurredAt = "2024-03-15T18:30:00Z"
        resolvedAt = "2024-03-15T19:45:00Z"
        rootCause = "Database timeout increased from 5s to 30s without pool size adjustment"
        resolution = "Reverted timeout to 5s, service recovered immediately"
        preventionMeasures = "Implement connection pool monitoring alerts, review timeout changes in code review"
        relatedJiraIssues = @("PAY-189")
        relatedCommits = @("oldcommit123")
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/incidents" -Body $incident1
    
    Write-Host "✓ Incident 1 data loaded" -ForegroundColor Green
}

# ============================================================================
# Incident 2: Order Service Kafka Consumer Lag
# ============================================================================

function Load-Incident2-OrderService {
    Write-Host ""
    Write-Host "Loading Incident 2: Order Service Kafka Consumer Lag" -ForegroundColor Cyan
    Write-Host "=====================================================" -ForegroundColor Cyan
    
    # GitHub Commits
    Write-Host "Creating GitHub commits..." -ForegroundColor Yellow
    
    $commit = @{
        repository = "sentinelai/order-service"
        commitHash = "789ghi012jkl"
        author = "kafka.expert@company.com"
        timestamp = "2026-07-18T08:30:00Z"
        message = "Update Kafka consumer configuration for reliability"
        filesChanged = @(
            "src/main/resources/application.yml"
            "src/main/java/com/sentinel/order/consumer/OrderEventConsumer.java"
        )
        additions = 18
        deletions = 7
        branch = "feature/kafka-consumer-improvements"
        tags = @("v3.2.1")
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/github" -Body $commit
    
    # Jira Issues
    Write-Host "Creating Jira issues..." -ForegroundColor Yellow
    
    $jira = @{
        issueKey = "ORD-334"
        type = "Bug"
        summary = "Kafka consumer lag after deployment"
        description = "Order service Kafka consumer lag increased to 50K messages after v3.2.1 deployment. Consumer group rebalancing caused by pod restart. Orders delayed by 15 minutes. Related to commit 789ghi012jkl consumer configuration changes."
        priority = "High"
        status = "Resolved"
        assignee = "kafka.expert@company.com"
        reporter = "ops.team@company.com"
        created = "2026-07-18T09:30:00Z"
        updated = "2026-07-18T11:20:00Z"
        resolved = "2026-07-18T11:20:00Z"
        fixVersions = @("v3.2.2")
        labels = @("kafka", "consumer-lag", "rebalancing", "production-incident")
        components = @("order-service", "kafka-consumer")
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/jira" -Body $jira
    
    # Confluence Documentation
    Write-Host "Creating Confluence pages..." -ForegroundColor Yellow
    
    $confluence = @{
        pageId = "CONF-234567"
        space = "ENG"
        title = "Order Service Kafka Consumer Troubleshooting"
        content = @"
# Order Service Kafka Consumer Troubleshooting

## Consumer Group: order-service-consumer-group

### Normal Operation
- Partitions: 3 (order-events-0, order-events-1, order-events-2)
- Expected lag: less than 500 messages per partition
- Processing rate: 1000 msg/sec

### Common Issues

#### Consumer Lag Spike After Deployment
Symptoms:
- Lag increases to 10K+ messages
- Order processing delays
- Consumer group rebalancing

Causes:
- Pod restarts trigger rebalancing
- Consumer doesn't commit offsets before shutdown
- Insufficient consumer threads

Resolution:
1. Implement graceful shutdown with offset commit
2. Increase max.poll.interval.ms if processing is slow
3. Add consumer group monitoring alerts

### Related Issues
- ORD-334: Consumer lag after v3.2.1 deployment
- INC-2025-156: Kafka rebalancing storm (2025-11-22)

### Configuration
spring:
  kafka:
    consumer:
      enable-auto-commit: false
      max-poll-records: 500
      max-poll-interval-ms: 300000

## References
- Commit 789ghi012jkl: Consumer configuration changes
- Runbook: Kafka Consumer Lag Response
"@
        author = "platform.team@company.com"
        created = "2026-04-20T10:00:00Z"
        updated = "2026-07-18T12:00:00Z"
        labels = @("kafka", "consumer", "troubleshooting", "order-service")
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/confluence" -Body $confluence
    
    # Jenkins Deployment
    Write-Host "Creating Jenkins deployment..." -ForegroundColor Yellow
    
    $deployment = @{
        buildNumber = 2108
        jobName = "order-service-deploy-prod"
        service = "order-service"
        environment = "production"
        version = "v3.2.1"
        commitHash = "789ghi012jkl"
        status = "SUCCESS"
        timestamp = "2026-07-18T09:00:00Z"
        duration = 312
        triggeredBy = "kafka.expert@company.com"
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/jenkins" -Body $deployment
    
    # Previous Incident
    Write-Host "Creating previous incident..." -ForegroundColor Yellow
    
    $incident = @{
        incidentId = "INC-2025-156"
        title = "Order Service Kafka Consumer Rebalancing Storm"
        description = "Kafka consumer group experienced continuous rebalancing causing severe lag. Multiple pod restarts triggered cascading rebalances. Consumer lag exceeded 100K messages."
        service = "order-service"
        severity = "High"
        occurredAt = "2025-11-22T14:00:00Z"
        resolvedAt = "2025-11-22T16:30:00Z"
        rootCause = "Insufficient max.poll.interval.ms for slow message processing"
        resolution = "Increased max.poll.interval.ms from 300s to 600s, reduced max.poll.records"
        preventionMeasures = "Add consumer lag monitoring, implement graceful shutdown"
        relatedJiraIssues = @("ORD-245")
        relatedCommits = @("oldkafkacommit456")
    }
    Invoke-KnowledgeAPI -Endpoint "/api/sync/incidents" -Body $incident
    
    Write-Host "✓ Incident 2 data loaded" -ForegroundColor Green
}

# ============================================================================
# Main Execution
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

# Load all incidents
Load-Incident1-PaymentService
Load-Incident2-OrderService

Write-Host ""
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "Demo Data Load Complete!" -ForegroundColor Green
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "  - 2 production incidents loaded" -ForegroundColor White
Write-Host "  - GitHub commits, Jira issues, Confluence pages synced" -ForegroundColor White
Write-Host "  - Jenkins deployments recorded" -ForegroundColor White
Write-Host "  - Previous incidents loaded for correlation" -ForegroundColor White
Write-Host ""
Write-Host "Embeddings will be generated automatically by the Knowledge Service." -ForegroundColor Cyan
Write-Host ""
Write-Host "You can now test:" -ForegroundColor Yellow
Write-Host "  1. Log RCA with incident log files" -ForegroundColor White
Write-Host "  2. ELK Investigation" -ForegroundColor White
Write-Host "  3. Engineering Context Explorer" -ForegroundColor White
Write-Host ""

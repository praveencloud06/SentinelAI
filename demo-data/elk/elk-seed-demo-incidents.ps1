# ============================================================================
# SentinelAI ELK Demo Incidents Data Loader
# ============================================================================
# This script loads realistic log data for the 5 demo incidents into Elasticsearch
# Usage: .\elk-seed-demo-incidents.ps1
# ============================================================================

param(
    [string]$EsUrl = "http://localhost:9200",
    [string]$Index = "sentinelai-logs"
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " SentinelAI ELK Demo Incidents Data Loader" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Check Elasticsearch
Write-Host "[1/4] Checking Elasticsearch at $EsUrl..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$EsUrl/_cluster/health" -Method Get -TimeoutSec 5 | Out-Null
    Write-Host "  ✓ Elasticsearch is running" -ForegroundColor Green
}
catch {
    Write-Host "  ✗ Cannot reach Elasticsearch at $EsUrl" -ForegroundColor Red
    Write-Host "  Please start Elasticsearch and try again." -ForegroundColor Red
    exit 1
}

# Create index with mapping
Write-Host ""
Write-Host "[2/4] Creating index $Index..." -ForegroundColor Yellow

try { 
    Invoke-RestMethod -Uri "$EsUrl/$Index" -Method Delete -ErrorAction SilentlyContinue | Out-Null 
}
catch {
    # Ignore errors if index doesn't exist
}

$mapping = @{
    mappings = @{
        properties = @{
            timestamp   = @{ type = "date" }
            service     = @{ type = "keyword" }
            severity    = @{ type = "keyword" }
            message     = @{ type = "text" }
            traceId     = @{ type = "keyword" }
            environment = @{ type = "keyword" }
        }
    }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "$EsUrl/$Index" -Method Put -ContentType "application/json" -Body $mapping | Out-Null
Write-Host "  ✓ Index created" -ForegroundColor Green

# Load data
Write-Host ""
Write-Host "[3/4] Loading demo incident data..." -ForegroundColor Yellow

$dataFile = "elk-seed-data-enhanced.ndjson"

if (-not (Test-Path $dataFile)) {
    Write-Host "  ✗ $dataFile not found!" -ForegroundColor Red
    Write-Host "  Please ensure the file exists in the current directory." -ForegroundColor Red
    exit 1
}

$content = Get-Content $dataFile -Raw

try {
    $response = Invoke-RestMethod -Uri "$EsUrl/_bulk?refresh=true" `
        -Method Post `
        -ContentType "application/x-ndjson" `
        -Body $content `
        -TimeoutSec 60
    
    if ($response.errors -eq $false) {
        $itemCount = $response.items.Count
        Write-Host "  ✓ Successfully loaded $itemCount log entries" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ Loaded data but some errors occurred:" -ForegroundColor Yellow
        $errorCount = ($response.items | Where-Object { $_.index.error }).Count
        Write-Host "    $errorCount errors out of $($response.items.Count) items" -ForegroundColor Yellow
    }
}
catch {
    Write-Host "  ✗ Failed to load data: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Verify
Write-Host ""
Write-Host "[4/4] Verifying data..." -ForegroundColor Yellow

$count = Invoke-RestMethod -Uri "$EsUrl/$Index/_count" -Method Get
Write-Host "  Total documents: $($count.count)" -ForegroundColor White

# Show sample logs per service
$services = @("payment-service", "order-service", "vehicle-registration-service", "notification-service", "auth-service")

foreach ($service in $services) {
    $query = @{
        size = 1
        query = @{ 
            bool = @{ 
                filter = @(
                    @{ term = @{ service = $service } },
                    @{ term = @{ severity = "ERROR" } }
                ) 
            } 
        }
    } | ConvertTo-Json -Depth 10
    
    $result = Invoke-RestMethod -Uri "$EsUrl/$Index/_search" -Method Post -ContentType "application/json" -Body $query
    if ($result.hits.hits.Count -gt 0) {
        $hit = $result.hits.hits[0]._source
        Write-Host "  ✓ $service - $($hit.message.Substring(0, [Math]::Min(60, $hit.message.Length)))..." -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " Demo Data Loaded Successfully!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Demo Incidents Available:" -ForegroundColor Yellow
Write-Host "  1. Payment Service HikariCP Pool Exhaustion (2026-07-20)" -ForegroundColor White
Write-Host "  2. Order Service Kafka Consumer Lag (2026-07-18)" -ForegroundColor White
Write-Host "  3. Vehicle Registration Oracle Timeout (2026-07-15)" -ForegroundColor White
Write-Host "  4. Notification Service Redis Outage (2026-07-12)" -ForegroundColor White
Write-Host "  5. Auth Service JWT Validation Failure (2026-07-10)" -ForegroundColor White
Write-Host ""
Write-Host "Test ELK Investigation with:" -ForegroundColor Yellow
Write-Host "  Service: payment-service, Severity: ERROR, Timeframe: 60 min" -ForegroundColor Gray
Write-Host "  Service: order-service, Severity: WARN, Timeframe: 120 min" -ForegroundColor Gray
Write-Host ""
Write-Host "Kibana: http://localhost:5601" -ForegroundColor Cyan
Write-Host ""

# ============================================================================
# SentinelAI ELK Demo Incidents Data Loader
# ============================================================================

param(
    [string]$EsUrl = "http://localhost:9200",
    [string]$Index = "sentinelai-logs"
)

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " SentinelAI ELK Demo Incidents Data Loader" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Check Elasticsearch
Write-Host "[1/4] Checking Elasticsearch at $EsUrl..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$EsUrl/_cluster/health" -Method Get -TimeoutSec 5
    Write-Host "  OK Elasticsearch is running" -ForegroundColor Green
}
catch {
    Write-Host "  X Cannot reach Elasticsearch at $EsUrl" -ForegroundColor Red
    Write-Host "  Please start Elasticsearch and try again." -ForegroundColor Red
    exit 1
}

# Delete and create index
Write-Host ""
Write-Host "[2/4] Creating index $Index..." -ForegroundColor Yellow

try {
    Invoke-RestMethod -Uri "$EsUrl/$Index" -Method Delete -ErrorAction SilentlyContinue | Out-Null
}
catch {
    # Ignore if index doesn't exist
}

$mappingJson = @"
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
"@

try {
    Invoke-RestMethod -Uri "$EsUrl/$Index" -Method Put -ContentType "application/json" -Body $mappingJson | Out-Null
    Write-Host "  OK Index created" -ForegroundColor Green
}
catch {
    Write-Host "  X Failed to create index: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Load data
Write-Host ""
Write-Host "[3/4] Generating and loading demo incident data..." -ForegroundColor Yellow

# Generate fresh data with current timestamps
$dataFile = "elk-seed-data-enhanced.ndjson"

Write-Host "  Generating current timestamps..." -ForegroundColor Gray
try {
    & "$PSScriptRoot\generate-elk-demo-data.ps1" -OutputFile $dataFile | Out-Null
    Write-Host "  OK Data generated with current timestamps" -ForegroundColor Green
}
catch {
    Write-Host "  X Failed to generate data: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $dataFile)) {
    Write-Host "  X $dataFile not found!" -ForegroundColor Red
    exit 1
}

# Read as byte array to preserve exact formatting
$contentBytes = [System.IO.File]::ReadAllBytes((Resolve-Path $dataFile).Path)

try {
    $response = Invoke-RestMethod -Uri "$EsUrl/_bulk?refresh=true" `
        -Method Post `
        -ContentType "application/x-ndjson" `
        -Body $contentBytes `
        -TimeoutSec 60
    
    if ($response.errors -eq $false) {
        $itemCount = $response.items.Count
        Write-Host "  OK Successfully loaded $itemCount log entries" -ForegroundColor Green
    }
    else {
        Write-Host "  Warning: Loaded data but some errors occurred" -ForegroundColor Yellow
        $errorItems = $response.items | Where-Object { $_.index.error }
        if ($errorItems) {
            Write-Host "    $($errorItems.Count) errors out of $($response.items.Count) items" -ForegroundColor Yellow
        }
    }
}
catch {
    Write-Host "  X Failed to load data: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Verify
Write-Host ""
Write-Host "[4/4] Verifying data..." -ForegroundColor Yellow

try {
    $countResult = Invoke-RestMethod -Uri "$EsUrl/$Index/_count" -Method Get
    Write-Host "  Total documents: $($countResult.count)" -ForegroundColor White
}
catch {
    Write-Host "  Warning: Could not verify document count" -ForegroundColor Yellow
}

# Show sample from each service
$services = @("payment-service", "order-service", "vehicle-registration-service", "notification-service", "auth-service")

foreach ($service in $services) {
    $queryJson = @"
{
  "size": 1,
  "query": {
    "bool": {
      "filter": [
        { "term": { "service": "$service" } },
        { "term": { "severity": "ERROR" } }
      ]
    }
  }
}
"@
    
    try {
        $result = Invoke-RestMethod -Uri "$EsUrl/$Index/_search" -Method Post -ContentType "application/json" -Body $queryJson
        if ($result.hits.hits.Count -gt 0) {
            $hit = $result.hits.hits[0]._source
            $msgPreview = $hit.message.Substring(0, [Math]::Min(60, $hit.message.Length))
            Write-Host "  OK $service - $msgPreview..." -ForegroundColor Gray
        }
    }
    catch {
        Write-Host "  - $service (no errors found or query failed)" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " Demo Data Loaded Successfully!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Demo Incidents Available (with current timestamps):" -ForegroundColor Yellow
Write-Host "  1. Payment Service HikariCP Exhaustion (30 min ago)" -ForegroundColor White
Write-Host "  2. Order Service Kafka Consumer Lag (2 hours ago)" -ForegroundColor White
Write-Host "  3. Vehicle Registration Oracle Timeout (4 hours ago)" -ForegroundColor White
Write-Host "  4. Notification Service Redis Outage (8 hours ago)" -ForegroundColor White
Write-Host "  5. Auth Service JWT Validation Failure (12 hours ago)" -ForegroundColor White
Write-Host ""
Write-Host "Test ELK Investigation with:" -ForegroundColor Yellow
Write-Host "  Service: payment-service, Severity: ERROR, Time: Last 24 hours" -ForegroundColor Gray
Write-Host "  Service: order-service, Severity: WARN, Time: Last 24 hours" -ForegroundColor Gray
Write-Host ""
Write-Host "Kibana: http://localhost:5601" -ForegroundColor Cyan
Write-Host ""

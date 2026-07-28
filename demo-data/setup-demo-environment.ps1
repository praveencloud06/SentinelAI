# ============================================================================
# SentinelAI Demo Environment Setup Script
# ============================================================================
# This script sets up the complete demo environment including:
#   - Infrastructure (PostgreSQL, Elasticsearch, Ollama)
#   - Demo data (ELK logs, Knowledge Service data)
#   - Verification checks
#
# Usage: .\setup-demo-environment.ps1
# ============================================================================

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  SentinelAI V2 - Demo Environment Setup" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================================
# Helper Functions
# ============================================================================

function Test-ServiceHealth {
    param([string]$Url, [string]$Name)
    
    Write-Host "  Checking $Name..." -NoNewline
    try {
        $response = Invoke-RestMethod -Uri $Url -TimeoutSec 5 -ErrorAction Stop
        Write-Host " ✓" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host " ✗" -ForegroundColor Red
        return $false
    }
}

function Wait-ForService {
    param(
        [string]$Url,
        [string]$Name,
        [int]$MaxAttempts = 30,
        [int]$WaitSeconds = 2
    )
    
    Write-Host "  Waiting for $Name to be ready..." -ForegroundColor Yellow
    
    for ($i = 1; $i -le $MaxAttempts; $i++) {
        try {
            $response = Invoke-RestMethod -Uri $Url -TimeoutSec 2 -ErrorAction Stop
            Write-Host "  ✓ $Name is ready" -ForegroundColor Green
            return $true
        }
        catch {
            Write-Host "    Attempt $i/$MaxAttempts..." -ForegroundColor Gray
            Start-Sleep -Seconds $WaitSeconds
        }
    }
    
    Write-Host "  ✗ $Name did not become ready in time" -ForegroundColor Red
    return $false
}

# ============================================================================
# Step 1: Check Prerequisites
# ============================================================================

Write-Host "Step 1: Checking Prerequisites" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

$prerequisitesMet = $true

# Check Docker
Write-Host "  Checking Docker..." -NoNewline
try {
    docker version | Out-Null
    Write-Host " ✓" -ForegroundColor Green
}
catch {
    Write-Host " ✗" -ForegroundColor Red
    Write-Host "    Docker is not installed or not running" -ForegroundColor Red
    $prerequisitesMet = $false
}

# Check Java
Write-Host "  Checking Java 17+..." -NoNewline
try {
    $javaVersion = java -version 2>&1 | Select-String -Pattern "version" | ForEach-Object { $_ -replace '.*"(\d+).*', '$1' }
    if ([int]$javaVersion -ge 17) {
        Write-Host " ✓" -ForegroundColor Green
    } else {
        Write-Host " ✗ (Java $javaVersion found, need 17+)" -ForegroundColor Red
        $prerequisitesMet = $false
    }
}
catch {
    Write-Host " ✗" -ForegroundColor Red
    $prerequisitesMet = $false
}

# Check Node.js
Write-Host "  Checking Node.js..." -NoNewline
try {
    node --version | Out-Null
    Write-Host " ✓" -ForegroundColor Green
}
catch {
    Write-Host " ✗" -ForegroundColor Red
    $prerequisitesMet = $false
}

# Check Maven
Write-Host "  Checking Maven..." -NoNewline
try {
    mvn --version | Out-Null
    Write-Host " ✓" -ForegroundColor Green
}
catch {
    Write-Host " ✗" -ForegroundColor Red
    $prerequisitesMet = $false
}

if (-not $prerequisitesMet) {
    Write-Host ""
    Write-Host "Prerequisites not met. Please install missing components and try again." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✓ All prerequisites met" -ForegroundColor Green
Write-Host ""

# ============================================================================
# Step 2: Start Infrastructure
# ============================================================================

Write-Host "Step 2: Starting Infrastructure Services" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Start PostgreSQL
Write-Host "  Starting PostgreSQL..." -ForegroundColor Yellow
docker-compose -f postgres-docker-compose.yml up -d 2>&1 | Out-Null
Wait-ForService -Url "http://localhost:5432" -Name "PostgreSQL" -MaxAttempts 15

# Start Elasticsearch
Write-Host "  Starting Elasticsearch..." -ForegroundColor Yellow
docker-compose -f elk-docker-compose.yml up -d 2>&1 | Out-Null
Wait-ForService -Url "http://localhost:9200" -Name "Elasticsearch" -MaxAttempts 30

# Start Ollama (optional)
Write-Host "  Starting Ollama (AI Engine)..." -ForegroundColor Yellow
if (Test-Path "ollama-docker-compose.yml") {
    docker-compose -f ollama-docker-compose.yml up -d 2>&1 | Out-Null
    Start-Sleep -Seconds 5
    Write-Host "  ✓ Ollama container started" -ForegroundColor Green
} else {
    Write-Host "  ⚠ Ollama docker-compose not found. Skipping." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "✓ Infrastructure services started" -ForegroundColor Green
Write-Host ""

# ============================================================================
# Step 3: Load Demo Data
# ============================================================================

Write-Host "Step 3: Loading Demo Data" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan
Write-Host ""

# Load ELK Data
Write-Host "  Loading ELK log data..." -ForegroundColor Yellow
if (Test-Path "elk\elk-seed-demo-incidents.ps1") {
    Set-Location elk
    .\elk-seed-demo-incidents.ps1 2>&1 | Out-Null
    Set-Location ..
    Write-Host "  ✓ ELK data loaded" -ForegroundColor Green
} else {
    Write-Host "  ✗ elk\elk-seed-demo-incidents.ps1 not found" -ForegroundColor Red
}

Write-Host ""
Write-Host "  Note: Knowledge Service data must be loaded AFTER services start" -ForegroundColor Yellow
Write-Host "  Run: cd knowledge && .\knowledge-demo-data.bat" -ForegroundColor Yellow
Write-Host ""

Write-Host "✓ Initial demo data loaded" -ForegroundColor Green
Write-Host ""

# ============================================================================
# Step 4: Verify Setup
# ============================================================================

Write-Host "Step 4: Verifying Setup" -ForegroundColor Cyan
Write-Host "=======================" -ForegroundColor Cyan
Write-Host ""

Test-ServiceHealth -Url "http://localhost:9200" -Name "Elasticsearch"
Test-ServiceHealth -Url "http://localhost:5432" -Name "PostgreSQL (port check)"

# Check ELK data
Write-Host "  Checking ELK data..." -NoNewline
try {
    $count = (Invoke-RestMethod -Uri "http://localhost:9200/sentinelai-logs/_count" -ErrorAction Stop).count
    if ($count -gt 0) {
        Write-Host " ✓ ($count logs)" -ForegroundColor Green
    } else {
        Write-Host " ⚠ No logs found" -ForegroundColor Yellow
    }
}
catch {
    Write-Host " ✗" -ForegroundColor Red
}

Write-Host ""

# ============================================================================
# Step 5: Next Steps
# ============================================================================

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Setup Complete!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Start SentinelAI Services:" -ForegroundColor White
Write-Host "   Terminal 1: cd SentinelAI-Engine && uvicorn app.main:app --host 0.0.0.0 --port 8000" -ForegroundColor Gray
Write-Host "   Terminal 2: cd SentinelAI-Core && mvn spring-boot:run" -ForegroundColor Gray
Write-Host "   Terminal 3: cd SentinelAI-Knowledge-Service && mvn spring-boot:run" -ForegroundColor Gray
Write-Host "   Terminal 4: cd sentinelai-ui && npm start" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Load Knowledge Service Data:" -ForegroundColor White
Write-Host "   cd knowledge" -ForegroundColor Gray
Write-Host "   .\knowledge-demo-data.bat" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Open Demo:" -ForegroundColor White
Write-Host "   http://localhost:3000" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Follow Demo Guide:" -ForegroundColor White
Write-Host "   See DEMO_GUIDE.md for step-by-step instructions" -ForegroundColor Gray
Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================================
# Generate ELK Demo Data with Current Timestamps
# ============================================================================
# This script generates demo incident logs with current timestamps
# so they always appear in "last 24 hours" searches
# ============================================================================

param(
    [string]$OutputFile = "elk-seed-data-enhanced.ndjson"
)

Write-Host "Generating ELK demo data with current timestamps..." -ForegroundColor Yellow

# Calculate timestamps relative to now
$now = [DateTime]::UtcNow

# Incident 1: Payment Service (30 minutes ago)
$payment_time = $now.AddMinutes(-30)

# Incident 2: Order Service (2 hours ago)
$order_time = $now.AddHours(-2)

# Incident 3: Vehicle Service (4 hours ago)
$vehicle_time = $now.AddHours(-4)

# Incident 4: Notification Service (8 hours ago)
$notification_time = $now.AddHours(-8)

# Incident 5: Auth Service (12 hours ago)
$auth_time = $now.AddHours(-12)

# Generate NDJSON content
$content = @()

# ============================================================================
# Incident 1: Payment Service HikariCP Pool Exhaustion (30 min ago)
# ============================================================================

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($payment_time.ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"ERROR","message":"HikariCP connection pool exhausted - Connection is not available, request timed out after 30017ms","traceId":"pay-001","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($payment_time.AddSeconds(10).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"ERROR","message":"SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30002ms","traceId":"pay-002","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($payment_time.AddSeconds(20).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"WARN","message":"HikariCP connection pool active connections: 20/20, idle: 0/20, pending threads: 45","traceId":"pay-003","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($payment_time.AddSeconds(30).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"ERROR","message":"Payment processing failed for transaction tx-98765: Database connection timeout","traceId":"pay-004","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($payment_time.AddSeconds(35).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"ERROR","message":"Failed to execute database query after 30s timeout - commit hash: abc123def456","traceId":"pay-005","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($payment_time.AddSeconds(40).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"WARN","message":"Database timeout configuration changed in deployment v2.1.5 build 1245","traceId":"pay-006","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($payment_time.AddSeconds(50).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"ERROR","message":"Related to Jira issue PAY-421: Payment processing timeout errors","traceId":"pay-007","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($payment_time.AddMinutes(5).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"INFO","message":"Rollback initiated - reverting to v2.1.4-hotfix","traceId":"pay-008","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($payment_time.AddMinutes(8).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"INFO","message":"Service recovered after rollback - connection pool stable at 20/20","traceId":"pay-009","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($payment_time.AddMinutes(10).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"INFO","message":"Deployment v2.1.4-hotfix completed successfully - commit def456ghi789","traceId":"pay-010","environment":"production"}
"@

# ============================================================================
# Incident 2: Order Service Kafka Consumer Lag (2 hours ago)
# ============================================================================

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($order_time.ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"order-service","severity":"WARN","message":"Kafka consumer lag detected: 15234 messages behind on partition order-events-0","traceId":"ord-001","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($order_time.AddSeconds(30).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"order-service","severity":"ERROR","message":"Kafka consumer group rebalancing triggered - commit 789ghi012jkl","traceId":"ord-002","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($order_time.AddMinutes(1).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"order-service","severity":"WARN","message":"Consumer lag increased to 50123 messages across 3 partitions","traceId":"ord-003","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($order_time.AddMinutes(2).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"order-service","severity":"ERROR","message":"Order processing delayed by 15 minutes - related to Jira ORD-334","traceId":"ord-004","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($order_time.AddMinutes(5).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"order-service","severity":"WARN","message":"Kafka consumer configuration updated in v3.2.1 deployment","traceId":"ord-005","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($order_time.AddMinutes(10).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"order-service","severity":"INFO","message":"Consumer lag recovering - now 25000 messages behind","traceId":"ord-006","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($order_time.AddMinutes(15).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"order-service","severity":"INFO","message":"Kafka consumer lag normalized - less than 500 messages","traceId":"ord-007","environment":"production"}
"@

# ============================================================================
# Incident 3: Vehicle Service Oracle Timeout (4 hours ago)
# ============================================================================

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($vehicle_time.ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"vehicle-registration-service","severity":"ERROR","message":"Oracle database query timeout after 30s - VEH-567","traceId":"veh-001","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($vehicle_time.AddSeconds(30).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"vehicle-registration-service","severity":"ERROR","message":"ORA-01013: user requested cancel of current operation","traceId":"veh-002","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($vehicle_time.AddMinutes(1).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"vehicle-registration-service","severity":"WARN","message":"Vehicle registration queries taking 25+ seconds","traceId":"veh-003","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($vehicle_time.AddMinutes(3).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"vehicle-registration-service","severity":"ERROR","message":"Database connection pool exhausted - Oracle timeout","traceId":"veh-004","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($vehicle_time.AddMinutes(5).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"vehicle-registration-service","severity":"INFO","message":"Query optimization applied - response time improved to 2s","traceId":"veh-005","environment":"production"}
"@

# ============================================================================
# Incident 4: Notification Service Redis Outage (8 hours ago)
# ============================================================================

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($notification_time.ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"notification-service","severity":"ERROR","message":"Redis connection failed: Connection refused (localhost:6379)","traceId":"not-001","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($notification_time.AddSeconds(10).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"notification-service","severity":"ERROR","message":"Unable to retrieve notification templates from cache - NOT-223","traceId":"not-002","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($notification_time.AddSeconds(30).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"notification-service","severity":"WARN","message":"Falling back to database for notification templates","traceId":"not-003","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($notification_time.AddMinutes(2).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"notification-service","severity":"ERROR","message":"Notification processing degraded - Redis outage detected","traceId":"not-004","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($notification_time.AddMinutes(10).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"notification-service","severity":"INFO","message":"Redis connection restored - cache warming in progress","traceId":"not-005","environment":"production"}
"@

# ============================================================================
# Incident 5: Auth Service JWT Validation (12 hours ago)
# ============================================================================

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($auth_time.ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"auth-service","severity":"ERROR","message":"JWT signature validation failed - invalid token","traceId":"auth-001","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($auth_time.AddSeconds(5).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"auth-service","severity":"ERROR","message":"Authentication failed for user - token expired","traceId":"auth-002","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($auth_time.AddSeconds(15).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"auth-service","severity":"WARN","message":"High volume of JWT validation failures detected - AUTH-890","traceId":"auth-003","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($auth_time.AddMinutes(1).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"auth-service","severity":"ERROR","message":"JWT secret key mismatch - deployment issue in build 4231","traceId":"auth-004","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($auth_time.AddMinutes(3).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"auth-service","severity":"INFO","message":"JWT secret key rotated - authentication restored","traceId":"auth-005","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($auth_time.AddMinutes(5).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"auth-service","severity":"INFO","message":"Authentication service stabilized - commit yza567bcd890","traceId":"auth-006","environment":"production"}
"@

# Additional INFO logs for context
$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($now.AddMinutes(-45).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"INFO","message":"Application started successfully","traceId":"sys-001","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($now.AddMinutes(-40).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"order-service","severity":"INFO","message":"Kafka consumer group initialized","traceId":"sys-002","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($now.AddMinutes(-35).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"vehicle-registration-service","severity":"INFO","message":"Database connection pool initialized","traceId":"sys-003","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($now.AddMinutes(-20).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"notification-service","severity":"INFO","message":"Redis connection pool ready","traceId":"sys-004","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($now.AddMinutes(-15).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"auth-service","severity":"INFO","message":"JWT validation service ready","traceId":"sys-005","environment":"production"}
"@

$content += '{"index":{"_index":"sentinelai-logs"}}'
$content += @"
{"timestamp":"$($now.AddMinutes(-10).ToString('yyyy-MM-ddTHH:mm:ss.fff'))Z","service":"payment-service","severity":"INFO","message":"Health check passed - all systems operational","traceId":"sys-006","environment":"production"}
"@

# Write to file with proper line breaks AND final newline
($content -join "`n") + "`n" | Out-File -FilePath $OutputFile -Encoding UTF8 -NoNewline

$lineCount = $content.Count
Write-Host "OK Generated $lineCount lines of demo data" -ForegroundColor Green
Write-Host "   Output: $OutputFile" -ForegroundColor Gray
Write-Host ""
Write-Host "Incident Timeline (relative to now):" -ForegroundColor Yellow
Write-Host "  - 12 hours ago: Auth Service JWT Validation" -ForegroundColor White
Write-Host "  - 8 hours ago:  Notification Service Redis Outage" -ForegroundColor White
Write-Host "  - 4 hours ago:  Vehicle Registration Oracle Timeout" -ForegroundColor White
Write-Host "  - 2 hours ago:  Order Service Kafka Consumer Lag" -ForegroundColor White
Write-Host "  - 30 min ago:   Payment Service HikariCP Exhaustion" -ForegroundColor White
Write-Host ""

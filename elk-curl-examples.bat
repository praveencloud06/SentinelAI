@echo off
:: ============================================================
:: SentinelAI – ELK Investigation API – curl examples
::
:: Prerequisites:
::   1. elk-docker-compose.yml running  (Elasticsearch + Kibana)
::   2. postgres-docker-compose.yml running  (Postgres)
::   3. SentinelAI-Engine running  (python main.py in SentinelAI-Engine/)
::   4. SentinelAI-Core running on port 8080
::        (mvn spring-boot:run -Dspring-boot.run.profiles=local)
::   5. Sample data loaded  (run elk-seed-data.bat)
::
:: Usage: just read the examples – copy the curl lines you need
::        and paste them in any terminal / Git Bash / PowerShell.
:: ============================================================

echo.
echo ============================================================
echo  EXAMPLE 1: Investigate ERROR logs for payment-service (30 min)
echo ============================================================
echo.
echo curl -s -X POST http://localhost:8080/api/elk-investigation/search ^
echo   -H "Content-Type: application/json" ^
echo   -d "{\"service\":\"payment-service\",\"severity\":\"ERROR\",\"timeframeMinutes\":30}"
echo.

echo ============================================================
echo  EXAMPLE 2: Investigate WARN logs for order-service (1 hour)
echo ============================================================
echo.
echo curl -s -X POST http://localhost:8080/api/elk-investigation/search ^
echo   -H "Content-Type: application/json" ^
echo   -d "{\"service\":\"order-service\",\"severity\":\"WARN\",\"timeframeMinutes\":60}"
echo.

echo ============================================================
echo  EXAMPLE 3: Pretty-print response
echo ============================================================
echo.
echo curl -s -X POST http://localhost:8080/api/elk-investigation/search ^
echo   -H "Content-Type: application/json" ^
echo   -d "{\"service\":\"payment-service\",\"severity\":\"ERROR\",\"timeframeMinutes\":60}" ^
echo   ^| python -m json.tool
echo.

echo ============================================================
echo  EXAMPLE 4: Validation error (missing service)
echo ============================================================
echo.
echo curl -s -X POST http://localhost:8080/api/elk-investigation/search ^
echo   -H "Content-Type: application/json" ^
echo   -d "{\"severity\":\"ERROR\",\"timeframeMinutes\":30}"
echo.

echo ============================================================
echo  EXAMPLE 5: Elasticsearch direct query – verify seed data
echo ============================================================
echo.
echo curl -s "http://localhost:9200/logs-application/_search?pretty^&size=3"
echo.

echo ============================================================
echo  EXAMPLE 6: Elasticsearch – count documents by service
echo ============================================================
echo.
echo curl -s -X POST "http://localhost:9200/logs-application/_search?pretty" ^
echo   -H "Content-Type: application/json" ^
echo   -d "{\"size\":0,\"aggs\":{\"by_service\":{\"terms\":{\"field\":\"service\"}}}}"
echo.

echo ============================================================
echo  EXAMPLE 7: Existing RCA endpoint (unchanged)
echo ============================================================
echo.
echo curl -s -X POST http://localhost:8080/api/rca/analyze ^
echo   -H "Content-Type: application/json" ^
echo   -d "{\"log\":\"ERROR: NullPointerException in PaymentService at line 42\"}"
echo.

echo ============================================================
echo  EXAMPLE 8: Swagger UI
echo ============================================================
echo   Open in browser: http://localhost:8080/swagger-ui/index.html
echo.

echo ============================================================
echo  EXAMPLE 9: Kibana
echo ============================================================
echo   Open in browser: http://localhost:5601
echo   Go to: Analytics ^> Discover ^> create data view for logs-application
echo.

pause

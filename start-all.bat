@echo off
setlocal EnableDelayedExpansion

cd /d "%~dp0"

echo.
echo ================================================================
echo  SentinelAI -- Start Application Services
echo ================================================================
echo.
echo  This script starts:
echo    [1] SentinelAI-Engine  (Python / uvicorn  :8000)
echo    [2] SentinelAI-Core    (Spring Boot Maven  :8080)
echo    [3] sentinelai-ui      (React npm start    :3000)
echo.
echo  NOTE: Docker services (Postgres, Elasticsearch, Kibana) must
echo        already be running before starting this script.
echo        Use postgres-docker-compose.yml and elk-docker-compose.yml.
echo.
echo  Each service opens in its own terminal window.
echo ================================================================
echo.

:: ── [1] SentinelAI-Engine (Python / uvicorn) ─────────────────────────────────
echo [1/3] Starting SentinelAI-Engine (Python)...
start "SentinelAI-Engine" cmd /k "cd /d "%~dp0SentinelAI-Engine" && pip install -q -r requirements.txt && python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload"
echo  OK  -- AI Engine will be at http://localhost:8000
echo.

:: ── [2] SentinelAI-Core (Spring Boot) ────────────────────────────────────────
echo [2/3] Starting SentinelAI-Core (Spring Boot)...
start "SentinelAI-Core" cmd /k "cd /d "%~dp0SentinelAI-Core" && mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local -Dspring.profiles.active=local"
echo  OK  -- Core API will be at http://localhost:8080
echo       -- Swagger UI        http://localhost:8080/swagger-ui/index.html
echo.

:: Wait for Spring Boot to start before launching UI
echo  Waiting 20s for Spring Boot to start...
ping -n 21 127.0.0.1 >nul

:: ── [3] sentinelai-ui (React) ────────────────────────────────────────────────
echo [3/3] Starting sentinelai-ui (React)...
if not exist "%~dp0sentinelai-ui\node_modules" (
    echo  node_modules not found -- running npm install first...
    start "sentinelai-ui" cmd /k "cd /d "%~dp0sentinelai-ui" && npm install && npm start"
) else (
    start "sentinelai-ui" cmd /k "cd /d "%~dp0sentinelai-ui" && npm start"
)
echo  OK  -- UI will be at http://localhost:3000
echo.

:: ── Summary ───────────────────────────────────────────────────────────────────
echo ================================================================
echo  All application services launched.  URLs at a glance:
echo.
echo    React UI          http://localhost:3000
echo    Spring Boot API   http://localhost:8080
echo    Swagger UI        http://localhost:8080/swagger-ui/index.html
echo    AI Engine         http://localhost:8000
echo.
echo  To load sample ELK data (first run only):
echo    run  elk-seed-data.bat
echo ================================================================
echo.
pause

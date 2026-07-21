@echo off
setlocal

REM ============================================================
REM SentinelAI - Seed Elasticsearch with enterprise demo logs
REM ============================================================

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0elk-seed-data.ps1"
set EXIT_CODE=%ERRORLEVEL%

endlocal & exit /b %EXIT_CODE%

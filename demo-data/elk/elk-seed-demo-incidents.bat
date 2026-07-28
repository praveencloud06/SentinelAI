@echo off
REM ============================================================================
REM SentinelAI ELK Demo Incidents Data Loader (Batch Wrapper)
REM ============================================================================

echo.
echo =================================================
echo SentinelAI ELK Demo Incidents Data Loader
echo =================================================
echo.

powershell.exe -ExecutionPolicy Bypass -File "%~dp0elk-seed-demo-incidents-fixed.ps1"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ELK data load failed. Check the output above for errors.
    pause
    exit /b 1
)

pause

@echo off
REM ============================================================================
REM SentinelAI Knowledge Service Demo Data Loader
REM ============================================================================

echo.
echo =================================================
echo SentinelAI Knowledge Service Demo Data Loader
echo =================================================
echo.

powershell.exe -ExecutionPolicy Bypass -File "%~dp0knowledge-demo-data-simple.ps1"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Demo data load failed. Check the output above for errors.
    pause
    exit /b 1
)

echo.
echo Demo data loaded successfully!
echo.
pause

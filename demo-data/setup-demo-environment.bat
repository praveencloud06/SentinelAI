@echo off
REM ============================================================================
REM SentinelAI Demo Environment Setup (Batch Wrapper)
REM ============================================================================
REM This batch file calls the PowerShell setup script
REM Usage: setup-demo-environment.bat
REM ============================================================================

echo.
echo ============================================================
echo   SentinelAI V2 - Demo Environment Setup
echo ============================================================
echo.

powershell.exe -ExecutionPolicy Bypass -File "%~dp0setup-demo-environment.ps1"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Setup failed. Check the output above for errors.
    pause
    exit /b 1
)

pause

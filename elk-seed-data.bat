@echo off
REM ============================================================================
REM SentinelAI ELK Demo Data Loader (Redirect)
REM ============================================================================
REM This script redirects to the new demo data location
REM For direct access, use: demo-data\elk\elk-seed-demo-incidents.bat
REM ============================================================================

echo.
echo =================================================================
echo SentinelAI ELK Demo Data Loader
echo =================================================================
echo.
echo Note: Demo data has been reorganized into demo-data/elk/
echo Redirecting to: demo-data\elk\elk-seed-demo-incidents.bat
echo.
echo For future use, run directly from:
echo   cd demo-data\elk
echo   .\elk-seed-demo-incidents.bat
echo.
pause

cd demo-data\elk
call elk-seed-demo-incidents.bat
cd ..\..

exit /b %ERRORLEVEL%

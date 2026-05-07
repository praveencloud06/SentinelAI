@echo off
echo.
echo ================================================================
echo  SentinelAI -- Stop Application Services
echo ================================================================
echo.
echo  SentinelAI-Engine, SentinelAI-Core, and sentinelai-ui each
echo  run in their own terminal window.
echo.
echo  To stop them: switch to each window and press Ctrl+C,
echo  or close the window directly.
echo.
echo  Docker services (Postgres, Elasticsearch, Kibana) are NOT
echo  affected by this script -- stop them via their compose files
echo  when needed:
echo.
echo    docker compose -f postgres-docker-compose.yml down
echo    docker compose -f elk-docker-compose.yml down
echo.
echo ================================================================
pause

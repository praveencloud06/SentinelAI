@echo off
cd /d "C:\application\AI_Project\SentinelAI"

:: Start Postgres and pgAdmin first
echo Starting Database...
docker compose -f postgres-docker-compose.yml up -d

echo Services are starting in the background. 
echo pgAdmin: http://localhost:5050
:: echo Ollama API: http://localhost:11434
pause
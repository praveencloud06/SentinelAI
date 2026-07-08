@echo off
cd /d "C:\application\AI_Project\SentinelAI"

:: Start Postgres and pgAdmin first
echo Starting Database...
docker compose -f postgres-docker-compose.yml up -d

echo Starting ELK Stack...
docker compose -f elk-docker-compose.yml up -d

echo Services are starting in the background. 
echo.
echo ========================================
echo PostgreSQL : localhost:5111
echo pgAdmin    : http://localhost:5050
echo Elasticsearch : http://localhost:9200
echo Kibana     : http://localhost:5601
echo ========================================
pause
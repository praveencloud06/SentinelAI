# Connecting to Postgres from Dockerized Tools

If you are running pgAdmin or other tools inside a Docker container, use host.docker.internal as the host to connect to the Postgres service running in another container or on the host. Example connection settings:

- Host: host.docker.internal
- Port: 5111
- Username: sentinel
- Password: sentinel_pass
- Database: sentinelai

If you are running pgAdmin in your browser on your Windows host, you can use localhost or 127.0.0.1 as the host.
## Jira Integration

After RCA is confirmed, you can create a Jira ticket automatically:

- Configure Jira in `application-local.yml`:
	```yaml
	jira:
		base-url: http://localhost:8081 # Change to your Jira instance URL
		username: <jira-username>
		api-token: <jira-api-token>
	```

### Sample API Request (cURL)

```
curl -X POST "http://localhost:8080/api/rca/jira?projectKey=PROJ" \
	-H "Content-Type: application/json" \
	-d '{
		"issue": "Kafka timeout issue",
		"rootCause": "Network partition between app and broker",
		"impactedService": "OrderService",
		"recommendedFix": "Check VPC routing and restart Kafka client"
	}'
```

This will create a Jira ticket in the specified project with RCA details.
# SentinelAI - Project Progress & Overview

## What has been developed
- Project structure following Clean Architecture (Controller → Service → Repository)
- Java 17+ (recommended 21), Spring Boot 3.5.x base
- Modular directories for DTOs, models, exceptions, configs
- Test directories for controller, service, repository, integration

## Next Steps
1. Add production-grade pom.xml with all dependencies
2. Add docker-compose.yml for PostgreSQL (pgvector) and Ollama
3. Add application-local.yml config
4. Implement Log Ingestion API (Controller, Service, Repository, DTOs, Exception Handling)

## Configuration
- All configs externalized using Spring Profiles (local, aws)
- No hardcoded credentials or URLs
- Use application-local.yml for local development

## High-Level Flow
1. Log Upload (API) → Store in DB → Preprocess → Embed (Ollama) → Store Embedding
2. Semantic Search (API) → Vector similarity search in DB
3. RCA Generator (API) → Retrieve similar logs (RAG) → Analyze with Ollama → Structured RCA output

## Features (MVP)
- Log Ingestion API (upload JSON, CSV, or text logs)
- Vector Embedding + Storage (Ollama + Spring AI, pgvector)
- Semantic Search API (natural language, top-5 similar logs)
- RCA Generator (contextual analysis, structured JSON output)
- AI Integration (Spring AI abstraction for Ollama LLM)

## Ollama & Spring AI Integration
- Ollama runs locally (Docker, port 11434) and provides LLM models (Llama3/Mistral)
- Spring AI provides unified interfaces for embedding and chat (LLM) tasks
- EmbeddingModel: Converts logs/queries to vectors for similarity search
- ChatModel: Used for RCA generation (structured JSON output)
- All configs externalized in `application-local.yml`
- No external API calls—100% local

## How to Run (Hot Run)
1. Start Docker Compose:
	```sh
	docker-compose up -d
	```
2. Build and run the Spring Boot app:
	```sh
	./mvnw spring-boot:run
	```
	or use your IDE (run SentinelAiApplication)
3. The app will connect to local PostgreSQL and Ollama automatically (see `application-local.yml`)

## API Documentation (Swagger/OpenAPI)

After starting the app, access interactive API docs at:
 - [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
You can try all endpoints directly from the browser.

## API Endpoints
- `POST /api/logs/upload` — Upload log
- `POST /api/logs/search` — Semantic search
- `POST /api/rca/analyze` — RCA generator

See below for sample API requests.

## Sample API Requests (cURL)

### 1. Upload Log
```
curl -X POST http://localhost:8080/api/logs/upload \
	-H "Content-Type: application/json" \
	-d '{
		"logType": "text",
		"content": "2026-04-24T12:34:56.789Z ERROR [service] Kafka timeout exception at ..."
	}'
```

### 2. Semantic Search
```
curl -X POST http://localhost:8080/api/logs/search \
	-H "Content-Type: application/json" \
	-d '{
		"query": "Kafka timeout issue"
	}'
```

### 3. RCA Generator
```
curl -X POST http://localhost:8080/api/rca/analyze \
	-H "Content-Type: application/json" \
	-d '{
		"log": "2026-04-24T12:34:56.789Z ERROR [service] Kafka timeout exception at ..."
	}'
```

## Non-Functional
- 100% local execution
- Modular, future-ready for AWS
- Unit tests (JUnit), integration tests (Testcontainers)

---

Update this file as features/configs are added or changed.

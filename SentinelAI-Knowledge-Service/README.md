# SentinelAI Knowledge Service (SKS)

Independent Spring Boot microservice for building the SentinelAI engineering knowledge index. SKS synchronizes metadata from GitHub/GitLab-style repositories, Jira, Confluence, and Jenkins without performing RCA or AI reasoning.

## Scope

- Stores engineering metadata only. Source code is not stored.
- Builds a normalized index of repositories, commits, releases, Jira issues, Confluence documents, deployments, events, and relationships.
- Exposes timeline, relationship, and search APIs for future RCA consumption.
- Uses connector interfaces so real enterprise integrations can replace the current foundation connectors cleanly.

## Run Locally

```powershell
docker compose up -d
.\mvnw spring-boot:run
```

If Maven Wrapper is not present, use:

```powershell
mvn spring-boot:run
```

The service starts on `http://localhost:8090`.

## Main APIs

- `POST /api/sync/github`
- `POST /api/sync/jira`
- `POST /api/sync/confluence`
- `POST /api/sync/jenkins`
- `POST /api/sync/all`
- `POST /api/webhooks/{sourceSystem}`
- `GET /api/repositories`
- `GET /api/repositories/{id}`
- `GET /api/commits`
- `GET /api/commits/{hash}`
- `GET /api/jira/issues`
- `GET /api/deployments`
- `GET /api/releases`
- `GET /api/timeline`
- `GET /api/relationships`
- `GET /api/search/deployments?commitHash=...`
- `GET /api/search/jira?commitHash=...`
- `GET /api/search/releases?version=...`

Example sync body:

```json
{
  "tenantId": "default",
  "mode": "INCREMENTAL",
  "parameters": {
    "repository": "sentinelai/api"
  }
}
```

## Architecture

The package structure follows a hexagonal layout:

- `api`: controllers, DTOs, mappers
- `application`: orchestration/query services
- `domain`: enums and core vocabulary
- `connector`: connector contracts and first-phase connector implementations
- `infrastructure`: JPA entities and repositories
- `scheduler`: optional scheduled synchronization

## Notes

The current connector implementations create deterministic sample metadata through the real persistence path. They are intentionally not AI-enabled and do not call external systems yet. The next step is replacing each connector body with authenticated clients for GitHub/GitLab, Jira, Confluence, and Jenkins while preserving the API and persistence contracts.

## Webhook Ingestion

SKS accepts pushed events from external systems and normalizes them into the same tables used by scheduled/API sync.

Supported first-pass webhook payloads:

- `POST /api/webhooks/GITHUB` with GitHub `push`, `pull_request`, or `release` payloads
- `POST /api/webhooks/GITLAB` with GitLab push/merge request style payloads
- `POST /api/webhooks/JIRA` with Jira issue webhook payloads
- `POST /api/webhooks/CONFLUENCE` with Confluence page/content payloads
- `POST /api/webhooks/JENKINS` with Jenkins build/deployment payloads

The webhook layer stores metadata only, creates engineering events, and links Jira keys found in commit messages or pull request titles.

## External Platform Configuration

All external platform endpoints and credentials are configured under `sentinelai.knowledge.platforms` in `application.yml`. Use environment variables for real credentials.

Key settings:

```yaml
sentinelai:
  knowledge:
    platforms:
      github:
        enabled: true
        base-url: https://api.github.com
        username: ${SKS_GITHUB_USERNAME:}
        api-token: ${SKS_GITHUB_API_TOKEN:}
        webhook-secret: ${SKS_GITHUB_WEBHOOK_SECRET:}
        repositories:
          - your-org/your-repo
      jira:
        enabled: true
        base-url: https://your-company.atlassian.net
        username: ${SKS_JIRA_USERNAME:}
        api-token: ${SKS_JIRA_API_TOKEN:}
        webhook-secret: ${SKS_JIRA_WEBHOOK_SECRET:}
        project-keys:
          - PAY
      confluence:
        enabled: true
        base-url: https://your-company.atlassian.net/wiki
        username: ${SKS_CONFLUENCE_USERNAME:}
        api-token: ${SKS_CONFLUENCE_API_TOKEN:}
        webhook-secret: ${SKS_CONFLUENCE_WEBHOOK_SECRET:}
        space-keys:
          - ARCH
      jenkins:
        enabled: true
        base-url: https://jenkins.your-company.com
        username: ${SKS_JENKINS_USERNAME:}
        api-token: ${SKS_JENKINS_API_TOKEN:}
        webhook-secret: ${SKS_JENKINS_WEBHOOK_SECRET:}
        job-names:
          - payment-service-deploy
```

If `webhook-secret` is set, webhook requests must include the configured token header, defaulting to `x-sentinelai-webhook-token`.

Example local ingestion:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8090/api/webhooks/GITHUB -ContentType application/json -Headers @{"X-GitHub-Event"="push"} -InFile examples/github-push.json
Invoke-RestMethod -Method Post -Uri http://localhost:8090/api/webhooks/JIRA -ContentType application/json -InFile examples/jira-issue.json
Invoke-RestMethod -Method Post -Uri http://localhost:8090/api/webhooks/CONFLUENCE -ContentType application/json -InFile examples/confluence-page.json
Invoke-RestMethod -Method Post -Uri http://localhost:8090/api/webhooks/JENKINS -ContentType application/json -InFile examples/jenkins-deployment.json
Invoke-RestMethod http://localhost:8090/api/timeline
Invoke-RestMethod "http://localhost:8090/api/relationships?sourceType=Commit&sourceId=abc123"
```

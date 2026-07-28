# pgvector Extension Setup for SentinelAI

## Problem

The Knowledge Service requires the `pgvector` extension for storing and searching vector embeddings. If PostgreSQL doesn't have this extension installed, the service will fail to start with:

```
ERROR: type "vector" does not exist
```

## Solution

### Quick Fix (Docker Container)

If using the provided Docker PostgreSQL setup:

```bash
# Install pgvector extension
docker exec sentinelai-postgres psql -U sentinel -d sentinelai_knowledge -c "CREATE EXTENSION IF NOT EXISTS vector;"

# Verify installation
docker exec sentinelai-postgres psql -U sentinel -d sentinelai_knowledge -c "\dx vector"
```

Expected output:
```
                           List of installed extensions
  Name  | Version | Schema |                     Description                      
--------+---------+--------+------------------------------------------------------
 vector | 0.5.1   | public | vector data type and ivfflat and hnsw access methods
```

## Docker Setup (Already Configured)

The `postgres-docker-compose.yml` already uses the `ankane/pgvector:latest` image which includes pgvector pre-compiled. You just need to enable the extension in your database.

## Manual PostgreSQL Setup

If using a standalone PostgreSQL installation:

### 1. Install pgvector

**Ubuntu/Debian:**
```bash
sudo apt install postgresql-15-pgvector
```

**macOS (Homebrew):**
```bash
brew install pgvector
```

**Windows:**
Download from: https://github.com/pgvector/pgvector/releases

### 2. Enable Extension

```sql
-- Connect to your database
psql -U sentinel -d sentinelai_knowledge

-- Create extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Verify
\dx vector
```

## Verify Knowledge Service Starts

After installing pgvector, start the Knowledge Service:

```bash
cd SentinelAI-Knowledge-Service
mvn spring-boot:run
```

You should see:
```
Started KnowledgeServiceApplication in X.XXX seconds
```

The `knowledge_embeddings` table will be created automatically with the `vector(1536)` column type.

## What is pgvector?

pgvector is a PostgreSQL extension that adds:
- **vector data type** - Store embeddings as arrays of floats
- **vector similarity search** - Fast cosine similarity, L2 distance, inner product
- **HNSW indexing** - Hierarchical Navigable Small World graphs for fast ANN search
- **IVFFlat indexing** - Inverted file with flat compression

Used by SentinelAI for:
- Semantic search across engineering artifacts
- Finding similar incidents, commits, Jira issues
- AI-powered context retrieval

## Troubleshooting

### Extension not available
```
ERROR: extension "vector" is not available
```

**Solution:** The pgvector extension is not installed in PostgreSQL. Follow the installation steps above.

### Permission denied
```
ERROR: permission denied to create extension "vector"
```

**Solution:** You need superuser privileges. Use:
```bash
docker exec sentinelai-postgres psql -U postgres -d sentinelai_knowledge -c "CREATE EXTENSION vector;"
```

### Container not found
```
Error: No such container: sentinelai-postgres
```

**Solution:** Start PostgreSQL container first:
```bash
docker-compose -f postgres-docker-compose.yml up -d
```

## References

- pgvector GitHub: https://github.com/pgvector/pgvector
- pgvector Docs: https://github.com/pgvector/pgvector#usage
- Docker Image: https://hub.docker.com/r/ankane/pgvector

---

**Status:** ✅ Extension installed and verified (2026-07-23)

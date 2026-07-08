CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE repositories (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    source_system VARCHAR(40) NOT NULL,
    external_id VARCHAR(256) NOT NULL,
    name VARCHAR(256) NOT NULL,
    default_branch VARCHAR(120),
    url VARCHAR(512),
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_repository_external_id UNIQUE (tenant_id, source_system, external_id)
);

CREATE TABLE commit_metadata (
    hash VARCHAR(80) PRIMARY KEY,
    repository_id UUID NOT NULL REFERENCES repositories(id),
    author_name VARCHAR(200),
    author_email VARCHAR(320),
    message TEXT,
    committed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE commit_changed_files (
    commit_hash VARCHAR(80) NOT NULL REFERENCES commit_metadata(hash) ON DELETE CASCADE,
    file_path VARCHAR(1024) NOT NULL
);

CREATE TABLE pull_requests (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL REFERENCES repositories(id),
    number INTEGER NOT NULL,
    title VARCHAR(512) NOT NULL,
    status VARCHAR(80),
    created_at TIMESTAMPTZ,
    merged_at TIMESTAMPTZ,
    CONSTRAINT uk_pull_request_repo_number UNIQUE (repository_id, number)
);

CREATE TABLE releases (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL REFERENCES repositories(id),
    version VARCHAR(160) NOT NULL,
    tag_name VARCHAR(160),
    published_at TIMESTAMPTZ,
    CONSTRAINT uk_release_repo_version UNIQUE (repository_id, version)
);

CREATE TABLE jira_issues (
    issue_key VARCHAR(80) PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    project_key VARCHAR(80) NOT NULL,
    summary VARCHAR(512) NOT NULL,
    issue_type VARCHAR(80),
    status VARCHAR(120),
    assignee VARCHAR(200),
    fix_version VARCHAR(160),
    labels VARCHAR(512),
    components VARCHAR(512),
    updated_at TIMESTAMPTZ
);

CREATE TABLE confluence_documents (
    page_id VARCHAR(120) PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    title VARCHAR(512) NOT NULL,
    document_type VARCHAR(80),
    version INTEGER NOT NULL,
    author VARCHAR(200),
    last_modified_at TIMESTAMPTZ,
    embedding_status VARCHAR(80)
);

CREATE TABLE deployments (
    id UUID PRIMARY KEY,
    repository_id UUID REFERENCES repositories(id),
    release_version VARCHAR(160),
    environment VARCHAR(120) NOT NULL,
    build_number VARCHAR(120),
    build_status VARCHAR(80),
    build_duration_ms BIGINT,
    commit_range VARCHAR(512),
    jira_versions VARCHAR(512),
    deployed_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_deployment_build_environment ON deployments(build_number, environment);

CREATE TABLE engineering_events (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    source_system VARCHAR(40) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    subject_type VARCHAR(120) NOT NULL,
    subject_id VARCHAR(256) NOT NULL,
    title VARCHAR(512) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    metadata_json TEXT
);

CREATE TABLE engineering_relationships (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    source_type VARCHAR(120) NOT NULL,
    source_id VARCHAR(256) NOT NULL,
    relationship_type VARCHAR(120) NOT NULL,
    target_type VARCHAR(120) NOT NULL,
    target_id VARCHAR(256) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_relationship_edge UNIQUE (tenant_id, source_type, source_id, relationship_type, target_type, target_id)
);

CREATE INDEX idx_commit_repository ON commit_metadata(repository_id);
CREATE INDEX idx_event_timeline ON engineering_events(tenant_id, occurred_at DESC);
CREATE INDEX idx_relationship_source ON engineering_relationships(source_type, source_id);
CREATE INDEX idx_relationship_target ON engineering_relationships(target_type, target_id);
CREATE INDEX idx_deployment_commit_range ON deployments(commit_range);

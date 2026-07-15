-- V1: extensions + core project entities.
CREATE EXTENSION IF NOT EXISTS vector;      -- pgvector: BSG + code embeddings
CREATE EXTENSION IF NOT EXISTS pg_trgm;     -- fuzzy code/name search
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS organizations (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name         TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS migration_projects (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id                UUID REFERENCES organizations(id) ON DELETE CASCADE,
    name                  TEXT NOT NULL,
    source_language       TEXT,                       -- e.g. JAVA_8
    target_stack          TEXT,                       -- e.g. JAVA_21_SPRING_BOOT
    migration_type        TEXT NOT NULL DEFAULT 'CODE_MIGRATION',
    status                TEXT NOT NULL DEFAULT 'CREATED',
    assessment_report_json JSONB,
    output_repo_url       TEXT,
    budget_usd            NUMERIC(10,2) NOT NULL DEFAULT 25,
    spent_usd             NUMERIC(10,4) NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_projects_org ON migration_projects(org_id);

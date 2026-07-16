-- V4: usage metering — the cost-accounting layer behind budgets + invoices.
-- One row per metered LLM call, attributed to a tenant + project.

CREATE TABLE IF NOT EXISTS usage_events (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id         UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id     UUID NOT NULL REFERENCES migration_projects(id) ON DELETE CASCADE,
    model          TEXT NOT NULL,
    input_tokens   BIGINT NOT NULL DEFAULT 0,
    output_tokens  BIGINT NOT NULL DEFAULT 0,
    cost_usd       NUMERIC(12,6) NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_usage_org     ON usage_events(org_id);
CREATE INDEX IF NOT EXISTS idx_usage_project ON usage_events(project_id);

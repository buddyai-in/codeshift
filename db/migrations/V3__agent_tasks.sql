-- V3: agent task ledger — one row per agent invocation, with cost accounting.
-- Feeds the analytics/agent-costs surface and the per-project budget enforcement.

CREATE TABLE IF NOT EXISTS agent_tasks (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id           UUID NOT NULL REFERENCES migration_projects(id) ON DELETE CASCADE,
    agent_role           TEXT NOT NULL,          -- DISCOVERY | ANALYSIS | TRANSFORMATION | ...
    module_id            TEXT,
    status               TEXT NOT NULL DEFAULT 'RUNNING',
    retry_count          INT NOT NULL DEFAULT 0,
    triggered_by_task_id UUID REFERENCES agent_tasks(id),
    model                TEXT,                   -- resolved "provider:model" actually used
    input_tokens         INT NOT NULL DEFAULT 0,
    output_tokens        INT NOT NULL DEFAULT 0,
    cost_usd             NUMERIC(10,6) NOT NULL DEFAULT 0,
    failure_context      JSONB,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_agent_tasks_project ON agent_tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_agent_tasks_role    ON agent_tasks(agent_role);

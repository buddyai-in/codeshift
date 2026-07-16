-- V7: per-tenant model deployment (cloud / on-prem / in-VPC endpoint + model).
-- The BYOK API key lives encrypted in tenant_secrets, not here.

CREATE TABLE IF NOT EXISTS tenant_model_deployments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    deployment_type TEXT NOT NULL DEFAULT 'CLOUD',   -- CLOUD | ON_PREM | IN_VPC
    provider        TEXT NOT NULL,
    base_url        TEXT,                             -- required for ON_PREM / IN_VPC
    model           TEXT NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

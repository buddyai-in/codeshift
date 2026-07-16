-- V6: per-tenant encrypted secret vault (BYOK model keys, etc.).
-- Only ciphertext is stored; plaintext never touches the database.

CREATE TABLE IF NOT EXISTS tenant_secrets (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id      UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    ciphertext  TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, name)
);

CREATE INDEX IF NOT EXISTS idx_tenant_secrets_org ON tenant_secrets(org_id);

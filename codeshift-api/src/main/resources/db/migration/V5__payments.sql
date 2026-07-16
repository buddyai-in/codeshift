-- V5: payments — invoice checkout intents advanced to PAID/FAILED by the provider webhook.

CREATE TABLE IF NOT EXISTS payments (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id      UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    reference   TEXT NOT NULL UNIQUE,
    provider    TEXT NOT NULL,
    status      TEXT NOT NULL,
    amount_usd  NUMERIC(12,4) NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payments_org ON payments(org_id);

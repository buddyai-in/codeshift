-- V2: the Behavioral Specification Graph — the platform's trust boundary.
-- Versioned snapshots; nodes carry the full product-doc contract; edges form
-- the rule graph; pgvector column enables semantic retrieval over rules.

CREATE TABLE IF NOT EXISTS bsg_versions (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id     UUID NOT NULL REFERENCES migration_projects(id) ON DELETE CASCADE,
    version_number INT  NOT NULL,
    is_approved    BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by    TEXT,
    approved_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (project_id, version_number)
);

CREATE TABLE IF NOT EXISTS bsg_nodes (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version_id          UUID NOT NULL REFERENCES bsg_versions(id) ON DELETE CASCADE,
    node_ref            TEXT NOT NULL,                 -- e.g. BSG-042
    node_type           TEXT NOT NULL,                 -- BusinessRule | DataFlow | ...
    title               TEXT NOT NULL,
    description         TEXT NOT NULL,                 -- plain English (analyst-readable)
    source_location     TEXT,
    confidence          TEXT NOT NULL DEFAULT 'MEDIUM',-- HIGH | MEDIUM | LOW
    human_status        TEXT NOT NULL DEFAULT 'PENDING',-- PENDING|APPROVED|REJECTED|MODIFIED
    origin              TEXT NOT NULL DEFAULT 'MIGRATED',-- MIGRATED|NEW_FEATURE|INTEGRATION|REFACTORED
    target_code_location TEXT,
    test_coverage       BOOLEAN NOT NULL DEFAULT FALSE,
    embedding           vector(1536),                 -- for semantic_search / debt fingerprint
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (version_id, node_ref)
);

CREATE TABLE IF NOT EXISTS bsg_edges (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version_id     UUID NOT NULL REFERENCES bsg_versions(id) ON DELETE CASCADE,
    source_node_id UUID NOT NULL REFERENCES bsg_nodes(id) ON DELETE CASCADE,
    target_node_id UUID NOT NULL REFERENCES bsg_nodes(id) ON DELETE CASCADE,
    edge_type      TEXT NOT NULL                       -- depends_on|produces|validates|overrides|triggers
);

CREATE INDEX IF NOT EXISTS idx_bsg_nodes_version ON bsg_nodes(version_id);
CREATE INDEX IF NOT EXISTS idx_bsg_nodes_type    ON bsg_nodes(node_type);
CREATE INDEX IF NOT EXISTS idx_bsg_nodes_status  ON bsg_nodes(human_status);
CREATE INDEX IF NOT EXISTS idx_bsg_edges_version ON bsg_edges(version_id);
-- Approximate-NN index for retrieval (built lazily; safe to create empty).
CREATE INDEX IF NOT EXISTS idx_bsg_nodes_embedding
    ON bsg_nodes USING hnsw (embedding vector_cosine_ops);

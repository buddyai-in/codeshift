# Database migrations

PostgreSQL 16 + `pgvector`. Files are applied in filename order.

- `V1__init.sql` — extensions, organizations, `migration_projects` (with budget columns).
- `V2__bsg.sql` — the Behavioral Specification Graph: `bsg_versions`, `bsg_nodes`
  (with a `vector(1536)` embedding + HNSW index), `bsg_edges`.
- `V3__agent_tasks.sql` — per-invocation agent ledger with token/cost accounting.

## Applying

**Local dev (automatic):** `docker compose up -d` mounts this directory into the
Postgres container's init path, so a fresh volume applies V1–V3 on first boot.

**Manually / against an existing DB:**

```bash
for f in db/migrations/V*.sql; do psql "$DATABASE_URL" -f "$f"; done
```

> Phase 0 uses raw SQL files to keep the spine dependency-light. A managed
> migration tool (Alembic or Flyway) is introduced in Phase 1 when the schema
> starts changing per-feature.

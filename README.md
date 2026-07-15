# CodeShift

AI-powered legacy application modernisation platform — a coordinated pipeline of
specialist AI agents that migrates legacy codebases to modern stacks, gated by a
human-reviewable **Behavioral Specification Graph (BSG)**.

- **Architecture & plan:** [`docs/architecture/`](docs/architecture/) — the fully
  agentic, LLM-agnostic design (LangChain + LangGraph + MCP) and the phased build.
- **This repo** currently contains the **Phase 0 spine**: the smallest end-to-end
  skeleton that runs an agent graph, calls any LLM, persists state durably,
  streams to a UI, and is traced.

## What's in the spine

| Component | Path | What it does |
| --- | --- | --- |
| Model gateway | `packages/model_gateway` | LLM-agnostic `init_chat_model` + capability **profiles** (reasoning/codegen/cheap/embed) + retries + cross-provider **fallbacks** + typed structured output + **cost accounting** |
| BSG | `packages/bsg` | Typed Pydantic BSG schemas + async Postgres store |
| Graph | `packages/graph` | LangGraph spine: `discovery → bsg_review (interrupt) → finalize`, durable checkpointer, Kahn topological sort |
| Business API | `apps/business_api` | FastAPI: start runs, **SSE live agent-log stream**, resume at human gate |
| BSG MCP server | `mcp/bsg_mcp` | Exposes the BSG store as typed **MCP** tools (the pattern for every capability) |
| Common | `packages/common` | Config (`.env`), shared enums, telemetry (LangSmith) |
| DB | `db/migrations` | Postgres 16 + pgvector schema (BSG + agent-task cost ledger) |

## Quick start

```bash
# 1. Install the whole workspace (uv)
uv sync

# 2. (optional) start Postgres + Redis for durable, resumable runs
docker compose up -d          # or leave DB off to use the in-memory checkpointer
cp .env.example .env          # add provider API keys if you want live LLM calls

# 3. Prove the spine: run the graph until it interrupts, then resume it
uv run python -m codeshift_graph.demo

# 4. Run the API + live stream
uv run uvicorn codeshift_business_api.main:app --reload --port 8000
#   POST /runs                      -> starts a run, returns the human-gate payload
#   POST /runs/{thread_id}/resume   -> resume with {"decision":"APPROVED"}
#   GET  /runs/{thread_id}/stream   -> SSE live agent logs

# 5. Tests / lint / types
uv run pytest -q
uv run ruff check .
uv run mypy packages apps mcp
```

The spine **runs with zero API keys and zero infrastructure** (in-memory
checkpointer, deterministic discovery). Add `DATABASE_URL` to make runs durable;
add provider keys to enable live model calls.

## How this maps to the design

- **The graph is the process** — the pipeline, its human gates and (later) its
  feedback loops are a LangGraph `StateGraph`, not imperative glue.
- **LLM-agnostic by construction** — every model call goes through the gateway;
  providers are chosen by config (`CODESHIFT_PROFILE_*`), never hard-coded.
- **The BSG is a typed, versioned store** — never a prompt. Agents reach it
  through `bsg-mcp`.
- **Everything is resumable** — the durable checkpointer makes crashes and
  multi-day human reviews non-events.

See [`docs/architecture/implementation-plan.md`](docs/architecture/implementation-plan.md)
for how the next phases (Discovery→Assessment, Analysis/BSG, Transformation,
Validation, …) clip onto this spine.

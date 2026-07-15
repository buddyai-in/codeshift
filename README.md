# CodeShift

AI-powered legacy application modernisation platform — a coordinated pipeline of
specialist AI agents that migrates legacy codebases to modern stacks, gated by a
human-reviewable **Behavioral Specification Graph (BSG)**.

- **Architecture & plan:** [`docs/architecture/`](docs/architecture/) — the fully
  agentic, LLM-agnostic design and the phased build.
- **This repo** currently contains the **Phase 0 spine**: the smallest end-to-end
  skeleton that runs an agent graph, can call any LLM, persists state, streams to
  a UI, and is fully buildable — built on **Java 21 + Spring Boot**.

## Stack

Java-native agentic stack (chosen for a Spring Boot team; matches the product doc):

| Concern | Library |
| --- | --- |
| LLM-agnostic model gateway | **Spring AI** `ChatClient` / `ChatModel` (Anthropic, OpenAI, Bedrock, Vertex/Gemini, Azure, Mistral, Ollama — swap by config) |
| Agent orchestration (graph, human-in-the-loop, checkpointer) | **langgraph4j** (JVM port of LangGraph) |
| Capability plane (BSG/parsers/sandbox as tools) | **MCP Java SDK** via Spring AI MCP server |
| API + live streaming | **Spring Boot** (Web MVC + SSE) |
| Persistence | **PostgreSQL 16 + pgvector**, Spring Data JPA, Flyway |

## Modules (Maven multi-module)

| Module | Responsibility |
| --- | --- |
| `codeshift-common` | Shared enums / vocabulary + Kahn topological sort. No business logic. |
| `codeshift-model-gateway` | LLM-agnostic gateway: capability **profiles** (reasoning/codegen/cheap/embed) → `provider:model`, over Spring AI. Cost estimation. |
| `codeshift-bsg` | Typed BSG model (records) + JPA store. The trust boundary. |
| `codeshift-parser` | **JavaParser** analysis (Discovery): module inventory, dependency graph, messaging detection, `javax.*` migration signal. Deterministic. |
| `codeshift-assessment` | Free assessment report generator (effort + `$50/kLOC` price estimate + migration signals). The top-of-funnel lead magnet. |
| `codeshift-agents` | The intelligence layer. **Analysis Agent** (BSG builder): extracts typed, confidence-scored `BsgNode`s via the Spring AI gateway (`REASONING`), with a deterministic skeleton fallback when no LLM key is set. |
| `codeshift-graph` | **langgraph4j** spine: `discovery → analysis → review (interrupt) → finalize`. Discovery parses a real project; analysis builds the BSG; the gate approves it. Framework-agnostic. |
| `codeshift-bsg-mcp` | Spring Boot **MCP server** (stdio) exposing the BSG store as typed tools. |
| `codeshift-java-parser-mcp` | Spring Boot **MCP server** (stdio) exposing JavaParser analysis + assessment as tools. No DB. |
| `codeshift-api` | The Spring Boot app: **free `/public/assess`** endpoint (zip upload → report), run lifecycle + **SSE stream** + resume-at-gate. Flyway owns the schema. Boots DB-less with `--spring.profiles.active=nodb` for the assessment funnel. |
| `codeshift-web` | **React 18 + Vite** UI: upload a codebase → stat cards, migration signals, price, and an interactive **react-flow dependency graph**. |

## Demo the free assessment (no infrastructure)

The assessment funnel needs no database and no API keys:

```bash
# Terminal 1 — backend, DB-less profile
mvn -q -pl codeshift-api -am package -DskipTests
java -jar codeshift-api/target/codeshift-api-0.1.0-SNAPSHOT.jar --spring.profiles.active=nodb

# Terminal 2 — frontend
cd codeshift-web && npm install && npm run dev   # http://localhost:5173
```

Open the UI and click **"Try the sample project"** — you get stat cards, migration
signals, a price estimate, and an interactive dependency graph. Or hit the API directly:

```bash
curl -s -XPOST localhost:8080/public/assess -F file=@your-sources.zip | jq .report
```

## Quick start

```bash
# 1. Build + run all unit tests (no DB or API keys needed)
mvn verify

# 2. Full local stack: Postgres + Redis + API + web
docker compose up --build

#    Host ports default to 15432 (Postgres) and 16379 (Redis) to avoid clashes.
#    Override with POSTGRES_PORT / REDIS_PORT if you want different bindings.

# 3. Run the control-plane API
mvn -pl codeshift-api spring-boot:run
#   POST /public/assess           (multipart: file=<source.zip>)  -> FREE assessment report (no auth)
#   POST /public/assess/path      {"projectPath":"...","projectName":"..."} -> assess a server dir
#   POST /runs                    {"projectId":"demo","projectPath":"/path/to/src"}  -> parse + build BSG, then gate
#   GET  /runs/{threadId}/bsg                               -> the BSG awaiting review
#   POST /runs/{threadId}/resume  {"decision":"APPROVED"}   -> approve the BSG at the human gate
#   GET  /runs/stream?projectId=demo                        -> SSE live per-node progress
#   GET  /health

# Try the free assessment against the bundled sample project (no DB needed):
#   curl -s -XPOST localhost:8080/public/assess/path -H 'Content-Type: application/json' \
#     -d '{"projectPath":"codeshift-parser/src/test/resources/sample-project","projectName":"sample"}'

# 4. Run the BSG MCP server (stdio; needs Postgres)
mvn -pl codeshift-bsg-mcp spring-boot:run
```

The spine **runs with zero API keys**. The graph uses an in-memory checkpointer,
Discovery is deterministic, and the Analysis Agent falls back to a BSG skeleton —
so `mvn verify` and the whole `discovery → analysis → gate` pipeline are green with
no infrastructure. Add `DATABASE_URL` (Postgres) to persist the BSG.

### Enable real LLM analysis

The Analysis Agent is LLM-agnostic. To have it extract real business rules instead
of the skeleton, add a Spring AI provider starter to `codeshift-api/pom.xml` and set
its key — no code change (the gateway resolves the `REASONING` profile from
`application.yml`):

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-model-anthropic</artifactId>
</dependency>
```
```bash
export ANTHROPIC_API_KEY=...    # then run the API as usual
```

## How this maps to the design

- **The graph is the process** — the pipeline, its human gates and (later) its
  feedback loops are a langgraph4j `StateGraph`, not imperative glue.
- **LLM-agnostic by construction** — every model call goes through the gateway;
  providers are chosen by `codeshift.model.profiles.*` config, never hard-coded.
- **The BSG is a typed, versioned store** — never a prompt. Reached through
  `BsgStore` and, over MCP, the `codeshift-bsg-mcp` server.
- **Resumable** — the checkpointer makes the human-review gate a durable pause;
  a Postgres checkpointer is the Phase 1 swap.

See [`docs/architecture/implementation-plan.md`](docs/architecture/implementation-plan.md)
for how the next phases (Discovery→Assessment, Analysis/BSG, Transformation,
Validation, …) clip onto this spine.

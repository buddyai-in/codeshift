# CodeShift — Agentic Architecture

This directory contains a **fully agentic, LLM‑agnostic architecture design** for
the CodeShift legacy‑modernisation platform, re‑expressing the 8‑agent pipeline
from the product document on **LangChain + LangGraph** with a pluggable **MCP**
capability plane.

## Documents

| Doc | What's in it |
| --- | --- |
| [`agentic-architecture.md`](./agentic-architecture.md) | Full design: goals, 8‑plane architecture, the LangGraph master graph, the LLM‑agnostic model gateway, MCP tool servers, the BSG store, durable HITL, streaming, observability/evals, security, deployment, ADRs, and traceability back to the product doc. |
| [`implementation-plan.md`](./implementation-plan.md) | Phased build (P0 spine → P6 GA), repo layout, engineering practices, migration path from the v1 LangChain4j plan, and a first‑two‑weeks checklist. Keeps the product doc's revenue milestones. |

## The idea in 30 seconds

- **The pipeline is a graph.** The product's "sequential pipeline with feedback
  loops and human review gates" maps 1:1 onto a **LangGraph `StateGraph`** —
  cyclic edges for the Validation→Transformation retry, `interrupt()` for the
  BSG/architecture human gates, fan‑out/fan‑in for Test‑Gen ‖ Transformation, and
  a **durable Postgres checkpointer** so runs survive crashes and multi‑day pauses.
- **The BSG is typed, versioned state — never a prompt.** It lives in Postgres +
  pgvector behind a `bsg-mcp` server; agents retrieve relevant slices, so the
  system scales to 100k+ LOC and stays inside the $/kLOC cost envelope.
- **LLM‑agnostic by construction.** Every call goes through a LangChain
  `init_chat_model` gateway with per‑role **model profiles**, fallback chains and
  BYOK — Anthropic / OpenAI / Gemini / Bedrock / Azure / Mistral / local, chosen
  by config, certified by an eval suite.
- **Capabilities are plugins.** Parsers (JVM/.NET/Node/PHP/COBOL), the sandbox,
  git, security and DevOps generators are **MCP servers** — adding a language is a
  new tool server, not an orchestrator change, and the founder's Java parser
  expertise is reused as‑is.

See the two documents above for the detail, diagrams, and phased plan.

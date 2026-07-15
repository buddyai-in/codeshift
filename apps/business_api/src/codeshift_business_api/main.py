"""FastAPI app: start migration runs, stream live agent logs, resume at gates.

This is the browser-facing bridge from LangGraph's async event stream to
Server-Sent Events — the mechanism behind the product's "live WebSocket
progress / live agent logs" requirement. A single compiled graph + checkpointer
is shared across requests via the lifespan context.
"""

from __future__ import annotations

import json
import uuid
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from codeshift_common.telemetry import init_telemetry
from codeshift_graph.builder import build_migration_graph
from codeshift_graph.checkpointer import checkpointer_cm
from fastapi import FastAPI, HTTPException
from langgraph.types import Command
from pydantic import BaseModel
from sse_starlette.sse import EventSourceResponse


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    init_telemetry()
    # Hold the checkpointer open for the app's lifetime; compile once.
    async with checkpointer_cm() as saver:
        app.state.graph = build_migration_graph(checkpointer=saver)
        yield


app = FastAPI(title="CodeShift API", version="0.1.0", lifespan=lifespan)


class StartRunRequest(BaseModel):
    project_id: str
    module_inventory: list[str] | None = None
    budget_usd: float = 25.0


class ResumeRequest(BaseModel):
    decision: str  # e.g. "APPROVED" | "REJECTED"


@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@app.post("/runs")
async def start_run(req: StartRunRequest) -> dict:
    """Start a run; returns the thread id and whatever the run interrupted on."""
    thread_id = str(uuid.uuid4())
    config = {"configurable": {"thread_id": thread_id}}
    state: dict = {
        "project_id": req.project_id,
        "budget": {"limit_usd": req.budget_usd, "spent_usd": 0.0},
    }
    if req.module_inventory:
        state["module_inventory"] = req.module_inventory

    result = await app.state.graph.ainvoke(state, config=config)
    interrupt_payload = None
    if result.get("__interrupt__"):
        interrupt_payload = result["__interrupt__"][0].value
    return {
        "thread_id": thread_id,
        "phase": result.get("phase"),
        "awaiting_human": interrupt_payload is not None,
        "interrupt": interrupt_payload,
        "log": result.get("log", []),
    }


@app.post("/runs/{thread_id}/resume")
async def resume_run(thread_id: str, req: ResumeRequest) -> dict:
    """Resume a suspended run at its human gate with a decision."""
    config = {"configurable": {"thread_id": thread_id}}
    snapshot = await app.state.graph.aget_state(config)
    if snapshot is None or not snapshot.created_at:
        raise HTTPException(status_code=404, detail="Unknown thread_id")

    result = await app.state.graph.ainvoke(Command(resume=req.decision), config=config)
    return {
        "thread_id": thread_id,
        "phase": result.get("phase"),
        "review_decision": result.get("review_decision"),
        "log": result.get("log", []),
    }


@app.get("/runs/{thread_id}/stream")
async def stream_run(thread_id: str, project_id: str) -> EventSourceResponse:
    """Live agent-log stream (SSE) for a fresh run.

    Bridges LangGraph `astream(stream_mode="updates")` to SSE so the UI can render
    per-node progress in real time. `stream_mode="messages"` would add token-level
    streaming for the code-diff view in later phases.
    """
    config = {"configurable": {"thread_id": thread_id}}

    async def event_generator() -> AsyncIterator[dict]:
        async for chunk in app.state.graph.astream(
            {"project_id": project_id, "budget": {"limit_usd": 25.0, "spent_usd": 0.0}},
            config=config,
            stream_mode="updates",
        ):
            yield {"event": "update", "data": json.dumps(chunk, default=str)}
        yield {"event": "done", "data": "{}"}

    return EventSourceResponse(event_generator())

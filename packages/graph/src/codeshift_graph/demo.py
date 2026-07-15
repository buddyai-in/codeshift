"""End-to-end demo: run the graph until it interrupts, then resume it.

Run with:  uv run python -m codeshift_graph.demo

Proves the core Phase 0 property: the run suspends at the human gate and resumes
from the exact checkpoint with a supplied decision. With DATABASE_URL set the
checkpoint is durable (survives process restarts); without it, it uses memory.
"""

from __future__ import annotations

import asyncio
import uuid

from codeshift_common.telemetry import init_telemetry
from langgraph.types import Command

from codeshift_graph.builder import build_migration_graph
from codeshift_graph.checkpointer import checkpointer_cm


async def main() -> None:
    init_telemetry()
    thread_id = str(uuid.uuid4())
    config = {"configurable": {"thread_id": thread_id}}

    async with checkpointer_cm() as saver:
        app = build_migration_graph(checkpointer=saver)

        print(f"\n=== Starting run (thread={thread_id}) ===")
        result = await app.ainvoke(
            {"project_id": "demo-project", "budget": {"limit_usd": 25.0, "spent_usd": 0.0}},
            config=config,
        )

        # The run is now suspended at the interrupt. LangGraph surfaces the
        # payload under "__interrupt__".
        interrupts = result.get("__interrupt__")
        if interrupts:
            payload = interrupts[0].value
            print("\n--- INTERRUPTED at human gate ---")
            print(f"  gate: {payload.get('gate')}")
            print(f"  translation order: {payload.get('translation_order')}")
            print(f"  question: {payload.get('question')}")

        # ... hours/days later, a human approves. Resume from the checkpoint.
        print("\n=== Resuming with decision=APPROVED ===")
        final = await app.ainvoke(Command(resume="APPROVED"), config=config)

        print("\n--- FINAL STATE ---")
        print(f"  phase: {final.get('phase')}")
        for line in final.get("log", []):
            print(f"  log: {line}")


if __name__ == "__main__":
    asyncio.run(main())

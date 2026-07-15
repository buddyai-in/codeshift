"""The orchestration spine: topological order + durable interrupt/resume."""

import uuid

import pytest
from codeshift_common.types import Phase
from codeshift_graph.builder import build_migration_graph
from codeshift_graph.nodes import _kahn_topological_order
from langgraph.checkpoint.memory import MemorySaver
from langgraph.types import Command


def test_kahn_leaf_first_order():
    modules = ["A", "B", "C"]
    edges = [("A", "B"), ("B", "C")]  # A depends_on B depends_on C
    order = _kahn_topological_order(modules, edges)
    # Leaves (no deps) come first: C before B before A.
    assert order.index("C") < order.index("B") < order.index("A")


@pytest.mark.asyncio
async def test_interrupt_then_resume():
    app = build_migration_graph(checkpointer=MemorySaver())
    config = {"configurable": {"thread_id": str(uuid.uuid4())}}

    # Run suspends at the human gate.
    result = await app.ainvoke(
        {"project_id": "t", "budget": {"limit_usd": 25.0, "spent_usd": 0.0}},
        config=config,
    )
    assert result.get("__interrupt__"), "run should pause at the BSG review gate"
    assert result["__interrupt__"][0].value["gate"] == "BSG_APPROVAL"

    # Resume from the exact checkpoint with a human decision.
    final = await app.ainvoke(Command(resume="APPROVED"), config=config)
    assert final["review_decision"] == "APPROVED"
    assert final["phase"] == Phase.ARCHITECTURE

"""Graph nodes for the Phase 0 spine.

Three nodes prove the whole orchestration pattern end-to-end:

  discovery ──▶ bsg_review (interrupt / HITL gate) ──▶ finalize

- `discovery` is deterministic (topological sort via Kahn's algorithm) with an
  optional LLM summary — so the spine runs with zero API keys.
- `bsg_review` calls `interrupt()` — the run durably suspends here until a human
  resumes it with a decision. This is CodeShift's core "trust boundary" gate.
- `finalize` records the outcome.

Every later agent (Analysis, Architecture, Transformation, ...) slots in as more
nodes on this same graph — the pattern does not change.
"""

from __future__ import annotations

from codeshift_common.types import Phase
from langgraph.types import interrupt

from codeshift_graph.state import MigrationState


def _kahn_topological_order(modules: list[str], edges: list[tuple[str, str]]) -> list[str]:
    """Leaf-first order so a module's dependencies are always processed first.

    Eliminates ~30% of hallucinated-type failures (product doc §5.1) by giving
    the Transformation Agent already-translated upstream modules as context.
    """
    indegree = {m: 0 for m in modules}
    adj: dict[str, list[str]] = {m: [] for m in modules}
    for src, dst in edges:  # edge src depends_on dst
        adj.setdefault(dst, []).append(src)
        indegree[src] = indegree.get(src, 0) + 1
        indegree.setdefault(dst, 0)

    queue = [m for m in modules if indegree.get(m, 0) == 0]
    order: list[str] = []
    while queue:
        node = queue.pop(0)
        order.append(node)
        for nxt in adj.get(node, []):
            indegree[nxt] -= 1
            if indegree[nxt] == 0:
                queue.append(nxt)
    # Any leftover (cycle) appended deterministically so we never drop modules.
    order.extend(m for m in modules if m not in order)
    return order


async def discovery_node(state: MigrationState) -> dict:
    """Scan the codebase without modifying it; produce inventory + translation order.

    Phase 0 uses a tiny built-in sample so the spine runs offline. Phase 1 swaps
    the sample for real output from `java-parser-mcp`.
    """
    modules = state.get("module_inventory") or [
        "OrderController",
        "OrderService",
        "OrderRepository",
        "PricingRule",
    ]
    edges = state.get("dependency_edges") or [
        ("OrderController", "OrderService"),
        ("OrderService", "OrderRepository"),
        ("OrderService", "PricingRule"),
    ]
    order = _kahn_topological_order(modules, edges)
    return {
        # Store the enum's string value so checkpoints stay msgpack-clean
        # (Phase is a str-Enum, so equality with the enum member still holds).
        "phase": Phase.BSG_REVIEW.value,
        "module_inventory": modules,
        "dependency_edges": edges,
        "topo_order": order,
        "log": [f"discovery: {len(modules)} modules, translation order = {order}"],
    }


async def bsg_review_node(state: MigrationState) -> dict:
    """Durable human-in-the-loop gate (product doc §4.2, §5).

    `interrupt()` suspends the run and surfaces a review payload to the caller.
    The run resumes only when a human sends a decision via `Command(resume=...)`,
    even if that is days later and on a different worker.
    """
    decision = interrupt(
        {
            "gate": "BSG_APPROVAL",
            "project_id": state.get("project_id"),
            "translation_order": state.get("topo_order", []),
            "question": "Approve the discovered module inventory / translation order?",
        }
    )
    return {
        "review_decision": str(decision),
        "log": [f"bsg_review: human decision = {decision!r}"],
    }


async def finalize_node(state: MigrationState) -> dict:
    decision = state.get("review_decision")
    phase = Phase.ARCHITECTURE if decision == "APPROVED" else Phase.DISCOVERY
    return {
        "phase": phase.value,
        "log": [f"finalize: routing to {phase.value} after decision {decision!r}"],
    }

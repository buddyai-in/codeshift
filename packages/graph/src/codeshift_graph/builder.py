"""Assemble the migration graph.

The graph *is* the process: nodes are agents, edges are control flow, the
interrupt in `bsg_review` is a human gate, and the checkpointer makes the whole
thing durable. New pillars/agents are new nodes on this same StateGraph.
"""

from __future__ import annotations

from langgraph.checkpoint.base import BaseCheckpointSaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.state import CompiledStateGraph

from codeshift_graph.nodes import bsg_review_node, discovery_node, finalize_node
from codeshift_graph.state import MigrationState


def build_migration_graph(
    checkpointer: BaseCheckpointSaver | None = None,
) -> CompiledStateGraph:
    """Compile the Phase 0 spine graph.

    discovery ─▶ bsg_review (interrupt) ─▶ finalize ─▶ END
    """
    graph = StateGraph(MigrationState)
    graph.add_node("discovery", discovery_node)
    graph.add_node("bsg_review", bsg_review_node)
    graph.add_node("finalize", finalize_node)

    graph.add_edge(START, "discovery")
    graph.add_edge("discovery", "bsg_review")
    graph.add_edge("bsg_review", "finalize")
    graph.add_edge("finalize", END)

    return graph.compile(checkpointer=checkpointer)

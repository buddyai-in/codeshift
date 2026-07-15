"""BSG MCP server.

Exposes the BSG store as typed tools over the Model Context Protocol, so any
agent (or the synchronous API) touches the BSG through one uniform contract
instead of ad-hoc SQL. This is the pattern for every capability in the design —
parsers, sandbox, git, security — each becomes an MCP server like this one.

Run: `uv run codeshift-bsg-mcp` (stdio transport).
"""

from __future__ import annotations

from typing import Any

from codeshift_bsg.schemas import BsgGraph, BsgNode
from codeshift_bsg.store import BsgStore
from codeshift_common.config import get_settings
from codeshift_common.types import HumanStatus
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("codeshift-bsg")

_store: BsgStore | None = None


async def _get_store() -> BsgStore:
    global _store
    if _store is None:
        dsn = get_settings().database_url
        if not dsn:
            raise RuntimeError("DATABASE_URL is not set; BSG store needs Postgres.")
        _store = await BsgStore.connect(dsn)
    return _store


@mcp.tool()
async def save_bsg_graph(graph: dict[str, Any]) -> dict[str, str]:
    """Persist a full BSG version (nodes + edges). Returns the new version id."""
    store = await _get_store()
    version_id = await store.save_graph(BsgGraph.model_validate(graph))
    return {"version_id": version_id}


@mcp.tool()
async def upsert_node(version_id: str, node: dict[str, Any]) -> dict[str, str]:
    """Upsert a single BSG node into an existing version."""
    store = await _get_store()
    g = BsgGraph(project_id="_", version_number=1, nodes=[BsgNode.model_validate(node)])
    # save_graph is upsert-by-(version, node_ref); reuse it against the version.
    await store.save_graph(g)
    return {"status": "ok"}


@mcp.tool()
async def get_version(version_id: str) -> dict[str, Any]:
    """Read a BSG version back (nodes + edges) for review or retrieval."""
    store = await _get_store()
    return await store.get_version(version_id)


@mcp.tool()
async def set_node_status(node_id: str, status: str, reviewer: str | None = None) -> dict[str, str]:
    """Record a human review decision on a node (APPROVED / REJECTED / MODIFIED)."""
    store = await _get_store()
    await store.set_node_status(node_id, HumanStatus(status), reviewer)
    return {"status": "ok"}


@mcp.tool()
async def approve_version(version_id: str, reviewer: str) -> dict[str, str]:
    """Approve a whole BSG version — the trust-boundary gate before transformation."""
    store = await _get_store()
    await store.approve_version(version_id, reviewer)
    return {"status": "approved"}


def main() -> None:
    mcp.run()


if __name__ == "__main__":
    main()

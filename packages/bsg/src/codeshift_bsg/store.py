"""Async Postgres-backed BSG store.

Kept deliberately small for Phase 0: create a version, upsert nodes/edges, read a
version back, and update a node's human review status. This is the concrete
implementation the `bsg-mcp` server exposes as tools and the graph writes through.

Semantic search (pgvector) and version diffing land in Phase 1/2 alongside the
Analysis Agent; the schema (V2) already has the embedding column and HNSW index.
"""

from __future__ import annotations

import json
from typing import Any

import asyncpg
from codeshift_common.types import HumanStatus

from codeshift_bsg.schemas import BsgGraph, BsgNode


class BsgStore:
    def __init__(self, pool: asyncpg.Pool) -> None:
        self._pool = pool

    @classmethod
    async def connect(cls, dsn: str) -> BsgStore:
        pool = await asyncpg.create_pool(dsn, min_size=1, max_size=5)
        assert pool is not None
        return cls(pool)

    async def close(self) -> None:
        await self._pool.close()

    async def create_version(self, project_id: str, version_number: int) -> str:
        row = await self._pool.fetchrow(
            """
            INSERT INTO bsg_versions (project_id, version_number)
            VALUES ($1, $2)
            ON CONFLICT (project_id, version_number)
            DO UPDATE SET version_number = EXCLUDED.version_number
            RETURNING id
            """,
            project_id,
            version_number,
        )
        return str(row["id"])

    async def save_graph(self, graph: BsgGraph) -> str:
        """Persist a full BSG version (nodes + edges) transactionally."""
        async with self._pool.acquire() as conn:
            async with conn.transaction():
                version_id = await conn.fetchval(
                    """
                    INSERT INTO bsg_versions (project_id, version_number)
                    VALUES ($1, $2)
                    ON CONFLICT (project_id, version_number)
                    DO UPDATE SET version_number = EXCLUDED.version_number
                    RETURNING id
                    """,
                    graph.project_id,
                    graph.version_number,
                )
                ref_to_id: dict[str, str] = {}
                for node in graph.nodes:
                    node_id = await conn.fetchval(
                        """
                        INSERT INTO bsg_nodes
                          (version_id, node_ref, node_type, title, description,
                           source_location, confidence, human_status, origin,
                           target_code_location, test_coverage)
                        VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)
                        ON CONFLICT (version_id, node_ref) DO UPDATE SET
                          title = EXCLUDED.title,
                          description = EXCLUDED.description,
                          confidence = EXCLUDED.confidence,
                          human_status = EXCLUDED.human_status
                        RETURNING id
                        """,
                        version_id,
                        node.node_ref,
                        node.node_type.value,
                        node.title,
                        node.description,
                        node.source_location,
                        node.confidence.value,
                        node.human_status.value,
                        node.origin.value,
                        node.target_code_location,
                        node.test_coverage,
                    )
                    ref_to_id[node.node_ref] = str(node_id)

                for edge in graph.edges:
                    src, tgt = ref_to_id.get(edge.source_ref), ref_to_id.get(edge.target_ref)
                    if src and tgt:
                        await conn.execute(
                            """
                            INSERT INTO bsg_edges
                              (version_id, source_node_id, target_node_id, edge_type)
                            VALUES ($1,$2,$3,$4)
                            """,
                            version_id,
                            src,
                            tgt,
                            edge.edge_type,
                        )
                return str(version_id)

    async def get_version(self, version_id: str) -> dict[str, Any]:
        nodes = await self._pool.fetch(
            "SELECT * FROM bsg_nodes WHERE version_id = $1 ORDER BY node_ref", version_id
        )
        edges = await self._pool.fetch("SELECT * FROM bsg_edges WHERE version_id = $1", version_id)
        return {
            "version_id": version_id,
            "nodes": [dict(n) for n in nodes],
            "edges": [dict(e) for e in edges],
        }

    async def set_node_status(
        self, node_id: str, status: HumanStatus, reviewer: str | None = None
    ) -> None:
        await self._pool.execute(
            "UPDATE bsg_nodes SET human_status = $2 WHERE id = $1",
            node_id,
            status.value,
        )

    async def approve_version(self, version_id: str, reviewer: str) -> None:
        await self._pool.execute(
            """
            UPDATE bsg_versions SET is_approved = TRUE, approved_by = $2, approved_at = now()
            WHERE id = $1
            """,
            version_id,
            reviewer,
        )


def node_from_row(row: dict[str, Any]) -> BsgNode:
    """Rehydrate a BsgNode from a DB row (used by the MCP server)."""
    return BsgNode.model_validate({k: v for k, v in row.items() if k in BsgNode.model_fields})


def _json(value: Any) -> str:  # noqa: ANN401 - small helper
    return json.dumps(value, default=str)

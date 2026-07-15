"""Shared graph state.

Kept small and serialisable: it holds *references* (project id, BSG version id,
topological order, cursors, budget) — never heavy payloads. Large artifacts (BSG
nodes, code, test results) live in Postgres/S3 and are fetched on demand. This is
what lets a run scale to 100k+ LOC without blowing up the checkpoint size.
"""

from __future__ import annotations

from operator import add
from typing import Annotated, TypedDict


class TokenBudget(TypedDict):
    limit_usd: float
    spent_usd: float


class MigrationState(TypedDict, total=False):
    project_id: str
    phase: str  # a Phase value (str-Enum) — stored as plain str for clean checkpoints

    # Discovery output (references / lightweight facts, not file contents).
    module_inventory: list[str]
    topo_order: list[str]  # leaf-first (Kahn's algorithm)
    dependency_edges: list[tuple[str, str]]

    # BSG pointer + review bookkeeping.
    bsg_version_id: str | None
    open_review_items: Annotated[list[str], add]

    # Human decision captured at the review gate (filled on resume).
    review_decision: str | None  # APPROVED | REJECTED | edits...

    # Cost governance.
    budget: TokenBudget

    # Human-readable trail for the UI / demo.
    log: Annotated[list[str], add]

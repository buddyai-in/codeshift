"""Typed BSG schemas.

These Pydantic models are the *only* way business rules cross the AI boundary.
The Analysis Agent emits `BsgNode` objects via the gateway's structured output —
never free text — so every rule is validatable, human-reviewable, and auditable.
"""

from __future__ import annotations

from codeshift_common.types import BsgConfidence, BsgNodeType, BsgOrigin, HumanStatus
from pydantic import BaseModel, Field


class BsgNode(BaseModel):
    """A single behavioral specification (product doc §4.1 / §4.3)."""

    node_ref: str = Field(description="Stable reference, e.g. 'BSG-042'.")
    node_type: BsgNodeType
    title: str = Field(description="Short human-readable title.")
    description: str = Field(
        description="Plain-English description a business analyst can validate."
    )
    source_location: str | None = Field(
        default=None, description="e.g. 'OrderService.java:118-146'."
    )
    confidence: BsgConfidence = BsgConfidence.MEDIUM
    human_status: HumanStatus = HumanStatus.PENDING
    origin: BsgOrigin = BsgOrigin.MIGRATED
    target_code_location: str | None = None
    test_coverage: bool = False


class BsgEdge(BaseModel):
    """A relationship between two BSG nodes."""

    source_ref: str
    target_ref: str
    edge_type: str = Field(
        description="One of: depends_on | produces | validates | overrides | triggers."
    )


class BsgGraph(BaseModel):
    """A whole BSG version — what the Analysis Agent produces for review."""

    project_id: str
    version_number: int = 1
    nodes: list[BsgNode] = Field(default_factory=list)
    edges: list[BsgEdge] = Field(default_factory=list)

    @property
    def pending_count(self) -> int:
        return sum(1 for n in self.nodes if n.human_status == HumanStatus.PENDING)

    @property
    def low_confidence(self) -> list[BsgNode]:
        """Nodes that most need a human look (product doc: LOW -> human review)."""
        return [n for n in self.nodes if n.confidence == BsgConfidence.LOW]

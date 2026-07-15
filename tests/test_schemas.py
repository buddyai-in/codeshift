"""The BSG is a typed trust boundary."""

from codeshift_bsg.schemas import BsgGraph, BsgNode
from codeshift_common.types import BsgConfidence, BsgNodeType, HumanStatus


def test_bsg_node_defaults():
    node = BsgNode(
        node_ref="BSG-042",
        node_type=BsgNodeType.BUSINESS_RULE,
        title="Loyalty discount",
        description="Orders over $100 from gold members get 10% off.",
        confidence=BsgConfidence.LOW,
    )
    assert node.human_status == HumanStatus.PENDING
    assert node.test_coverage is False


def test_graph_review_helpers():
    g = BsgGraph(
        project_id="p1",
        nodes=[
            BsgNode(
                node_ref="BSG-001",
                node_type=BsgNodeType.BUSINESS_RULE,
                title="A",
                description="d",
                confidence=BsgConfidence.LOW,
            ),
            BsgNode(
                node_ref="BSG-002",
                node_type=BsgNodeType.DATA_FLOW,
                title="B",
                description="d",
                confidence=BsgConfidence.HIGH,
            ),
        ],
    )
    assert g.pending_count == 2
    assert [n.node_ref for n in g.low_confidence] == ["BSG-001"]

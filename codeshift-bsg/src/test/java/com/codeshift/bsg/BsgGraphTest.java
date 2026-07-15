package com.codeshift.bsg;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The BSG is a typed trust boundary. */
class BsgGraphTest {

    @Test
    void extractedNodeIsPendingAndUncovered() {
        BsgNode node = BsgNode.extracted("BSG-042", BsgNodeType.BUSINESS_RULE,
                "Loyalty discount", "Orders over $100 from gold members get 10% off.",
                "OrderService.java:118-146", BsgConfidence.LOW);
        assertThat(node.humanStatus().name()).isEqualTo("PENDING");
        assertThat(node.testCoverage()).isFalse();
    }

    @Test
    void reviewHelpers() {
        BsgGraph g = new BsgGraph("p1", 1, List.of(
                BsgNode.extracted("BSG-001", BsgNodeType.BUSINESS_RULE, "A", "d", null, BsgConfidence.LOW),
                BsgNode.extracted("BSG-002", BsgNodeType.DATA_FLOW, "B", "d", null, BsgConfidence.HIGH)
        ), List.of());
        assertThat(g.pendingCount()).isEqualTo(2);
        assertThat(g.lowConfidence()).extracting(BsgNode::nodeRef).containsExactly("BSG-001");
    }
}

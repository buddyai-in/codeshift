package com.codeshift.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.bsg.BsgProducer;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exercises the run lifecycle without a Spring context or a database — proving
 * the spine runs with zero infrastructure (in-memory checkpointer, stub agent).
 */
class GraphRuntimeTest {

    private static final BsgProducer STUB = (projectId, moduleIds, projectPath) ->
            new BsgGraph(projectId, 1, moduleIds.stream().map(id -> BsgNode.extracted(
                    "BSG-" + id, BsgNodeType.BUSINESS_RULE, "Rule " + id, "d", id,
                    BsgConfidence.MEDIUM)).toList(), List.of());

    @Test
    void startPausesAtGateWithBsgThenResumeAdvances() {
        GraphRuntime runtime = new GraphRuntime(STUB);

        GraphRuntime.StartResult started = runtime.start("demo", List.of(), null);
        assertThat(started.awaitingHuman()).isTrue();
        assertThat(started.phase()).isEqualTo("BSG_REVIEW");
        assertThat(started.translationOrder())
                .containsExactly("OrderRepository", "PricingRule", "OrderService", "OrderController");
        // The Analysis Agent ran: the BSG has a node per module and is fetchable.
        assertThat(started.bsgNodeCount()).isEqualTo(4);
        assertThat(runtime.bsgOf(started.threadId()).nodes()).hasSize(4);

        GraphRuntime.ResumeResult resumed = runtime.resume(started.threadId(), "APPROVED");
        assertThat(resumed.reviewDecision()).isEqualTo("APPROVED");
        assertThat(resumed.phase()).isEqualTo("ARCHITECTURE");
    }
}

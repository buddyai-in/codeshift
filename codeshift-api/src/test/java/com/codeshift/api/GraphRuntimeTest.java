package com.codeshift.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exercises the run lifecycle without a Spring context or a database — proving
 * the spine runs with zero infrastructure (in-memory checkpointer).
 */
class GraphRuntimeTest {

    @Test
    void startPausesAtGateThenResumeAdvances() {
        GraphRuntime runtime = new GraphRuntime();

        GraphRuntime.StartResult started = runtime.start("demo", List.of(), null);
        assertThat(started.awaitingHuman()).isTrue();
        assertThat(started.phase()).isEqualTo("BSG_REVIEW");
        assertThat(started.translationOrder())
                .containsExactly("OrderRepository", "PricingRule", "OrderService", "OrderController");

        GraphRuntime.ResumeResult resumed = runtime.resume(started.threadId(), "APPROVED");
        assertThat(resumed.reviewDecision()).isEqualTo("APPROVED");
        assertThat(resumed.phase()).isEqualTo("ARCHITECTURE");
    }
}

package com.codeshift.agents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.common.HumanStatus;
import com.codeshift.gateway.ModelGateway;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Analysis Agent's deterministic (no-LLM) skeleton path. */
class AnalysisAgentTest {

    @Test
    void skeletonWhenNoModelAvailable() {
        ModelGateway gateway = mock(ModelGateway.class);
        when(gateway.isAvailable()).thenReturn(false);
        AnalysisAgent agent = new AnalysisAgent(gateway);

        BsgGraph bsg = agent.produce("demo",
                List.of("com.acme.repo.OrderRepository", "com.acme.web.OrderController"), null);

        assertThat(bsg.nodes()).hasSize(2);
        assertThat(bsg.nodes()).allMatch(n -> n.humanStatus() == HumanStatus.PENDING);
        assertThat(bsg.pendingCount()).isEqualTo(2);
        assertThat(bsg.nodes().get(0).nodeRef()).isEqualTo("BSG-001");
        assertThat(bsg.nodes()).anyMatch(n -> n.title().contains("OrderController"));
    }
}

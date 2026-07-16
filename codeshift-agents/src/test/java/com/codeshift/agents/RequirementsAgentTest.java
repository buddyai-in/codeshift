package com.codeshift.agents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import com.codeshift.common.BsgOrigin;
import com.codeshift.common.NewCodeMode;
import com.codeshift.gateway.ModelGateway;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequirementsAgentTest {

    @Test
    void appendsNewFeatureNodeAsNewVersion() {
        ModelGateway gateway = mock(ModelGateway.class);
        when(gateway.isAvailable()).thenReturn(false); // offline → skeleton path
        RequirementsAgent agent = new RequirementsAgent(gateway);

        BsgGraph current = new BsgGraph("p1", 1, List.of(
                BsgNode.extracted("BSG-001", BsgNodeType.BUSINESS_RULE, "Order total", "d",
                        "OrderService", BsgConfidence.HIGH)),
                List.of());

        BsgGraph next = agent.addFeature(current,
                "When an order ships, send an SMS via Twilio with a tracking link",
                NewCodeMode.FEATURE, 2);

        assertThat(next.versionNumber()).isEqualTo(2);
        assertThat(next.nodes()).hasSize(2); // existing + 1 new feature
        BsgNode added = next.nodes().get(1);
        assertThat(added.origin()).isEqualTo(BsgOrigin.NEW_FEATURE);
        assertThat(added.nodeRef()).isEqualTo("BSG-F001");
        assertThat(added.description()).contains("Twilio");
    }

    @Test
    void integrationModeTagsNodeAsIntegrationExternalContract() {
        ModelGateway gateway = mock(ModelGateway.class);
        when(gateway.isAvailable()).thenReturn(false);
        RequirementsAgent agent = new RequirementsAgent(gateway);

        BsgGraph next = agent.addFeature(new BsgGraph("p1", 1, List.of(), List.of()),
                "Connect to Stripe for payments", NewCodeMode.INTEGRATION, 1);

        BsgNode added = next.nodes().get(0);
        assertThat(added.origin()).isEqualTo(BsgOrigin.INTEGRATION);
        assertThat(added.nodeType()).isEqualTo(BsgNodeType.EXTERNAL_CONTRACT);
    }
}

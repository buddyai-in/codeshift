package com.codeshift.agents;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.bsg.model.PerformanceReport;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import java.util.List;
import org.junit.jupiter.api.Test;

class PerformanceAgentTest {

    @Test
    void recommendsCachingAsyncAndVirtualThreads() {
        BsgGraph bsg = new BsgGraph("p", 1, List.of(
                BsgNode.extracted("BSG-001", BsgNodeType.BUSINESS_RULE, "Load product catalog",
                        "Reads the product catalog", "CatalogService", BsgConfidence.HIGH),
                BsgNode.extracted("BSG-002", BsgNodeType.BUSINESS_RULE, "Send email receipt",
                        "Emails a receipt after checkout", "NotificationService", BsgConfidence.HIGH),
                BsgNode.extracted("BSG-003", BsgNodeType.EXTERNAL_CONTRACT, "Call shipping API",
                        "Calls an external shipping API over HTTP", "ShippingClient", BsgConfidence.HIGH)),
                List.of());

        PerformanceReport r = new PerformanceAgent().analyze(bsg);
        assertThat(r.recommendations()).extracting(PerformanceReport.Recommendation::type)
                .contains("CACHING", "ASYNC", "VIRTUAL_THREADS");
    }
}

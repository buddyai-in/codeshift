package com.codeshift.bsg;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import com.codeshift.common.BsgOrigin;
import com.codeshift.common.HumanStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** BSG persistence + versioning + the audit trail, against a real (H2) database. */
@SpringBootTest(classes = PersistenceTestApp.class)
class BsgPersistenceTest {

    @Autowired
    private BsgStore bsgStore;

    @Autowired
    private ProjectStore projectStore;

    @Test
    void persistsVersionedBsgAsAnAuditTrail() {
        UUID projectId = projectStore.create("acme-orders", "JAVA_8", "JAVA_21_SPRING_BOOT");

        // Version 1 — the migrated BSG.
        BsgGraph v1 = new BsgGraph(projectId.toString(), 1, List.of(
                BsgNode.extracted("BSG-001", BsgNodeType.BUSINESS_RULE, "Order total",
                        "Sums line items", "OrderService", BsgConfidence.HIGH)),
                List.of());
        UUID v1Id = bsgStore.saveGraph(v1);
        bsgStore.approveVersion(v1Id, "analyst@acme");

        BsgGraph reloaded = bsgStore.getVersion(v1Id);
        assertThat(reloaded.nodes()).hasSize(1);
        assertThat(reloaded.nodes().get(0).nodeRef()).isEqualTo("BSG-001");

        // Version 2 — a new feature added a NEW_FEATURE node (new-code addition).
        int next = bsgStore.nextVersionNumber(projectId);
        assertThat(next).isEqualTo(2);
        BsgGraph v2 = new BsgGraph(projectId.toString(), next, List.of(
                BsgNode.extracted("BSG-001", BsgNodeType.BUSINESS_RULE, "Order total",
                        "Sums line items", "OrderService", BsgConfidence.HIGH),
                new BsgNode("BSG-100", BsgNodeType.BUSINESS_RULE, "SMS on ship",
                        "When an order ships, send an SMS", "NotificationService",
                        BsgConfidence.MEDIUM, HumanStatus.PENDING, BsgOrigin.NEW_FEATURE, null, false)),
                List.of());
        UUID v2Id = bsgStore.saveGraph(v2);

        // The version chain is the audit trail — newest first.
        List<BsgStore.VersionSummary> versions = bsgStore.listVersions(projectId);
        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).versionNumber()).isEqualTo(2);
        assertThat(versions.get(0).nodeCount()).isEqualTo(2);
        assertThat(versions.get(1).approved()).isTrue(); // v1 stays approved

        // v2 carries the new NEW_FEATURE node.
        assertThat(bsgStore.getVersion(v2Id).nodes())
                .anyMatch(n -> n.nodeRef().equals("BSG-100") && n.origin() == BsgOrigin.NEW_FEATURE);
    }
}

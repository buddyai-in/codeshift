package com.codeshift.agents;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.bsg.model.DebtReport;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import java.util.List;
import org.junit.jupiter.api.Test;

class DebtAgentTest {

    private final DebtAgent agent = new DebtAgent();

    private BsgNode node(String ref, BsgConfidence c, boolean covered) {
        return new BsgNode(ref, BsgNodeType.BUSINESS_RULE, ref, "d", "M",
                c, com.codeshift.common.HumanStatus.APPROVED,
                com.codeshift.common.BsgOrigin.MIGRATED, null, covered);
    }

    @Test
    void lowConfidenceDrivesDebtUpAndDeltaTracksNewNodes() {
        BsgGraph v1 = new BsgGraph("p", 1, List.of(
                node("BSG-001", BsgConfidence.HIGH, true)), List.of());
        BsgGraph v2 = new BsgGraph("p", 2, List.of(
                node("BSG-001", BsgConfidence.HIGH, true),
                node("BSG-002", BsgConfidence.LOW, false)), List.of());

        DebtReport clean = agent.analyze(v1, null);
        assertThat(clean.grade()).isEqualTo("A");
        assertThat(clean.debtScore()).isLessThan(20);

        DebtReport report = agent.analyze(v2, v1);
        assertThat(report.debtScore()).isGreaterThan(clean.debtScore());
        assertThat(report.signals()).anyMatch(s -> s.contains("LOW confidence"));
        assertThat(report.delta().addedRefs()).containsExactly("BSG-002");
        assertThat(report.delta().unchanged()).isEqualTo(1);
    }
}

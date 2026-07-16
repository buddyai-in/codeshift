package com.codeshift.compliance;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ComplianceReporterTest {

    @Test
    void seededTemplateBsgIsFullyCompliant() {
        // A vertical project seeded with the PCI template covers every control.
        BsgGraph seeded = new BsgGraph("proj", 1,
                ComplianceTemplates.templateNodes(ComplianceStandard.PCI_DSS), List.of());

        ComplianceReport report = ComplianceReporter.assess(seeded, ComplianceStandard.PCI_DSS);

        assertThat(report.standard()).isEqualTo("PCI_DSS");
        assertThat(report.reference()).isEqualTo("PCI-DSS v4.0");
        assertThat(report.passed()).isTrue();
        assertThat(report.score()).isEqualTo(100);
        assertThat(report.coveredControls()).isEqualTo(report.totalControls());
    }

    @Test
    void unrelatedBsgShowsGapsWithRemediation() {
        // A generic migrated BSG (no security rules) fails PCI and lists remediations.
        BsgGraph generic = new BsgGraph("proj", 1, List.of(
                BsgNode.extracted("BSG-001", BsgNodeType.BUSINESS_RULE, "Order total",
                        "Sums the order line items", "OrderService", BsgConfidence.HIGH)),
                List.of());

        ComplianceReport report = ComplianceReporter.assess(generic, ComplianceStandard.PCI_DSS);

        assertThat(report.passed()).isFalse();
        assertThat(report.coveredControls()).isEqualTo(0);
        assertThat(report.results())
                .allSatisfy(r -> {
                    assertThat(r.covered()).isFalse();
                    assertThat(r.remediation()).isNotBlank();
                });
    }

    @Test
    void partialCoverageIsScoredAndAttributed() {
        // One PCI-relevant rule (audit logging) covers exactly one control.
        BsgGraph partial = new BsgGraph("proj", 1, List.of(
                BsgNode.extracted("BSG-010", BsgNodeType.BUSINESS_RULE, "Access audit log",
                        "Writes an immutable audit log entry for every access", "AuditService",
                        BsgConfidence.HIGH)),
                List.of());

        ComplianceReport report = ComplianceReporter.assess(partial, ComplianceStandard.PCI_DSS);

        assertThat(report.coveredControls()).isEqualTo(1);
        assertThat(report.score()).isBetween(1, 99);
        assertThat(report.results())
                .filteredOn(r -> r.controlId().equals("PCI-10.2"))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.covered()).isTrue();
                    assertThat(r.matchedNodeRefs()).contains("BSG-010");
                });
    }

    @Test
    void hipaaPackIsAvailableToo() {
        BsgGraph seeded = new BsgGraph("proj", 1,
                ComplianceTemplates.templateNodes(ComplianceStandard.HIPAA), List.of());

        ComplianceReport report = ComplianceReporter.assess(seeded, ComplianceStandard.HIPAA);

        assertThat(report.passed()).isTrue();
        assertThat(report.totalControls()).isEqualTo(5);
    }
}

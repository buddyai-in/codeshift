package com.codeshift.agents;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.bsg.model.TransformationResult;
import com.codeshift.bsg.model.TransformationResult.GeneratedTest;
import com.codeshift.bsg.model.TransformationResult.TransformedModule;
import com.codeshift.bsg.model.ValidationReport;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationAgentTest {

    private final ValidationAgent agent = new ValidationAgent();

    private BsgGraph bsg() {
        return new BsgGraph("demo", 1, List.of(
                BsgNode.extracted("BSG-001", BsgNodeType.BUSINESS_RULE, "A", "d", "M1", BsgConfidence.HIGH),
                BsgNode.extracted("BSG-002", BsgNodeType.BUSINESS_RULE, "B", "d", "M2", BsgConfidence.HIGH)),
                List.of());
    }

    @Test
    void passesWhenCompiledAndCovered() {
        TransformationResult tr = new TransformationResult(
                List.of(new TransformedModule("M1", "M1", "SERVICE", "//", true, List.of("BSG-001"))),
                List.of(new GeneratedTest("T", "//", "BSG-002")),
                true, List.of());

        ValidationReport r = agent.validate(bsg(), tr);
        assertThat(r.compileOk()).isTrue();
        assertThat(r.coveragePercent()).isEqualTo(100);
        assertThat(r.passed()).isTrue();
    }

    @Test
    void failsWhenCompileFailedOrCoverageLow() {
        TransformationResult tr = new TransformationResult(
                List.of(new TransformedModule("M1", "M1", "SERVICE", "//", false, List.of())),
                List.of(), false, List.of("error"));

        ValidationReport r = agent.validate(bsg(), tr);
        assertThat(r.compileOk()).isFalse();
        assertThat(r.passed()).isFalse();
        assertThat(r.issues()).isNotEmpty();
    }
}

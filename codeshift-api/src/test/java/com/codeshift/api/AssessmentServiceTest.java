package com.codeshift.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.assessment.AssessmentResult;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Exercises the free-assessment use case end-to-end (parse → report + graph), no DB. */
class AssessmentServiceTest {

    @Test
    void assessesSampleDirectory() {
        AssessmentResult result = new AssessmentService().assessDirectory(
                "sample", Path.of("../codeshift-parser/src/test/resources/sample-project"));

        assertThat(result.report().moduleCount()).isEqualTo(5);
        assertThat(result.report().usesJavaxNamespace()).isTrue();
        assertThat(result.report().messagingSystems()).contains("JMS");
        assertThat(result.report().priceEstimateUsd()).isEqualTo(999); // Growth floor
        assertThat(result.report().translationOrder().indexOf("com.acme.repo.OrderRepository"))
                .isLessThan(result.report().translationOrder().indexOf("com.acme.web.OrderController"));

        // The graph the UI renders: 5 nodes, and Controller→Service among the edges.
        assertThat(result.graph().nodes()).hasSize(5);
        assertThat(result.graph().edges())
                .anySatisfy(e -> {
                    assertThat(e.source()).isEqualTo("com.acme.web.OrderController");
                    assertThat(e.target()).isEqualTo("com.acme.service.OrderService");
                });
    }
}

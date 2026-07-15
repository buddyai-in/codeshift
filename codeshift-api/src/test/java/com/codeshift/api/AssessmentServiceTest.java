package com.codeshift.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.assessment.AssessmentReport;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Exercises the free-assessment use case end-to-end (parse → report), no DB. */
class AssessmentServiceTest {

    @Test
    void assessesSampleDirectory() {
        AssessmentReport report = new AssessmentService().assessDirectory(
                "sample", Path.of("../codeshift-parser/src/test/resources/sample-project"));

        assertThat(report.moduleCount()).isEqualTo(5);
        assertThat(report.usesJavaxNamespace()).isTrue();
        assertThat(report.messagingSystems()).contains("JMS");
        assertThat(report.priceEstimateUsd()).isEqualTo(999); // Growth floor for a tiny codebase
        assertThat(report.translationOrder().indexOf("com.acme.repo.OrderRepository"))
                .isLessThan(report.translationOrder().indexOf("com.acme.web.OrderController"));
    }
}

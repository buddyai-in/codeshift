package com.codeshift.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.parser.JavaProjectAnalyzer;
import com.codeshift.parser.ProjectAnalysis;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AssessmentGeneratorTest {

    private AssessmentReport reportForSample() {
        ProjectAnalysis a = JavaProjectAnalyzer.analyze(
                Path.of("../codeshift-parser/src/test/resources/sample-project"));
        return AssessmentGenerator.generate("sample", a);
    }

    @Test
    void producesReportWithPricingAndSignals() {
        AssessmentReport r = reportForSample();
        assertThat(r.moduleCount()).isEqualTo(5);
        assertThat(r.priceEstimateUsd()).isGreaterThanOrEqualTo(999); // Growth floor
        assertThat(r.suggestedTier()).isEqualTo("Starter");           // tiny sample
        assertThat(r.migrationSignals())
                .anyMatch(s -> s.contains("javax.*"))
                .anyMatch(s -> s.contains("Kafka"));
        // Leaf-first: dependencies precede dependents (which leaf sorts first is unspecified).
        assertThat(r.translationOrder().indexOf("com.acme.repo.OrderRepository"))
                .isLessThan(r.translationOrder().indexOf("com.acme.web.OrderController"));
    }

    @Test
    void smallCodebaseHitsGrowthFloor() {
        AssessmentReport r = reportForSample();
        // A handful of lines must still price at the Growth floor, not a few dollars.
        assertThat(r.priceEstimateUsd()).isEqualTo(999);
        assertThat(r.estimatedEffortDays()).isGreaterThanOrEqualTo(1);
    }
}

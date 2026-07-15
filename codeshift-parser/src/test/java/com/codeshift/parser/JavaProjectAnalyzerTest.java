package com.codeshift.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class JavaProjectAnalyzerTest {

    private ProjectAnalysis analyzeSample() {
        Path root = Path.of("src/test/resources/sample-project");
        return JavaProjectAnalyzer.analyze(root);
    }

    @Test
    void buildsInventoryAndDependencyGraph() {
        ProjectAnalysis a = analyzeSample();

        assertThat(a.moduleIds()).contains(
                "com.acme.web.OrderController",
                "com.acme.service.OrderService",
                "com.acme.repo.OrderRepository",
                "com.acme.rules.PricingRule",
                "com.acme.messaging.OrderPublisher");

        // Edges derived from imports: Controller -> Service -> {Repository, PricingRule}
        assertThat(a.dependencyEdges())
                .anySatisfy(e -> assertThat(e).containsExactly(
                        "com.acme.web.OrderController", "com.acme.service.OrderService"))
                .anySatisfy(e -> assertThat(e).containsExactly(
                        "com.acme.service.OrderService", "com.acme.repo.OrderRepository"));
    }

    @Test
    void leafFirstOrderPutsDependenciesBeforeDependents() {
        List<String> order = analyzeSample().translationOrder();
        assertThat(order.indexOf("com.acme.repo.OrderRepository"))
                .isLessThan(order.indexOf("com.acme.service.OrderService"));
        assertThat(order.indexOf("com.acme.service.OrderService"))
                .isLessThan(order.indexOf("com.acme.web.OrderController"));
    }

    @Test
    void detectsMessagingAndJavaxSignal() {
        ProjectAnalysis a = analyzeSample();
        assertThat(a.messagingSystems()).contains("JMS");
        assertThat(a.usesJavaxNamespace()).isTrue();
    }

    @Test
    void identifiesWebEntryPoint() {
        ProjectAnalysis a = analyzeSample();
        assertThat(a.modules())
                .filteredOn(SourceModule::entryPoint)
                .extracting(SourceModule::simpleName)
                .contains("OrderController");
    }
}

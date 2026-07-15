package com.codeshift.agents;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.BsgGraph;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureAgentTest {

    @Test
    void infersLayersBoundariesAndPhases() {
        List<String> order = List.of(
                "com.acme.repo.OrderRepository",
                "com.acme.rules.PricingRule",
                "com.acme.service.OrderService",
                "com.acme.web.OrderController");
        ArchitecturePlan plan = new ArchitectureAgent().produce(
                new BsgGraph("demo", 1, List.of(), List.of()), order, null);

        assertThat(plan.targetStack()).isEqualTo("JAVA_21_SPRING_BOOT");

        // Layer inference from naming.
        assertThat(plan.moduleMappings())
                .anySatisfy(m -> {
                    assertThat(m.moduleId()).isEqualTo("com.acme.web.OrderController");
                    assertThat(m.layer()).isEqualTo("CONTROLLER");
                })
                .anySatisfy(m -> {
                    assertThat(m.moduleId()).isEqualTo("com.acme.repo.OrderRepository");
                    assertThat(m.layer()).isEqualTo("REPOSITORY");
                });

        // Phases: data/domain layer (rank 1) migrates before the web layer (rank 4).
        int dataPhase = phaseOrderContaining(plan, "com.acme.repo.OrderRepository");
        int webPhase = phaseOrderContaining(plan, "com.acme.web.OrderController");
        assertThat(dataPhase).isLessThan(webPhase);

        // Boundaries clustered by package.
        assertThat(plan.microservices()).isNotEmpty();
    }

    private int phaseOrderContaining(ArchitecturePlan plan, String moduleId) {
        return plan.phases().stream()
                .filter(p -> p.moduleIds().contains(moduleId))
                .findFirst().orElseThrow().order();
    }
}

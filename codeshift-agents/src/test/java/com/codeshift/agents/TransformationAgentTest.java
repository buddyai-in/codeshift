package com.codeshift.agents;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.ArchitecturePlan.ModuleMapping;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.bsg.model.TransformationResult;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransformationAgentTest {

    @Test
    void generatesCompilableModulesAndTracesRules() {
        BsgGraph bsg = new BsgGraph("demo", 1, List.of(
                BsgNode.extracted("BSG-001", BsgNodeType.BUSINESS_RULE, "Pricing",
                        "Even ids get 9.99", "com.acme.rules.PricingRule", BsgConfidence.MEDIUM),
                BsgNode.extracted("BSG-002", BsgNodeType.BUSINESS_RULE, "Order lookup",
                        "Loads an order by id", "com.acme.service.OrderService", BsgConfidence.HIGH)),
                List.of());
        ArchitecturePlan arch = new ArchitecturePlan("JAVA_21_SPRING_BOOT", List.of(
                new ModuleMapping("com.acme.rules.PricingRule", "PricingRule", "DOMAIN"),
                new ModuleMapping("com.acme.service.OrderService", "OrderService", "SERVICE")),
                List.of(), List.of());

        TransformationResult result = new TransformationAgent()
                .produce(bsg, arch, List.of("com.acme.rules.PricingRule", "com.acme.service.OrderService"));

        // Real compilation succeeded for the generated modules.
        assertThat(result.allCompiled()).isTrue();
        assertThat(result.modules()).hasSize(2);
        assertThat(result.modules()).allMatch(TransformationResult.TransformedModule::compiled);

        // Each module traces its BSG rule(s).
        assertThat(result.modules())
                .anySatisfy(m -> {
                    assertThat(m.moduleId()).isEqualTo("com.acme.rules.PricingRule");
                    assertThat(m.bsgRuleRefs()).contains("BSG-001");
                });

        // A JUnit 5 test per BusinessRule, with the rule id embedded.
        assertThat(result.tests()).hasSize(2);
        assertThat(result.tests()).allMatch(t -> t.sourceCode().contains(t.bsgRuleRef()));
    }
}

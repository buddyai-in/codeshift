package com.codeshift.agents;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.ArchitecturePlan.ServiceBoundary;
import com.codeshift.bsg.model.HardeningResult;
import com.codeshift.bsg.model.TransformationResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class HardeningAgentTest {

    private final HardeningAgent agent = new HardeningAgent();

    private ArchitecturePlan arch() {
        return new ArchitecturePlan("JAVA_21_SPRING_BOOT", List.of(),
                List.of(new ServiceBoundary("OrdersService", List.of("M1"))), List.of());
    }

    private TransformationResult empty() {
        return new TransformationResult(List.of(), List.of(), true, List.of());
    }

    @Test
    void generatesDeployableDevOpsBundle() {
        HardeningResult r = agent.produce(arch(), empty(), null, List.of());
        assertThat(r.devops().dockerfile()).contains("temurin:21").contains("USER appuser");
        assertThat(r.devops().kubernetesManifest()).contains("kind: Deployment").contains("ordersservice");
        assertThat(r.devops().ciPipeline()).contains("mvn -B verify");
    }

    @Test
    void plansKafkaTopicsFromDetectedMessaging() {
        HardeningResult r = agent.produce(arch(), empty(), null, List.of("JMS", "IBM_MQ"));
        assertThat(r.messaging().topics()).hasSize(2);
        assertThat(r.messaging().topics())
                .anySatisfy(t -> assertThat(t.name()).isEqualTo("jms-events"));
    }

    @Test
    void scansRealSourceForSignals() {
        // The bundled sample uses javax.jms → a MEDIUM namespace finding.
        HardeningResult r = agent.produce(arch(), empty(),
                "../codeshift-parser/src/test/resources/sample-project", List.of("JMS"));
        assertThat(r.security().findings())
                .anySatisfy(f -> assertThat(f.message()).contains("javax.*"));
    }
}

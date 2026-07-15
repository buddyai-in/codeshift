package com.codeshift.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.junit.jupiter.api.Test;

/** The orchestration spine: leaf-first discovery + durable interrupt/resume. */
class MigrationGraphTest {

    @Test
    void interruptsAtGateThenResumesToArchitecture() throws Exception {
        CompiledGraph<MigrationState> app = new MigrationGraphFactory().build(new MemorySaver());
        RunnableConfig cfg = RunnableConfig.builder().threadId("t1").build();

        // Run suspends before the human review gate (interruptBefore("review")).
        Optional<MigrationState> paused = app.invoke(Map.of("project_id", "demo"), cfg);
        assertThat(paused).isPresent();
        assertThat(paused.get().topoOrder())
                .containsExactly("OrderRepository", "PricingRule", "OrderService", "OrderController");

        // A human injects the decision, then we resume from the exact checkpoint.
        RunnableConfig resumeCfg = app.updateState(cfg, Map.of("review_decision", "APPROVED"));
        Optional<MigrationState> done = app.invoke(GraphInput.resume(), resumeCfg);

        assertThat(done).isPresent();
        assertThat(done.get().reviewDecision()).contains("APPROVED");
        assertThat(done.get().phase()).contains("ARCHITECTURE");
    }

    @Test
    void discoveryParsesRealProjectWhenPathGiven() throws Exception {
        CompiledGraph<MigrationState> app = new MigrationGraphFactory().build(new MemorySaver());
        RunnableConfig cfg = RunnableConfig.builder().threadId("t2").build();

        // Phase 1 wiring: a real project path routes discovery through JavaParser.
        Optional<MigrationState> paused = app.invoke(
                Map.of("project_id", "acme",
                        "project_path", "../codeshift-parser/src/test/resources/sample-project"),
                cfg);

        assertThat(paused).isPresent();
        assertThat(paused.get().topoOrder())
                .contains("com.acme.repo.OrderRepository", "com.acme.web.OrderController");
        assertThat(paused.get().topoOrder().indexOf("com.acme.repo.OrderRepository"))
                .isLessThan(paused.get().topoOrder().indexOf("com.acme.web.OrderController"));
    }
}

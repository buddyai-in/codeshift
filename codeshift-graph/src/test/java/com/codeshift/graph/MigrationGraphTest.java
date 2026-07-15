package com.codeshift.graph;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.bsg.ArchitectureProducer;
import com.codeshift.bsg.BsgProducer;
import com.codeshift.bsg.TransformationProducer;
import com.codeshift.bsg.ValidationProducer;
import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.bsg.model.TransformationResult;
import com.codeshift.bsg.model.ValidationReport;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.junit.jupiter.api.Test;

/** The orchestration spine: discovery → analysis → BSG gate → architecture → gate #2. */
class MigrationGraphTest {

    // Deterministic stand-in for the Analysis Agent: one node per module.
    private static final BsgProducer STUB_PRODUCER = (projectId, moduleIds, projectPath) ->
            new BsgGraph(projectId, 1, moduleIds.stream().map(id -> BsgNode.extracted(
                    "BSG-" + id, BsgNodeType.BUSINESS_RULE, "Rule for " + id, "d", id,
                    BsgConfidence.MEDIUM)).toList(), List.of());

    // Stand-in for the Architecture Agent.
    private static final ArchitectureProducer ARCH_STUB = (bsg, topoOrder, stack) ->
            new ArchitecturePlan(stack,
                    topoOrder.stream()
                            .map(id -> new ArchitecturePlan.ModuleMapping(id, id, "SERVICE")).toList(),
                    List.of(new ArchitecturePlan.ServiceBoundary("Monolith", topoOrder)),
                    List.of(new ArchitecturePlan.MigrationPhase(1, "All", topoOrder)));

    // Stand-in for the Transformation Agent.
    private static final TransformationProducer TRANSFORM_STUB = (bsg, arch, topoOrder) ->
            new TransformationResult(
                    arch.moduleMappings().stream()
                            .map(m -> new TransformationResult.TransformedModule(
                                    m.moduleId(), m.targetClass(), m.layer(), "// code", true, List.of()))
                            .toList(),
                    List.of(), true, List.of());

    // Stand-in for the Validation Agent (always passes).
    private static final ValidationProducer VALIDATION_STUB = (bsg, tr) ->
            new ValidationReport(true, bsg.nodes().size(), bsg.nodes().size(), 100, true, List.of());

    private CompiledGraph<MigrationState> app() throws Exception {
        return new MigrationGraphFactory()
                .build(new MemorySaver(), STUB_PRODUCER, ARCH_STUB, TRANSFORM_STUB, VALIDATION_STUB);
    }

    @Test
    void twoGates_bsgThenArchitectureThenBuild() throws Exception {
        CompiledGraph<MigrationState> app = app();
        RunnableConfig cfg = RunnableConfig.builder().threadId("t1").build();

        // Gate #1: suspends before the BSG review gate with the BSG present.
        Optional<MigrationState> atBsgGate = app.invoke(Map.of("project_id", "demo"), cfg);
        assertThat(atBsgGate).isPresent();
        assertThat(atBsgGate.get().topoOrder())
                .containsExactly("OrderRepository", "PricingRule", "OrderService", "OrderController");
        assertThat(atBsgGate.get().bsg().get().nodes()).hasSize(4);

        // Approve BSG → advances and suspends at gate #2 with an architecture plan.
        RunnableConfig c2 = app.updateState(cfg, Map.of("review_decision", "APPROVED"));
        Optional<MigrationState> atArchGate = app.invoke(GraphInput.resume(), c2);
        assertThat(atArchGate).isPresent();
        assertThat(atArchGate.get().phase()).contains("ARCH_REVIEW");
        assertThat(atArchGate.get().architecture()).isPresent();
        assertThat(atArchGate.get().architecture().get().moduleMappings()).hasSize(4);

        // Approve architecture → build + validation run → DELIVERY.
        RunnableConfig c3 = app.updateState(cfg, Map.of("review_decision", "APPROVED"));
        Optional<MigrationState> done = app.invoke(GraphInput.resume(), c3);
        assertThat(done).isPresent();
        assertThat(done.get().phase()).contains("DELIVERY");
        assertThat(done.get().transformation().get().modules()).hasSize(4);
        assertThat(done.get().validation()).isPresent();
        assertThat(done.get().validation().get().passed()).isTrue();
    }

    @Test
    void rejectedBsgEndsWithoutArchitecture() throws Exception {
        CompiledGraph<MigrationState> app = app();
        RunnableConfig cfg = RunnableConfig.builder().threadId("t3").build();

        app.invoke(Map.of("project_id", "demo"), cfg);
        RunnableConfig c2 = app.updateState(cfg, Map.of("review_decision", "REJECTED"));
        Optional<MigrationState> done = app.invoke(GraphInput.resume(), c2);

        assertThat(done).isPresent();
        assertThat(done.get().architecture()).isEmpty(); // never reached the Architecture Agent
    }

    @Test
    void discoveryParsesRealProjectWhenPathGiven() throws Exception {
        CompiledGraph<MigrationState> app = app();
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

package com.codeshift.graph;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

import com.codeshift.bsg.ArchitectureProducer;
import com.codeshift.bsg.BsgProducer;
import com.codeshift.bsg.HardeningProducer;
import com.codeshift.bsg.TransformationProducer;
import com.codeshift.bsg.ValidationProducer;
import java.util.Map;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;

/**
 * Assembles the migration graph. The graph <em>is</em> the process: nodes are
 * agents, edges are control flow, the two interrupts are the human review gates,
 * and the checkpointer makes the whole thing durable and resumable.
 *
 * <p>Framework-agnostic on purpose (no Spring). New pillars/agents are new nodes.
 */
public class MigrationGraphFactory {

    /**
     * Compile the pipeline through both review gates:
     *
     * <pre>
     * discovery ─▶ analysis ─▶ review* ─▶ bsg-gate ─┬─(approved)─▶ architecture ─▶ arch-review* ─▶ arch-gate ─▶ END
     *                                               └─(rejected)──────────────────────────────────────────────▶ END
     * </pre>
     *
     * (* = durable interrupt / human gate)
     *
     * @param bsgProducer    Analysis Agent (builds the BSG)
     * @param archProducer   Architecture Agent (builds the target architecture)
     * @param transformProducer Transformation + Test Generation agents
     * @param validationProducer Validation Agent (compile + BSG coverage)
     * @param hardeningProducer  Security / Cloud / Messaging agents
     */
    public CompiledGraph<MigrationState> build(BaseCheckpointSaver checkpointSaver,
            BsgProducer bsgProducer, ArchitectureProducer archProducer,
            TransformationProducer transformProducer, ValidationProducer validationProducer,
            HardeningProducer hardeningProducer) throws GraphStateException {
        final int maxBuildRetries = 3;
        StateGraph<MigrationState> workflow =
                new StateGraph<>(MigrationState.SCHEMA, MigrationState::new)
                        .addNode("discovery", GraphNodes.discovery())
                        .addNode("analysis", GraphNodes.analysis(bsgProducer))
                        .addNode("review", GraphNodes.review())
                        .addNode("bsg_gate", GraphNodes.bsgGate())
                        .addNode("architecture", GraphNodes.architecture(archProducer))
                        .addNode("arch_review", GraphNodes.review())
                        .addNode("arch_gate", GraphNodes.archGate())
                        .addNode("build", GraphNodes.build(transformProducer))
                        .addNode("validation", GraphNodes.validation(validationProducer))
                        .addNode("hardening", GraphNodes.hardening(hardeningProducer))
                        .addNode("delivery", GraphNodes.delivery())
                        .addEdge(START, "discovery")
                        .addEdge("discovery", "analysis")
                        .addEdge("analysis", "review")
                        .addEdge("review", "bsg_gate")
                        .addConditionalEdges("bsg_gate",
                                edge_async(s -> "APPROVED".equals(s.reviewDecision().orElse(""))
                                        ? "approved" : "rejected"),
                                Map.of("approved", "architecture", "rejected", END))
                        .addEdge("architecture", "arch_review")
                        .addEdge("arch_review", "arch_gate")
                        .addConditionalEdges("arch_gate",
                                edge_async(s -> "APPROVED".equals(s.reviewDecision().orElse(""))
                                        ? "approved" : "rejected"),
                                Map.of("approved", "build", "rejected", END))
                        .addEdge("build", "validation")
                        // Feedback loop: validation failure → targeted rebuild (bounded).
                        .addConditionalEdges("validation",
                                edge_async(s -> {
                                    if (s.validation().map(v -> v.passed()).orElse(false)) {
                                        return "passed";
                                    }
                                    return s.buildRetries() < maxBuildRetries ? "retry" : "failed";
                                }),
                                Map.of("passed", "hardening", "retry", "build", "failed", "hardening"))
                        .addEdge("hardening", "delivery")
                        .addEdge("delivery", END);

        return workflow.compile(CompileConfig.builder()
                .checkpointSaver(checkpointSaver)
                .interruptBefore("review", "arch_review") // gate #1 (BSG) and gate #2 (architecture)
                .build());
    }
}

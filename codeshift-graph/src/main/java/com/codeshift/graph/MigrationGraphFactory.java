package com.codeshift.graph;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;

/**
 * Assembles the migration graph. The graph <em>is</em> the process: nodes are
 * agents, edges are control flow, the interrupt before {@code review} is the
 * human gate, and the checkpointer makes the whole thing durable and resumable.
 *
 * <p>Framework-agnostic on purpose (no Spring) so the orchestration core stays
 * portable. New pillars/agents are new nodes on this same {@link StateGraph}.
 */
public class MigrationGraphFactory {

    /**
     * Compile the Phase 0 spine:
     *
     * <pre>discovery ─▶ review (interruptBefore) ─▶ finalize ─▶ END</pre>
     */
    public CompiledGraph<MigrationState> build(BaseCheckpointSaver checkpointSaver)
            throws GraphStateException {
        StateGraph<MigrationState> workflow =
                new StateGraph<>(MigrationState.SCHEMA, MigrationState::new)
                        .addNode("discovery", GraphNodes.discovery())
                        .addNode("review", GraphNodes.review())
                        .addNode("finalize", GraphNodes.finalizeNode())
                        .addEdge(START, "discovery")
                        .addEdge("discovery", "review")
                        .addEdge("review", "finalize")
                        .addEdge("finalize", END);

        return workflow.compile(CompileConfig.builder()
                .checkpointSaver(checkpointSaver)
                .interruptBefore("review") // durable human-in-the-loop gate
                .build());
    }
}

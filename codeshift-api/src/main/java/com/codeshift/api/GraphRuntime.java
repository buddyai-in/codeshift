package com.codeshift.api;

import com.codeshift.graph.MigrationGraphFactory;
import com.codeshift.graph.MigrationState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Drives migration runs on the compiled langgraph4j graph.
 *
 * <p>Phase 0 uses an in-memory checkpointer so runs work with zero infrastructure.
 * A durable (JDBC/Postgres) checkpointer is a Phase 1 swap — it changes only the
 * saver passed to {@link MigrationGraphFactory#build}, nothing here.
 */
@Service
public class GraphRuntime {

    private final CompiledGraph<MigrationState> graph;

    public GraphRuntime() {
        try {
            this.graph = new MigrationGraphFactory().build(new MemorySaver());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compile migration graph", e);
        }
    }

    /** Start a run; it advances to the human review gate and suspends there. */
    public StartResult start(String projectId, List<String> modules, String projectPath) {
        String threadId = UUID.randomUUID().toString();
        RunnableConfig cfg = RunnableConfig.builder().threadId(threadId).build();
        Map<String, Object> input = new HashMap<>();
        input.put("project_id", projectId);
        if (projectPath != null && !projectPath.isBlank()) {
            input.put("project_path", projectPath); // routes discovery through JavaParser
        }
        if (modules != null && !modules.isEmpty()) {
            input.put("module_inventory", modules);
        }
        MigrationState state = graph.invoke(input, cfg)
                .orElseThrow(() -> new IllegalStateException("Graph produced no state"));
        return new StartResult(threadId, state.phase().orElse(null), true,
                state.topoOrder(), state.log());
    }

    /** Resume a suspended run at its gate with a human decision. */
    public ResumeResult resume(String threadId, String decision) {
        RunnableConfig cfg = RunnableConfig.builder().threadId(threadId).build();
        try {
            RunnableConfig resumeCfg = graph.updateState(cfg, Map.of("review_decision", decision));
            MigrationState state = graph.invoke(GraphInput.resume(), resumeCfg)
                    .orElseThrow(() -> new IllegalStateException("Unknown thread " + threadId));
            return new ResumeResult(threadId, state.phase().orElse(null),
                    state.reviewDecision().orElse(null), state.log());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resume run " + threadId, e);
        }
    }

    /** Stream live per-node updates (SSE) as a fresh run advances to the gate. */
    public void stream(String projectId, SseEmitter emitter) {
        Thread worker = new Thread(() -> {
            String threadId = UUID.randomUUID().toString();
            RunnableConfig cfg = RunnableConfig.builder().threadId(threadId).build();
            try {
                for (NodeOutput<MigrationState> out : graph.stream(Map.of("project_id", projectId), cfg)) {
                    Optional<String> phase = out.state().phase();
                    emitter.send(SseEmitter.event().name("update")
                            .data(Map.of("node", out.node(), "phase", phase.orElse("-"))));
                }
                emitter.send(SseEmitter.event().name("done").data(Map.of("threadId", threadId)));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    /** Response of {@link #start}. */
    public record StartResult(String threadId, String phase, boolean awaitingHuman,
                              List<String> translationOrder, List<String> log) {}

    /** Response of {@link #resume}. */
    public record ResumeResult(String threadId, String phase, String reviewDecision,
                               List<String> log) {}
}

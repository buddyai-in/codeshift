package com.codeshift.api;

import com.codeshift.bsg.ArchitectureProducer;
import com.codeshift.bsg.BsgProducer;
import com.codeshift.bsg.BsgStore;
import com.codeshift.bsg.HardeningProducer;
import com.codeshift.bsg.ProjectStore;
import com.codeshift.bsg.TenantStore;
import com.codeshift.bsg.TransformationProducer;
import com.codeshift.bsg.ValidationProducer;
import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.bsg.model.HardeningResult;
import com.codeshift.bsg.model.TransformationResult;
import com.codeshift.bsg.model.ValidationReport;
import com.codeshift.common.HumanStatus;
import com.codeshift.graph.MigrationGraphFactory;
import com.codeshift.graph.MigrationState;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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

    private static final Logger log = LoggerFactory.getLogger(GraphRuntime.class);

    /** Decision value the graph treats as "approve this gate" (see MigrationGraphFactory). */
    private static final String APPROVED = "APPROVED";

    /** The platform's standard target for a migrated Java system (Architecture Agent refines later). */
    private static final String DEFAULT_TARGET_STACK = "JAVA_21_SPRING_BOOT";

    private final CompiledGraph<MigrationState> graph;

    // Persistence is profile-gated (absent under the nodb profile), so it's optional here.
    private final ObjectProvider<ProjectStore> projectStore;
    private final ObjectProvider<BsgStore> bsgStore;
    private final ObjectProvider<TenantStore> tenantStore;

    // threadId -> persisted project id, so the BSG gate auto-persists exactly once per run.
    private final Map<String, String> persistedProjects = new ConcurrentHashMap<>();

    public GraphRuntime(BsgProducer bsgProducer, ArchitectureProducer architectureProducer,
            TransformationProducer transformationProducer, ValidationProducer validationProducer,
            HardeningProducer hardeningProducer, ObjectProvider<ProjectStore> projectStore,
            ObjectProvider<BsgStore> bsgStore, ObjectProvider<TenantStore> tenantStore) {
        this.projectStore = projectStore;
        this.bsgStore = bsgStore;
        this.tenantStore = tenantStore;
        try {
            this.graph = new MigrationGraphFactory().build(new MemorySaver(),
                    bsgProducer, architectureProducer, transformationProducer, validationProducer,
                    hardeningProducer);
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
        long bsgNodes = state.bsg().map(b -> (long) b.nodes().size()).orElse(0L);
        return new StartResult(threadId, state.phase().orElse(null), true,
                state.topoOrder(), bsgNodes, state.log());
    }

    /** Start a run from an uploaded source zip (extracted to a temp dir, then cleaned up). */
    public StartResult startFromZip(String projectName, InputStream zipStream) {
        Path dir = null;
        try {
            dir = ZipExtractor.extractToTempDir(zipStream);
            // start() runs discovery + analysis synchronously (up to the gate), so the
            // temp dir is only needed during this call.
            return start(projectName, List.of(), dir.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read zip: " + e.getMessage(), e);
        } finally {
            ZipExtractor.deleteRecursively(dir);
        }
    }

    /** The BSG produced by the Analysis Agent for a run, for the review gate. */
    public BsgGraph bsgOf(String threadId) {
        RunnableConfig cfg = RunnableConfig.builder().threadId(threadId).build();
        return graph.getState(cfg).state().bsg()
                .orElseThrow(() -> new IllegalStateException("No BSG for thread " + threadId));
    }

    /** The architecture plan proposed for a run, for gate #2. */
    public ArchitecturePlan architectureOf(String threadId) {
        RunnableConfig cfg = RunnableConfig.builder().threadId(threadId).build();
        return graph.getState(cfg).state().architecture()
                .orElseThrow(() -> new IllegalStateException("No architecture for thread " + threadId));
    }

    /** The generated code + tests for a run (after both gates are approved). */
    public TransformationResult transformationOf(String threadId) {
        RunnableConfig cfg = RunnableConfig.builder().threadId(threadId).build();
        return graph.getState(cfg).state().transformation()
                .orElseThrow(() -> new IllegalStateException("No transformation for thread " + threadId));
    }

    /** The Validation Agent's report for a run. */
    public ValidationReport validationOf(String threadId) {
        RunnableConfig cfg = RunnableConfig.builder().threadId(threadId).build();
        return graph.getState(cfg).state().validation()
                .orElseThrow(() -> new IllegalStateException("No validation for thread " + threadId));
    }

    /** The hardening result (security + DevOps + messaging) for a run. */
    public HardeningResult hardeningOf(String threadId) {
        RunnableConfig cfg = RunnableConfig.builder().threadId(threadId).build();
        return graph.getState(cfg).state().hardening()
                .orElseThrow(() -> new IllegalStateException("No hardening for thread " + threadId));
    }

    /**
     * Record a human review decision (and optional edits) on one BSG node, writing
     * it back into the run's durable state. This is the per-node review workflow
     * behind the gate — the reviewer curates the BSG before approving the whole thing.
     */
    public BsgGraph updateBsgNode(String threadId, String nodeRef, String status,
            String title, String description) {
        RunnableConfig cfg = RunnableConfig.builder().threadId(threadId).build();
        BsgGraph current = bsgOf(threadId);
        List<BsgNode> updated = current.nodes().stream()
                .map(n -> n.nodeRef().equals(nodeRef) ? applyEdit(n, status, title, description) : n)
                .toList();
        BsgGraph next = new BsgGraph(current.projectId(), current.versionNumber(),
                updated, current.edges());
        try {
            graph.updateState(cfg, Map.of("bsg", next));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update BSG node " + nodeRef, e);
        }
        return next;
    }

    private static BsgNode applyEdit(BsgNode n, String status, String title, String description) {
        HumanStatus st = status != null && !status.isBlank()
                ? HumanStatus.valueOf(status) : n.humanStatus();
        String t = title != null && !title.isBlank() ? title : n.title();
        String d = description != null && !description.isBlank() ? description : n.description();
        return new BsgNode(n.nodeRef(), n.nodeType(), t, d, n.sourceLocation(),
                n.confidence(), st, n.origin(), n.targetCodeLocation(), n.testCoverage());
    }

    /** Resume a suspended run at its gate with a human decision. */
    public ResumeResult resume(String threadId, String decision) {
        RunnableConfig cfg = RunnableConfig.builder().threadId(threadId).build();
        try {
            // The first gate reached is the BSG review. If it's approved, auto-persist the
            // curated BSG snapshot into a project before resuming — that snapshot is exactly
            // what the human signed off on, and it feeds the new-code / debt / portfolio pillars.
            String persistedProjectId = persistedProjects.get(threadId);
            if (persistedProjectId == null && APPROVED.equalsIgnoreCase(decision)) {
                persistedProjectId = autoPersistApprovedBsg(threadId);
            }

            RunnableConfig resumeCfg = graph.updateState(cfg, Map.of("review_decision", decision));
            MigrationState state = graph.invoke(GraphInput.resume(), resumeCfg)
                    .orElseThrow(() -> new IllegalStateException("Unknown thread " + threadId));
            return new ResumeResult(threadId, state.phase().orElse(null),
                    state.reviewDecision().orElse(null), state.log(), persistedProjectId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resume run " + threadId, e);
        }
    }

    /**
     * Persist the approved run BSG as a new project + approved v1. Best-effort: with the
     * {@code nodb} profile the stores are absent and this is a no-op; a persistence failure
     * is logged but never fails the migration resume. Runs at most once per thread.
     */
    private String autoPersistApprovedBsg(String threadId) {
        ProjectStore projects = projectStore.getIfAvailable();
        BsgStore store = bsgStore.getIfAvailable();
        if (projects == null || store == null) {
            return null; // nodb profile — nothing to persist into
        }
        try {
            BsgGraph approved = bsgOf(threadId);
            String projectName = approved.projectId() != null && !approved.projectId().isBlank()
                    ? approved.projectId() : "migration-" + threadId.substring(0, 8);
            UUID orgId = currentOrgId();
            UUID projectId = projects.create(orgId, projectName, "JAVA", DEFAULT_TARGET_STACK);

            int versionNumber = store.nextVersionNumber(projectId);
            BsgGraph toSave = new BsgGraph(projectId.toString(), versionNumber,
                    approved.nodes(), approved.edges());
            UUID versionId = store.saveGraph(toSave);
            store.approveVersion(versionId, "migration-run");

            persistedProjects.put(threadId, projectId.toString());
            log.info("Auto-persisted approved BSG for run {} as project {} (v{}, {} nodes)",
                    threadId, projectId, versionNumber, approved.nodes().size());
            return projectId.toString();
        } catch (Exception e) {
            log.warn("Auto-persist of approved BSG failed for run {} (continuing): {}",
                    threadId, e.getMessage());
            return null;
        }
    }

    /** The calling tenant (from the request's X-Tenant-Id), or the default org. */
    private UUID currentOrgId() {
        return TenantContext.current().orElseGet(() -> {
            TenantStore ts = tenantStore.getIfAvailable();
            return ts != null ? ts.defaultOrgId() : null;
        });
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
                              List<String> translationOrder, long bsgNodeCount,
                              List<String> log) {}

    /**
     * Response of {@link #resume}. {@code persistedProjectId} is set the first time the BSG
     * gate is approved (the run's BSG was auto-persisted into that project); null otherwise
     * and always null under the {@code nodb} profile.
     */
    public record ResumeResult(String threadId, String phase, String reviewDecision,
                               List<String> log, String persistedProjectId) {}
}

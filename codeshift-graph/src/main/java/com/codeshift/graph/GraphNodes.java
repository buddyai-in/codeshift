package com.codeshift.graph;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import com.codeshift.bsg.ArchitectureProducer;
import com.codeshift.bsg.BsgProducer;
import com.codeshift.bsg.HardeningProducer;
import com.codeshift.bsg.TransformationProducer;
import com.codeshift.bsg.ValidationProducer;
import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.HardeningResult;
import com.codeshift.bsg.model.TransformationResult;
import com.codeshift.bsg.model.ValidationReport;
import com.codeshift.common.Phase;
import com.codeshift.common.TopologicalSort;
import com.codeshift.parser.JavaProjectAnalyzer;
import com.codeshift.parser.ProjectAnalysis;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bsc.langgraph4j.action.AsyncNodeAction;

/**
 * Graph nodes for the Phase 0 spine:
 *
 * <pre>
 *   discovery ──▶ review (human gate) ──▶ finalize
 * </pre>
 *
 * <ul>
 *   <li>{@code discovery} is deterministic (Kahn topological sort) — so the spine
 *       runs with zero API keys. Phase 1 swaps the built-in sample for real output
 *       from {@code java-parser-mcp}.</li>
 *   <li>The graph interrupts <em>before</em> {@code review}; the run durably
 *       suspends there until a human injects a decision and resumes it. This is
 *       CodeShift's core "trust boundary" gate.</li>
 *   <li>{@code finalize} routes on the human decision.</li>
 * </ul>
 *
 * Every later agent (Analysis, Architecture, Transformation, …) slots in as more
 * nodes on this same graph — the pattern does not change.
 */
public final class GraphNodes {

    private GraphNodes() {}

    // Built-in sample so the spine runs offline (replaced by java-parser-mcp in Phase 1).
    private static final List<String> DEFAULT_MODULES =
            List.of("OrderController", "OrderService", "OrderRepository", "PricingRule");
    private static final List<String[]> DEFAULT_EDGES = List.of(
            new String[] {"OrderController", "OrderService"},
            new String[] {"OrderService", "OrderRepository"},
            new String[] {"OrderService", "PricingRule"});

    public static AsyncNodeAction<MigrationState> discovery() {
        return node_async(state -> {
            // Phase 1: if a real project path is supplied, parse it with JavaParser;
            // otherwise fall back to the built-in sample so the spine runs offline.
            String projectPath = state.<String>value("project_path").orElse(null);
            List<String> modules;
            List<String[]> edges;
            List<String> messaging;
            String source;
            if (projectPath != null && !projectPath.isBlank()) {
                ProjectAnalysis analysis = JavaProjectAnalyzer.analyze(Path.of(projectPath));
                modules = analysis.moduleIds();
                edges = analysis.dependencyEdges();
                messaging = new ArrayList<>(analysis.messagingSystems());
                source = "JavaParser(" + projectPath + ")";
            } else {
                modules = state.<List<String>>value("module_inventory").orElse(DEFAULT_MODULES);
                edges = state.<List<String[]>>value("dependency_edges").orElse(DEFAULT_EDGES);
                messaging = List.of();
                source = "sample";
            }
            List<String> order = TopologicalSort.leafFirst(modules, edges);
            return Map.of(
                    "phase", Phase.ANALYSIS.name(),
                    "module_inventory", modules,
                    "dependency_edges", edges,
                    "topo_order", order,
                    "messaging_systems", messaging,
                    "log", List.of("discovery[" + source + "]: " + modules.size()
                            + " modules, translation order = " + order));
        });
    }

    /**
     * Analysis Agent (BSG builder). Populates the Behavioral Specification Graph
     * from the discovered modules, then hands it to the human review gate.
     */
    public static AsyncNodeAction<MigrationState> analysis(BsgProducer producer) {
        return node_async(state -> {
            String projectId = state.projectId().orElse("unknown");
            List<String> modules = state.topoOrder().isEmpty()
                    ? state.<List<String>>value("module_inventory").orElse(List.of())
                    : state.topoOrder();
            String projectPath = state.<String>value("project_path").orElse(null);

            BsgGraph bsg = producer.produce(projectId, modules, projectPath);
            return Map.of(
                    "phase", Phase.BSG_REVIEW.name(),
                    "bsg", bsg,
                    "bsg_pending", bsg.pendingCount(),
                    "log", List.of("analysis: extracted " + bsg.nodes().size()
                            + " BSG nodes (" + bsg.pendingCount() + " pending review)"));
        });
    }

    public static AsyncNodeAction<MigrationState> review() {
        return node_async(state -> {
            String decision = state.reviewDecision().orElse("PENDING");
            return Map.of("log", List.of("review: human decision = " + decision));
        });
    }

    /** Post-BSG-gate: records the decision. The conditional edge does the routing. */
    public static AsyncNodeAction<MigrationState> bsgGate() {
        return node_async(state -> {
            String decision = state.reviewDecision().orElse("PENDING");
            return Map.of("log", List.of("bsg-gate: BSG " + decision));
        });
    }

    /**
     * Architecture Agent (product doc §5, agent #3). Turns the approved BSG into a
     * target architecture (layers, boundaries, phases), then hands it to gate #2.
     */
    public static AsyncNodeAction<MigrationState> architecture(ArchitectureProducer producer) {
        return node_async(state -> {
            BsgGraph bsg = state.bsg().orElse(new BsgGraph("unknown", 1, List.of(), List.of()));
            List<String> order = state.topoOrder();
            String stack = state.<String>value("target_stack").orElse("JAVA_21_SPRING_BOOT");
            ArchitecturePlan plan = producer.produce(bsg, order, stack);
            return Map.of(
                    "phase", Phase.ARCH_REVIEW.name(),
                    "architecture", plan,
                    "log", List.of("architecture: " + plan.moduleMappings().size()
                            + " modules mapped, " + plan.microservices().size() + " service(s), "
                            + plan.phases().size() + " phases"));
        });
    }

    /** Post-architecture-gate: on approval the run is ready to build. */
    public static AsyncNodeAction<MigrationState> archGate() {
        return node_async(state -> {
            String decision = state.reviewDecision().orElse("PENDING");
            String phase = "APPROVED".equals(decision) ? Phase.BUILD.name() : Phase.ARCH_REVIEW.name();
            return Map.of(
                    "phase", phase,
                    "log", List.of("arch-gate: architecture " + decision + " → " + phase));
        });
    }

    /**
     * Build: Transformation Agent (#4) + Test Generation Agent (#5). Generates
     * target code + tests from the approved BSG + architecture and compile-checks
     * them in the sandbox. Terminal for the Phase 3 pipeline (→ DELIVERY).
     */
    public static AsyncNodeAction<MigrationState> build(TransformationProducer producer) {
        return node_async(state -> {
            BsgGraph bsg = state.bsg().orElse(new BsgGraph("unknown", 1, List.of(), List.of()));
            ArchitecturePlan arch = state.architecture()
                    .orElse(new ArchitecturePlan("JAVA_21_SPRING_BOOT", List.of(), List.of(), List.of()));
            TransformationResult result = producer.produce(bsg, arch, state.topoOrder());
            int attempt = state.buildRetries() + 1;
            return Map.of(
                    "phase", Phase.BUILD.name(),
                    "transformation", result,
                    "build_retries", attempt,
                    "log", List.of("build[attempt " + attempt + "]: " + result.modules().size()
                            + " modules transformed ("
                            + (result.allCompiled() ? "all compiled" : "compile issues") + "), "
                            + result.tests().size() + " tests generated"));
        });
    }

    /**
     * Validation Agent (#6): compile + BSG coverage check. On failure the graph's
     * bounded feedback loop routes back to {@code build} for a targeted retry.
     */
    public static AsyncNodeAction<MigrationState> validation(ValidationProducer producer) {
        return node_async(state -> {
            BsgGraph bsg = state.bsg().orElse(new BsgGraph("unknown", 1, List.of(), List.of()));
            TransformationResult tr = state.transformation()
                    .orElse(new TransformationResult(List.of(), List.of(), false, List.of()));
            ValidationReport report = producer.validate(bsg, tr);
            return Map.of(
                    "phase", Phase.VALIDATION.name(),
                    "validation", report,
                    "log", List.of("validation: compile=" + report.compileOk()
                            + ", BSG coverage=" + report.coveragePercent() + "%, "
                            + (report.passed() ? "PASSED" : "FAILED " + report.issues())));
        });
    }

    /**
     * Hardening branch (§9): Security Agent + Cloud/DevOps Agent + Messaging Agent.
     * Runs on every validated migration.
     */
    public static AsyncNodeAction<MigrationState> hardening(HardeningProducer producer) {
        return node_async(state -> {
            ArchitecturePlan arch = state.architecture()
                    .orElse(new ArchitecturePlan("JAVA_21_SPRING_BOOT", List.of(), List.of(), List.of()));
            TransformationResult tr = state.transformation()
                    .orElse(new TransformationResult(List.of(), List.of(), false, List.of()));
            String path = state.<String>value("project_path").orElse(null);
            List<String> messaging = state.<List<String>>value("messaging_systems").orElse(List.of());
            HardeningResult result = producer.produce(arch, tr, path, messaging);
            return Map.of(
                    "phase", Phase.HARDENING.name(),
                    "hardening", result,
                    "log", List.of("hardening: " + result.security().findings().size()
                            + " security findings, DevOps bundle, "
                            + result.messaging().topics().size() + " Kafka topic(s)"));
        });
    }

    /** Terminal node: the run is validated, hardened and ready for delivery. */
    public static AsyncNodeAction<MigrationState> delivery() {
        return node_async(state -> Map.of(
                "phase", Phase.DELIVERY.name(),
                "log", List.of("delivery: migration validated and ready")));
    }
}

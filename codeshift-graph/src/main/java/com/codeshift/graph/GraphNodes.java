package com.codeshift.graph;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import com.codeshift.common.Phase;
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
            List<String> modules = state.<List<String>>value("module_inventory").orElse(DEFAULT_MODULES);
            List<String[]> edges = state.<List<String[]>>value("dependency_edges").orElse(DEFAULT_EDGES);
            List<String> order = TopologicalSort.leafFirst(modules, edges);
            return Map.of(
                    "phase", Phase.BSG_REVIEW.name(),
                    "module_inventory", modules,
                    "dependency_edges", edges,
                    "topo_order", order,
                    "log", List.of("discovery: " + modules.size()
                            + " modules, translation order = " + order));
        });
    }

    public static AsyncNodeAction<MigrationState> review() {
        return node_async(state -> {
            String decision = state.reviewDecision().orElse("PENDING");
            return Map.of("log", List.of("review: human decision = " + decision));
        });
    }

    public static AsyncNodeAction<MigrationState> finalizeNode() {
        return node_async(state -> {
            String decision = state.reviewDecision().orElse("PENDING");
            String phase = "APPROVED".equals(decision)
                    ? Phase.ARCHITECTURE.name() : Phase.DISCOVERY.name();
            return Map.of(
                    "phase", phase,
                    "log", List.of("finalize: routing to " + phase + " after decision " + decision));
        });
    }
}

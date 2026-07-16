package com.codeshift.agents;

import com.codeshift.bsg.PerformanceProducer;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.bsg.model.PerformanceReport;
import com.codeshift.bsg.model.PerformanceReport.Recommendation;
import com.codeshift.common.BsgNodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Performance Agent (product doc §9.2). Uses the BSG (data flows, node
 * types/locations) to surface optimisation opportunities: N+1 queries, caching,
 * virtual-thread conversion of blocking I/O, and async conversion of fire-and-
 * forget work. Deterministic heuristics over the specification.
 */
@Component
public class PerformanceAgent implements PerformanceProducer {

    @Override
    public PerformanceReport analyze(BsgGraph bsg) {
        List<Recommendation> recs = new ArrayList<>();

        // N+1: repository/data-flow rules that load per-entity.
        long dataFlows = bsg.nodes().stream()
                .filter(n -> n.nodeType() == BsgNodeType.DATA_FLOW
                        || loc(n).contains("repository") || text(n).contains("load")
                        || text(n).contains("find"))
                .count();
        if (dataFlows >= 1) {
            recs.add(new Recommendation("N_PLUS_ONE", "repository access",
                    "Data-flow rules load per entity; add Spring Data @EntityGraph or batch queries.",
                    "High — most common perf issue in migrated Java apps"));
        }

        for (BsgNode n : bsg.nodes()) {
            String t = text(n);
            if (t.contains("catalog") || t.contains("config") || t.contains("reference")
                    || t.contains("list all") || t.contains("read-only")) {
                recs.add(new Recommendation("CACHING", n.nodeRef(),
                        "Read-heavy rule '" + n.title() + "' — add a Redis cache (@Cacheable).",
                        "Medium — latency reduction on hot reads"));
            }
            if (t.contains("http") || t.contains("external") || t.contains("call") || t.contains("api")) {
                recs.add(new Recommendation("VIRTUAL_THREADS", n.nodeRef(),
                        "Blocking I/O in '" + n.title() + "' — convert to Java 21 virtual threads.",
                        "Medium — throughput under load"));
            }
            if (t.contains("email") || t.contains("sms") || t.contains("notification")
                    || t.contains("report") || t.contains("audit")) {
                recs.add(new Recommendation("ASYNC", n.nodeRef(),
                        "Fire-and-forget work in '" + n.title() + "' — make async (@Async + Kafka event).",
                        "Medium — faster request path"));
            }
        }
        return new PerformanceReport(recs);
    }

    private static String text(BsgNode n) {
        return (n.title() + " " + n.description()).toLowerCase(Locale.ROOT);
    }

    private static String loc(BsgNode n) {
        return n.sourceLocation() == null ? "" : n.sourceLocation().toLowerCase(Locale.ROOT);
    }
}

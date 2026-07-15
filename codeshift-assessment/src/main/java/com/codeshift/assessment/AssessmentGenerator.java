package com.codeshift.assessment;

import com.codeshift.parser.ProjectAnalysis;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a {@link ProjectAnalysis} into an {@link AssessmentReport}.
 *
 * <p>Deterministic and LLM-free. The estimates are transparent heuristics (the
 * product promises "get an assessment + pricing estimate in 10 minutes"): pricing
 * follows the published $50/kLOC model with a Growth-tier floor; effort and
 * complexity are simple, explainable functions of size and coupling.
 */
public final class AssessmentGenerator {

    private static final long PRICE_PER_KLOC_USD = 50;
    private static final long GROWTH_FLOOR_USD = 999;

    private AssessmentGenerator() {}

    public static AssessmentReport generate(String projectName, ProjectAnalysis analysis) {
        int loc = analysis.totalLinesOfCode();
        int modules = analysis.modules().size();
        int edges = analysis.dependencyEdges().size();

        double complexity = complexityScore(modules, edges, analysis.hasCycles());
        int effortDays = estimateEffortDays(loc, modules, analysis.hasCycles(),
                analysis.messagingSystems().size());
        long price = priceUsd(loc);
        String tier = suggestedTier(loc);

        return new AssessmentReport(
                projectName,
                modules,
                loc,
                edges,
                analysis.entryPointCount(),
                analysis.hasCycles(),
                List.copyOf(analysis.messagingSystems()),
                analysis.usesJavaxNamespace(),
                migrationSignals(analysis),
                round1(complexity),
                effortDays,
                price,
                tier,
                analysis.translationOrder());
    }

    private static List<String> migrationSignals(ProjectAnalysis a) {
        List<String> signals = new ArrayList<>();
        if (a.usesJavaxNamespace()) {
            signals.add("Uses the javax.* namespace — pre-Jakarta (Java 8 / Spring Boot 2.x). "
                    + "Migration to jakarta.* is required for Spring Boot 3+/Java 21.");
        }
        if (!a.messagingSystems().isEmpty()) {
            signals.add("Messaging detected (" + String.join(", ", a.messagingSystems())
                    + ") — candidate for the MQ→Kafka migration pillar.");
        }
        if (a.hasCycles()) {
            signals.add("Dependency cycles present — architecture review recommended before transformation.");
        }
        if (signals.isEmpty()) {
            signals.add("No high-risk migration signals detected in the import graph.");
        }
        return signals;
    }

    /** 0–100. Blends average coupling (edges per module) with a cycle penalty. */
    private static double complexityScore(int modules, int edges, boolean hasCycles) {
        if (modules == 0) {
            return 0.0;
        }
        double avgDegree = (double) edges / modules;
        double base = Math.min(80.0, avgDegree * 25.0);
        return Math.min(100.0, base + (hasCycles ? 20.0 : 0.0));
    }

    private static int estimateEffortDays(int loc, int modules, boolean hasCycles, int messaging) {
        double days = (loc / 1000.0) * 0.5   // ~half a day per kLOC
                + modules * 0.2               // per-module review overhead
                + messaging * 2.0             // each messaging system adds redesign
                + (hasCycles ? 5 : 0);
        return Math.max(1, (int) Math.ceil(days));
    }

    private static long priceUsd(int loc) {
        long raw = Math.round((loc / 1000.0) * PRICE_PER_KLOC_USD);
        return Math.max(GROWTH_FLOOR_USD, raw);
    }

    private static String suggestedTier(int loc) {
        if (loc <= 10_000) {
            return "Starter";
        }
        if (loc <= 100_000) {
            return "Growth";
        }
        return "Enterprise";
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}

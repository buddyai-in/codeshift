package com.codeshift.assessment;

import java.util.List;

/**
 * The free assessment report — CodeShift's top-of-funnel lead magnet (product
 * doc §15.1). Produced from a {@link com.codeshift.parser.ProjectAnalysis} with no
 * account and no LLM cost: upload → dependency graph + effort + a price estimate.
 */
public record AssessmentReport(
        String projectName,
        int moduleCount,
        int estimatedLoc,
        int dependencyEdgeCount,
        long entryPointCount,
        boolean hasCycles,
        List<String> messagingSystems,
        boolean usesJavaxNamespace,
        List<String> migrationSignals,
        double complexityScore,      // 0–100, higher = more entangled
        int estimatedEffortDays,
        long priceEstimateUsd,
        String suggestedTier,
        List<String> translationOrder) {}

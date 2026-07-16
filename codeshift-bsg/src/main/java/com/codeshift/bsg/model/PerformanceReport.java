package com.codeshift.bsg.model;

import java.io.Serializable;
import java.util.List;

/**
 * Performance Agent output (product doc §9.2): BSG-driven optimisation
 * recommendations — N+1 queries, caching opportunities, virtual-thread and async
 * conversions.
 */
public record PerformanceReport(List<Recommendation> recommendations) implements Serializable {

    public record Recommendation(String type, String target, String rationale, String estimatedImpact)
            implements Serializable {}
}

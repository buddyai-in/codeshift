package com.codeshift.parser;

import com.codeshift.common.TopologicalSort;
import java.util.List;
import java.util.Set;

/**
 * The output of Discovery: a module inventory, an inter-module dependency graph,
 * detected messaging systems, and migration signals — everything the assessment
 * report and the BSG skeleton are built from.
 */
public record ProjectAnalysis(
        List<SourceModule> modules,
        List<String[]> dependencyEdges,   // {from, to} where "from depends_on to"
        Set<String> messagingSystems,     // e.g. JMS, IBM_MQ, RABBITMQ, KAFKA, ACTIVEMQ
        boolean usesJavaxNamespace) {     // javax.* imports → pre-Jakarta migration signal

    public List<String> moduleIds() {
        return modules.stream().map(SourceModule::id).toList();
    }

    public int totalLinesOfCode() {
        return modules.stream().mapToInt(SourceModule::linesOfCode).sum();
    }

    public long entryPointCount() {
        return modules.stream().filter(SourceModule::entryPoint).count();
    }

    /** Leaf-first translation order (Kahn) — dependencies before dependents. */
    public List<String> translationOrder() {
        return TopologicalSort.leafFirst(moduleIds(), dependencyEdges);
    }

    public boolean hasCycles() {
        return TopologicalSort.hasCycle(moduleIds(), dependencyEdges);
    }
}

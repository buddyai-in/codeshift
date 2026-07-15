package com.codeshift.assessment;

import com.codeshift.parser.ProjectAnalysis;
import java.util.List;

/** A UI-friendly view of the dependency graph (for react-flow rendering). */
public record DependencyGraphView(List<Node> nodes, List<Edge> edges) {

    public record Node(String id, String label, String packageName, boolean entryPoint) {}

    public record Edge(String source, String target) {}

    public static DependencyGraphView from(ProjectAnalysis analysis) {
        List<Node> nodes = analysis.modules().stream()
                .map(m -> new Node(m.id(), m.simpleName(), m.packageName(), m.entryPoint()))
                .toList();
        List<Edge> edges = analysis.dependencyEdges().stream()
                .map(e -> new Edge(e[0], e[1]))
                .toList();
        return new DependencyGraphView(nodes, edges);
    }
}

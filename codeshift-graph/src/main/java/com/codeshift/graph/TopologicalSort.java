package com.codeshift.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Kahn's algorithm — leaf-first ordering for module translation. */
public final class TopologicalSort {

    private TopologicalSort() {}

    /**
     * Returns a leaf-first order so a module's dependencies are always processed
     * first. Eliminates ~30% of hallucinated-type failures (product doc §5.1) by
     * giving the Transformation Agent already-translated upstream modules as context.
     *
     * @param modules all module ids
     * @param edges   dependency edges where {@code edge[0] depends_on edge[1]}
     */
    public static List<String> leafFirst(List<String> modules, List<String[]> edges) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (String m : modules) {
            indegree.put(m, 0);
            dependents.put(m, new ArrayList<>());
        }
        for (String[] edge : edges) {
            String src = edge[0]; // depends on
            String dst = edge[1];
            dependents.computeIfAbsent(dst, k -> new ArrayList<>()).add(src);
            indegree.merge(src, 1, Integer::sum);
            indegree.putIfAbsent(dst, 0);
        }

        Deque<String> queue = new ArrayDeque<>();
        for (String m : modules) {
            if (indegree.getOrDefault(m, 0) == 0) {
                queue.add(m);
            }
        }
        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String node = queue.poll();
            order.add(node);
            for (String next : dependents.getOrDefault(node, List.of())) {
                indegree.merge(next, -1, Integer::sum);
                if (indegree.get(next) == 0) {
                    queue.add(next);
                }
            }
        }
        // Any leftover (cycle) appended deterministically so we never drop modules.
        for (String m : modules) {
            if (!order.contains(m)) {
                order.add(m);
            }
        }
        return order;
    }
}

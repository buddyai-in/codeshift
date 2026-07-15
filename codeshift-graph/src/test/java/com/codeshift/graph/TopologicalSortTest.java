package com.codeshift.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TopologicalSortTest {

    @Test
    void leafFirstOrder() {
        List<String> modules = List.of("A", "B", "C");
        List<String[]> edges = List.of(
                new String[] {"A", "B"}, // A depends_on B
                new String[] {"B", "C"}); // B depends_on C
        List<String> order = TopologicalSort.leafFirst(modules, edges);
        // Leaves (no deps) come first: C before B before A.
        assertThat(order.indexOf("C")).isLessThan(order.indexOf("B"));
        assertThat(order.indexOf("B")).isLessThan(order.indexOf("A"));
    }
}

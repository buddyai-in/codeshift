package com.codeshift.bsg.model;

import com.codeshift.common.BsgConfidence;
import com.codeshift.common.HumanStatus;
import java.io.Serializable;
import java.util.List;

/** A whole BSG version — what the Analysis Agent produces for human review. */
public record BsgGraph(
        String projectId,
        int versionNumber,
        List<BsgNode> nodes,
        List<BsgEdge> edges) implements Serializable {

    public long pendingCount() {
        return nodes.stream().filter(n -> n.humanStatus() == HumanStatus.PENDING).count();
    }

    /** Nodes that most need a human look (product doc: LOW → human review). */
    public List<BsgNode> lowConfidence() {
        return nodes.stream().filter(n -> n.confidence() == BsgConfidence.LOW).toList();
    }
}

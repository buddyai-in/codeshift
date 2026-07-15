package com.codeshift.bsgmcp;

import com.codeshift.bsg.BsgStore;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.common.HumanStatus;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * The BSG tools exposed over MCP. Each {@code @Tool} method becomes a callable
 * tool an agent can invoke; the descriptions are what the model sees.
 */
public class BsgToolService {

    private final BsgStore store;

    public BsgToolService(BsgStore store) {
        this.store = store;
    }

    @Tool(description = "Persist a full BSG version (nodes + edges). Returns the new version id.")
    public String saveBsgGraph(@ToolParam(description = "The BSG version to persist") BsgGraph graph) {
        return store.saveGraph(graph).toString();
    }

    @Tool(description = "Read a BSG version back (nodes + edges) for review or retrieval.")
    public BsgGraph getBsgVersion(@ToolParam(description = "BSG version UUID") String versionId) {
        return store.getVersion(UUID.fromString(versionId));
    }

    @Tool(description = "Record a human review decision on a node: APPROVED, REJECTED or MODIFIED.")
    public String setNodeStatus(
            @ToolParam(description = "BSG node UUID") String nodeId,
            @ToolParam(description = "APPROVED | REJECTED | MODIFIED | PENDING") String status) {
        store.setNodeStatus(UUID.fromString(nodeId), HumanStatus.valueOf(status));
        return "ok";
    }

    @Tool(description = "Approve a whole BSG version — the trust-boundary gate before transformation.")
    public String approveBsgVersion(
            @ToolParam(description = "BSG version UUID") String versionId,
            @ToolParam(description = "Reviewer identity") String reviewer) {
        store.approveVersion(UUID.fromString(versionId), reviewer);
        return "approved";
    }
}

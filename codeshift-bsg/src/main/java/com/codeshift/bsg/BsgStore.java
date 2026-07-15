package com.codeshift.bsg;

import com.codeshift.bsg.entity.BsgEdgeEntity;
import com.codeshift.bsg.entity.BsgNodeEntity;
import com.codeshift.bsg.entity.BsgVersionEntity;
import com.codeshift.bsg.model.BsgEdge;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.bsg.repo.BsgRepositories.BsgEdgeRepository;
import com.codeshift.bsg.repo.BsgRepositories.BsgNodeRepository;
import com.codeshift.bsg.repo.BsgRepositories.BsgVersionRepository;
import com.codeshift.common.HumanStatus;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * The BSG store — the concrete persistence behind the trust boundary. Agents and
 * the API touch the BSG only through here (and, over MCP, through the bsg-mcp
 * server which delegates to this store).
 *
 * <p>Declared as a bean by whichever app needs persistence (see the API's
 * {@code PersistenceConfig} and the bsg-mcp app) rather than component-scanned,
 * so the DB-less free-assessment funnel can boot without JPA.
 *
 * <p>Phase 0 covers: persist a full version, read it back, record a human review
 * decision, approve a version. Semantic search (pgvector) and version diffing
 * arrive in Phase 1/2 — the schema already carries the embedding column.
 */
public class BsgStore {

    private final BsgVersionRepository versions;
    private final BsgNodeRepository nodes;
    private final BsgEdgeRepository edges;

    public BsgStore(BsgVersionRepository versions, BsgNodeRepository nodes, BsgEdgeRepository edges) {
        this.versions = versions;
        this.nodes = nodes;
        this.edges = edges;
    }

    /** Persist a full BSG version (nodes + edges) transactionally; returns the version id. */
    @Transactional
    public UUID saveGraph(BsgGraph graph) {
        BsgVersionEntity version = new BsgVersionEntity();
        version.setProjectId(UUID.fromString(graph.projectId()));
        version.setVersionNumber(graph.versionNumber());
        version = versions.save(version);
        UUID versionId = version.getId();

        Map<String, UUID> refToId = new HashMap<>();
        for (BsgNode node : graph.nodes()) {
            BsgNodeEntity e = toEntity(node, versionId);
            e = nodes.save(e);
            refToId.put(node.nodeRef(), e.getId());
        }
        for (BsgEdge edge : graph.edges()) {
            UUID src = refToId.get(edge.sourceRef());
            UUID tgt = refToId.get(edge.targetRef());
            if (src != null && tgt != null) {
                BsgEdgeEntity e = new BsgEdgeEntity();
                e.setVersionId(versionId);
                e.setSourceNodeId(src);
                e.setTargetNodeId(tgt);
                e.setEdgeType(edge.edgeType());
                edges.save(e);
            }
        }
        return versionId;
    }

    /** Read a BSG version back (nodes + edges) for review or retrieval. */
    @Transactional(readOnly = true)
    public BsgGraph getVersion(UUID versionId) {
        BsgVersionEntity v = versions.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown BSG version " + versionId));
        List<BsgNode> nodeModels = nodes.findByVersionIdOrderByNodeRef(versionId).stream()
                .map(BsgStore::toModel).toList();
        List<BsgEdge> edgeModels = edges.findByVersionId(versionId).stream()
                .map(e -> new BsgEdge(e.getSourceNodeId().toString(),
                        e.getTargetNodeId().toString(), e.getEdgeType()))
                .toList();
        return new BsgGraph(v.getProjectId().toString(), v.getVersionNumber(), nodeModels, edgeModels);
    }

    /** Record a human review decision on a node (APPROVED / REJECTED / MODIFIED). */
    @Transactional
    public void setNodeStatus(UUID nodeId, HumanStatus status) {
        BsgNodeEntity e = nodes.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown BSG node " + nodeId));
        e.setHumanStatus(status);
        nodes.save(e);
    }

    /** Approve a whole BSG version — the trust-boundary gate before transformation. */
    @Transactional
    public void approveVersion(UUID versionId, String reviewer) {
        BsgVersionEntity v = versions.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown BSG version " + versionId));
        v.setApproved(true);
        v.setApprovedBy(reviewer);
        v.setApprovedAt(OffsetDateTime.now());
        versions.save(v);
    }

    private static BsgNodeEntity toEntity(BsgNode node, UUID versionId) {
        BsgNodeEntity e = new BsgNodeEntity();
        e.setVersionId(versionId);
        e.setNodeRef(node.nodeRef());
        e.setNodeType(node.nodeType());
        e.setTitle(node.title());
        e.setDescription(node.description());
        e.setSourceLocation(node.sourceLocation());
        e.setConfidence(node.confidence());
        e.setHumanStatus(node.humanStatus());
        e.setOrigin(node.origin());
        e.setTargetCodeLocation(node.targetCodeLocation());
        e.setTestCoverage(node.testCoverage());
        return e;
    }

    private static BsgNode toModel(BsgNodeEntity e) {
        return new BsgNode(e.getNodeRef(), e.getNodeType(), e.getTitle(), e.getDescription(),
                e.getSourceLocation(), e.getConfidence(), e.getHumanStatus(), e.getOrigin(),
                e.getTargetCodeLocation(), e.isTestCoverage());
    }
}

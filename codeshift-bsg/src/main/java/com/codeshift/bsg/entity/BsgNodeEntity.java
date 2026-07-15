package com.codeshift.bsg.entity;

import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import com.codeshift.common.BsgOrigin;
import com.codeshift.common.HumanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * One behavioral specification row. The {@code embedding vector(1536)} column
 * exists in the schema (Flyway V2) for semantic retrieval but is intentionally
 * not mapped here yet — it is populated in Phase 1 alongside the Analysis Agent.
 */
@Entity
@Table(name = "bsg_nodes")
public class BsgNodeEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "node_ref", nullable = false)
    private String nodeRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false)
    private BsgNodeType nodeType;

    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "source_location")
    private String sourceLocation;

    @Enumerated(EnumType.STRING)
    private BsgConfidence confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "human_status")
    private HumanStatus humanStatus;

    @Enumerated(EnumType.STRING)
    private BsgOrigin origin;

    @Column(name = "target_code_location")
    private String targetCodeLocation;

    @Column(name = "test_coverage")
    private boolean testCoverage;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVersionId() {
        return versionId;
    }

    public void setVersionId(UUID versionId) {
        this.versionId = versionId;
    }

    public String getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(String nodeRef) {
        this.nodeRef = nodeRef;
    }

    public BsgNodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(BsgNodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public BsgConfidence getConfidence() {
        return confidence;
    }

    public void setConfidence(BsgConfidence confidence) {
        this.confidence = confidence;
    }

    public HumanStatus getHumanStatus() {
        return humanStatus;
    }

    public void setHumanStatus(HumanStatus humanStatus) {
        this.humanStatus = humanStatus;
    }

    public BsgOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(BsgOrigin origin) {
        this.origin = origin;
    }

    public String getTargetCodeLocation() {
        return targetCodeLocation;
    }

    public void setTargetCodeLocation(String targetCodeLocation) {
        this.targetCodeLocation = targetCodeLocation;
    }

    public boolean isTestCoverage() {
        return testCoverage;
    }

    public void setTestCoverage(boolean testCoverage) {
        this.testCoverage = testCoverage;
    }
}

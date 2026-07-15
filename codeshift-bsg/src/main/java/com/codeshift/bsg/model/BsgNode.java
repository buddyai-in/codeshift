package com.codeshift.bsg.model;

import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import com.codeshift.common.BsgOrigin;
import com.codeshift.common.HumanStatus;

/**
 * A single behavioral specification (product doc §4.1 / §4.3).
 *
 * <p>These records are the <em>only</em> way business rules cross the AI boundary:
 * the Analysis Agent emits typed {@code BsgNode}s (via structured output), never
 * free text — so every rule is validatable, human-reviewable, and auditable.
 */
public record BsgNode(
        String nodeRef,          // stable ref, e.g. "BSG-042"
        BsgNodeType nodeType,
        String title,
        String description,      // plain English a business analyst can validate
        String sourceLocation,   // e.g. "OrderService.java:118-146"
        BsgConfidence confidence,
        HumanStatus humanStatus,
        BsgOrigin origin,
        String targetCodeLocation,
        boolean testCoverage) {

    /** Convenience factory for a freshly-extracted, pending rule. */
    public static BsgNode extracted(
            String nodeRef, BsgNodeType type, String title, String description,
            String sourceLocation, BsgConfidence confidence) {
        return new BsgNode(nodeRef, type, title, description, sourceLocation,
                confidence, HumanStatus.PENDING, BsgOrigin.MIGRATED, null, false);
    }
}

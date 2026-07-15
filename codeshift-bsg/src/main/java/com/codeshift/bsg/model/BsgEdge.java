package com.codeshift.bsg.model;

/** A relationship between two BSG nodes. */
public record BsgEdge(String sourceRef, String targetRef, String edgeType) {
    // edgeType: depends_on | produces | validates | overrides | triggers
}

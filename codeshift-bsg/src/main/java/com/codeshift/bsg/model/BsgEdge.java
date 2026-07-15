package com.codeshift.bsg.model;

import java.io.Serializable;

/** A relationship between two BSG nodes. */
public record BsgEdge(String sourceRef, String targetRef, String edgeType)
        implements Serializable {
    // edgeType: depends_on | produces | validates | overrides | triggers
}

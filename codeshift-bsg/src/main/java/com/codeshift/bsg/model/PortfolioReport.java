package com.codeshift.bsg.model;

import java.io.Serializable;
import java.util.List;

/**
 * Portfolio Intelligence (product doc §10): a CIO-level view aggregating BSG
 * health across every application — the same BSG pipeline, rolled up.
 */
public record PortfolioReport(int projectCount, int avgDebtScore, List<ProjectHealth> projects)
        implements Serializable {

    public record ProjectHealth(String projectId, String name, int versionCount, int nodeCount,
            int debtScore, String debtGrade) implements Serializable {}
}

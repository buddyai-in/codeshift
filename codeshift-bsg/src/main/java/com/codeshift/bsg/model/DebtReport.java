package com.codeshift.bsg.model;

import java.io.Serializable;
import java.util.List;

/**
 * Technical Debt Intelligence output (product doc §8): a BSG-aware debt score,
 * the human-readable signals behind it, and the delta from the previous version.
 * Unlike code-smell tools, debt is ranked by BSG business impact.
 */
public record DebtReport(
        int debtScore,        // 0 (clean) – 100 (heavy)
        String grade,         // A–F
        List<String> signals,
        DebtDelta delta) implements Serializable {

    /** What changed since the previous BSG version (the delta-BSG analysis). */
    public record DebtDelta(List<String> addedRefs, List<String> removedRefs, int unchanged)
            implements Serializable {}
}

package com.codeshift.compliance;

import java.io.Serializable;
import java.util.List;

/**
 * A compliance report pack: for a given standard, which required controls the
 * migrated BSG reflects and which are gaps needing remediation.
 *
 * @param standard        the standard's enum name
 * @param reference       the standard's human reference (e.g. "PCI-DSS v4.0")
 * @param totalControls   controls in the pack
 * @param coveredControls controls the BSG reflects
 * @param score           coveredControls / totalControls, 0..100
 * @param passed          true when no control is a gap
 * @param results         per-control coverage detail
 */
public record ComplianceReport(String standard, String reference, int totalControls,
        int coveredControls, int score, boolean passed, List<ControlResult> results)
        implements Serializable {

    /** Coverage of one control: whether it's reflected, which BSG nodes matched, and the gap fix. */
    public record ControlResult(String controlId, String title, boolean covered,
            List<String> matchedNodeRefs, String remediation) implements Serializable {}
}

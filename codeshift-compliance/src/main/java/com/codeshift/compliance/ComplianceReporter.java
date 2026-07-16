package com.codeshift.compliance;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Checks a migrated BSG against a compliance control pack — the "compliance report
 * pack" deliverable. Deterministic and offline: a control is covered when any BSG
 * node's text carries one of the control's signal keywords, so gaps are surfaced
 * for remediation rather than assumed clean.
 */
public final class ComplianceReporter {

    private ComplianceReporter() {}

    public static ComplianceReport assess(BsgGraph bsg, ComplianceStandard standard) {
        List<ComplianceControl> controls = ComplianceTemplates.controlsFor(standard);
        List<ComplianceReport.ControlResult> results = new ArrayList<>();
        int covered = 0;

        for (ComplianceControl control : controls) {
            List<String> matched = matchingNodeRefs(bsg, control);
            boolean isCovered = !matched.isEmpty();
            if (isCovered) {
                covered++;
            }
            results.add(new ComplianceReport.ControlResult(control.id(), control.title(),
                    isCovered, matched, isCovered ? null : control.remediation()));
        }

        int total = controls.size();
        int score = total == 0 ? 100 : (int) Math.round(100.0 * covered / total);
        boolean passed = covered == total;
        return new ComplianceReport(standard.name(), standard.reference(), total, covered,
                score, passed, results);
    }

    private static List<String> matchingNodeRefs(BsgGraph bsg, ComplianceControl control) {
        List<String> refs = new ArrayList<>();
        for (BsgNode node : bsg.nodes()) {
            String haystack = text(node);
            for (String signal : control.signals()) {
                if (haystack.contains(signal)) {
                    refs.add(node.nodeRef());
                    break;
                }
            }
        }
        return refs;
    }

    private static String text(BsgNode node) {
        StringBuilder sb = new StringBuilder();
        append(sb, node.title());
        append(sb, node.description());
        append(sb, node.sourceLocation());
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private static void append(StringBuilder sb, String s) {
        if (s != null) {
            sb.append(' ').append(s);
        }
    }
}

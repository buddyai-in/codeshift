package com.codeshift.agents;

import com.codeshift.bsg.DebtProducer;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.bsg.model.DebtReport;
import com.codeshift.bsg.model.DebtReport.DebtDelta;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.HumanStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Technical Debt Intelligence (product doc §8). Scores debt from the BSG itself —
 * LOW-confidence rules, unreviewed rules, and growth — so debt is ranked by
 * business impact, not raw code smells. Runs a delta-BSG analysis against the
 * previous version (the continuous-monitoring signal).
 *
 * <p>Deterministic and BSG-aware; AI-debt fingerprinting (pgvector similarity to
 * existing nodes) is the Phase 5+ enhancement on top of this baseline.
 */
@Component
public class DebtAgent implements DebtProducer {

    @Override
    public DebtReport analyze(BsgGraph current, BsgGraph previous) {
        List<BsgNode> nodes = current.nodes();
        int total = Math.max(1, nodes.size());

        long low = nodes.stream().filter(n -> n.confidence() == BsgConfidence.LOW).count();
        long pending = nodes.stream().filter(n -> n.humanStatus() == HumanStatus.PENDING).count();
        long uncovered = nodes.stream().filter(n -> !n.testCoverage()).count();

        int lowPct = (int) (low * 100 / total);
        int pendingPct = (int) (pending * 100 / total);
        int uncoveredPct = (int) (uncovered * 100 / total);

        // Debt ranked by BSG impact: low confidence dominates, then unreviewed, then coverage.
        int score = Math.min(100, (int) Math.round(lowPct * 0.5 + pendingPct * 0.3 + uncoveredPct * 0.2));

        List<String> signals = new ArrayList<>();
        if (lowPct > 0) {
            signals.add(lowPct + "% of business rules are LOW confidence — highest-impact debt.");
        }
        if (pendingPct > 0) {
            signals.add(pendingPct + "% of rules are unreviewed (PENDING).");
        }
        if (uncoveredPct > 0) {
            signals.add(uncoveredPct + "% of rules lack test coverage.");
        }
        if (signals.isEmpty()) {
            signals.add("No BSG-level debt signals — rules are reviewed, covered and high-confidence.");
        }

        return new DebtReport(score, grade(score), signals, delta(current, previous));
    }

    private static DebtDelta delta(BsgGraph current, BsgGraph previous) {
        Set<String> curr = current.nodes().stream().map(BsgNode::nodeRef).collect(Collectors.toSet());
        Set<String> prev = previous == null ? Set.of()
                : previous.nodes().stream().map(BsgNode::nodeRef).collect(Collectors.toSet());
        List<String> added = curr.stream().filter(r -> !prev.contains(r)).sorted().toList();
        List<String> removed = prev.stream().filter(r -> !curr.contains(r)).sorted().toList();
        int unchanged = (int) curr.stream().filter(prev::contains).count();
        return new DebtDelta(added, removed, unchanged);
    }

    private static String grade(int score) {
        if (score < 20) {
            return "A";
        }
        if (score < 40) {
            return "B";
        }
        if (score < 60) {
            return "C";
        }
        if (score < 80) {
            return "D";
        }
        return "F";
    }
}

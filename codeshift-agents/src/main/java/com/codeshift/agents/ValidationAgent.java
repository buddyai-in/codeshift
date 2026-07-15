package com.codeshift.agents;

import com.codeshift.bsg.ValidationProducer;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.bsg.model.TransformationResult;
import com.codeshift.bsg.model.ValidationReport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validation Agent (product doc §5, agent #6). Checks that the generated code
 * compiled and measures BSG coverage: how many BSG nodes are backed by generated
 * code (module refs) and/or a generated test. When validation fails, the graph's
 * bounded feedback loop routes back to Transformation for a targeted retry.
 *
 * <p>The full Environment‑in‑the‑Loop dual‑run (identical inputs to both systems)
 * plugs in behind this same port once a runnable legacy image is available; the
 * compile + coverage checks are the deterministic baseline.
 */
@Component
public class ValidationAgent implements ValidationProducer {

    private static final int COVERAGE_THRESHOLD = 50;

    @Override
    public ValidationReport validate(BsgGraph bsg, TransformationResult transformation) {
        List<String> issues = new ArrayList<>();

        boolean compileOk = transformation.allCompiled();
        if (!compileOk) {
            issues.add("Not all generated modules compiled: " + transformation.diagnostics());
        }

        // A BSG node is "covered" if a transformed module references it or a test targets it.
        Set<String> coveredRefs = new HashSet<>();
        transformation.modules().forEach(m -> coveredRefs.addAll(m.bsgRuleRefs()));
        transformation.tests().forEach(t -> coveredRefs.add(t.bsgRuleRef()));

        int total = bsg.nodes().size();
        int covered = (int) bsg.nodes().stream()
                .map(BsgNode::nodeRef).filter(coveredRefs::contains).count();
        int coveragePercent = total == 0 ? 100 : (covered * 100) / total;

        if (coveragePercent < COVERAGE_THRESHOLD) {
            issues.add("BSG coverage " + coveragePercent + "% below threshold " + COVERAGE_THRESHOLD + "%");
        }

        boolean passed = compileOk && coveragePercent >= COVERAGE_THRESHOLD;
        return new ValidationReport(compileOk, total, covered, coveragePercent, passed, issues);
    }
}

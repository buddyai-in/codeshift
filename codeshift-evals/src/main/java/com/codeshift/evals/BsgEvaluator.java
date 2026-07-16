package com.codeshift.evals;

import com.codeshift.bsg.BsgProducer;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scores a {@link BsgProducer} against a golden corpus — the mechanism behind the
 * "eval suite as release gate" discipline. This is what lets "LLM-agnostic" be real:
 * you certify a producer/provider on the corpus before enabling it, and a
 * prompt/model change that regresses extraction fails the gate instead of shipping.
 *
 * <p>Deterministic and offline: it drives the producer directly and compares covered
 * source locations, so it runs in {@code mvn verify} with no keys and no network.
 */
public final class BsgEvaluator {

    private BsgEvaluator() {}

    /** Run the producer on a golden case and score its BSG coverage. */
    public static EvalScore evaluate(BsgProducer producer, GoldenCase golden) {
        BsgGraph bsg = producer.produce(golden.projectId(), golden.moduleIds(), null);
        return score(golden, bsg);
    }

    /** Score an already-produced BSG against the golden case (no producer call). */
    public static EvalScore score(GoldenCase golden, BsgGraph bsg) {
        Set<String> expected = golden.expectedSourceLocations();
        Set<String> produced = new HashSet<>();
        for (BsgNode n : bsg.nodes()) {
            if (n.sourceLocation() != null && !n.sourceLocation().isBlank()) {
                produced.add(n.sourceLocation());
            }
        }

        Set<String> matchedSet = new HashSet<>(expected);
        matchedSet.retainAll(produced);
        int matched = matchedSet.size();

        double precision = produced.isEmpty() ? 0.0 : (double) matched / produced.size();
        double recall = expected.isEmpty() ? 1.0 : (double) matched / expected.size();
        double f1 = (precision + recall) == 0.0
                ? 0.0 : 2 * precision * recall / (precision + recall);

        return new EvalScore(golden.name(), expected.size(), produced.size(), matched,
                precision, recall, f1);
    }

    /** Mean F1 across a corpus — the single number a release gate thresholds on. */
    public static double meanF1(BsgProducer producer, List<GoldenCase> corpus) {
        if (corpus.isEmpty()) {
            return 1.0;
        }
        double sum = 0.0;
        for (GoldenCase c : corpus) {
            sum += evaluate(producer, c).f1();
        }
        return sum / corpus.size();
    }
}

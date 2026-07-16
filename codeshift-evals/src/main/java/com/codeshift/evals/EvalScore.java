package com.codeshift.evals;

/**
 * The score of one BSG extraction against a {@link GoldenCase}: precision, recall
 * and F1 over covered behavioral units, plus the raw counts for reporting.
 *
 * @param caseName  the golden case scored
 * @param expected  number of expected units
 * @param produced  number of distinct units the producer covered
 * @param matched   expected units the producer actually covered
 * @param precision matched / produced (0..1)
 * @param recall    matched / expected (0..1)
 * @param f1        harmonic mean of precision and recall (0..1)
 */
public record EvalScore(String caseName, int expected, int produced, int matched,
        double precision, double recall, double f1) {
}

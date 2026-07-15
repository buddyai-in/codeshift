package com.codeshift.bsg.model;

import java.io.Serializable;
import java.util.List;

/**
 * Validation Agent output (product doc §5, agent #6): did the generated code
 * compile, and how much of the BSG is covered by generated code + tests. Failures
 * feed the bounded feedback loop back to Transformation.
 */
public record ValidationReport(
        boolean compileOk,
        int bsgNodeCount,
        int coveredNodeCount,
        int coveragePercent,
        boolean passed,
        List<String> issues) implements Serializable {}

package com.codeshift.assessment;

/** The full assessment payload for clients: the report plus the dependency graph. */
public record AssessmentResult(AssessmentReport report, DependencyGraphView graph) {}

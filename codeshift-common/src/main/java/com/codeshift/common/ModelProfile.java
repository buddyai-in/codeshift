package com.codeshift.common;

/**
 * Capability tiers the model gateway resolves to concrete provider models.
 * Agents ask for a profile, never a provider — this is what keeps CodeShift
 * LLM-agnostic.
 */
public enum ModelProfile {
    /** Deep reasoning, long context: Analysis (BSG), Architecture, Portfolio. */
    REASONING,
    /** Code quality + structured output: Transformation, Test-Gen, DataShift. */
    CODEGEN,
    /** Throughput and price: Discovery classify, secrets triage, routing. */
    CHEAP,
    /** Embeddings for retrieval over code + BSG. */
    EMBED
}

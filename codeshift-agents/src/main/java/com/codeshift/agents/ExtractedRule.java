package com.codeshift.agents;

/**
 * The shape the LLM returns for each behavioral rule (Spring AI structured output
 * binds JSON to this record). Kept as plain strings so the model can't produce an
 * unbindable enum value; the Analysis Agent maps these to typed {@code BsgNode}s.
 */
public record ExtractedRule(
        String nodeRef,          // e.g. "BSG-001"
        String nodeType,         // BusinessRule | DataFlow | StateTransition | ExternalContract | EdgeCase | ImplicitRule | MessagingContract
        String title,
        String description,      // plain English a business analyst can validate
        String sourceLocation,   // e.g. "OrderService.java"
        String confidence) {}    // HIGH | MEDIUM | LOW

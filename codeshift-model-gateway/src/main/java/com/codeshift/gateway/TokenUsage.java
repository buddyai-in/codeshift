package com.codeshift.gateway;

/** Accumulated token usage + estimated cost for a run or agent task. */
public record TokenUsage(long inputTokens, long outputTokens, double costUsd) {

    public static TokenUsage zero() {
        return new TokenUsage(0, 0, 0.0);
    }

    public TokenUsage plus(TokenUsage other) {
        return new TokenUsage(
                inputTokens + other.inputTokens,
                outputTokens + other.outputTokens,
                costUsd + other.costUsd);
    }
}

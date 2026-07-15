package com.codeshift.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CostEstimatorTest {

    @Test
    void knownModelUsesItsPrice() {
        // 1M in + 1M out on sonnet pricing (3 / 15 per Mtok) = 18.0
        assertThat(CostEstimator.estimate("anthropic:claude-sonnet-4-6", 1_000_000, 1_000_000))
                .isEqualTo(18.0);
    }

    @Test
    void unknownModelUsesDefault() {
        assertThat(CostEstimator.estimate("some:unknown-model", 1_000_000, 0)).isEqualTo(3.0);
    }

    @Test
    void tokenUsageAccumulates() {
        TokenUsage total = TokenUsage.zero()
                .plus(new TokenUsage(100, 50, 0.01))
                .plus(new TokenUsage(200, 25, 0.02));
        assertThat(total.inputTokens()).isEqualTo(300);
        assertThat(total.outputTokens()).isEqualTo(75);
        assertThat(total.costUsd()).isEqualTo(0.03);
    }
}

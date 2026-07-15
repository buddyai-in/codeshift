package com.codeshift.gateway;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estimates USD cost from token counts. Feeds the per-project budget guardrails
 * and the analytics/agent-costs surface described in the architecture.
 *
 * <p>Indicative prices (USD per 1M tokens). Real prices belong in config / the DB;
 * this table only powers local estimates. Unknown models use a conservative default.
 */
public final class CostEstimator {

    private record Price(double inPerMTok, double outPerMTok) {}

    private static final Map<String, Price> PRICE_PER_MTOK = new LinkedHashMap<>();
    private static final Price DEFAULT_PRICE = new Price(3.0, 15.0);

    static {
        PRICE_PER_MTOK.put("claude-opus-4-8", new Price(15.0, 75.0));
        PRICE_PER_MTOK.put("claude-sonnet-4-6", new Price(3.0, 15.0));
        PRICE_PER_MTOK.put("claude-haiku-4-5-20251001", new Price(0.80, 4.0));
        PRICE_PER_MTOK.put("gpt-4.1", new Price(2.5, 10.0));
        PRICE_PER_MTOK.put("gpt-4.1-mini", new Price(0.40, 1.6));
    }

    private CostEstimator() {}

    public static double estimate(String model, long inputTokens, long outputTokens) {
        Price price = DEFAULT_PRICE;
        for (Map.Entry<String, Price> e : PRICE_PER_MTOK.entrySet()) {
            if (model != null && model.contains(e.getKey())) {
                price = e.getValue();
                break;
            }
        }
        return (inputTokens / 1_000_000.0) * price.inPerMTok()
                + (outputTokens / 1_000_000.0) * price.outPerMTok();
    }

    public static TokenUsage usage(String model, long inputTokens, long outputTokens) {
        return new TokenUsage(inputTokens, outputTokens, estimate(model, inputTokens, outputTokens));
    }
}

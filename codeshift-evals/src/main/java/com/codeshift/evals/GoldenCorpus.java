package com.codeshift.evals;

import java.util.List;
import java.util.Set;

/**
 * The built-in BSG-extraction golden corpus. Small, deterministic, offline — the
 * baseline every producer/provider must clear before it ships. Grow this as real
 * migrations surface behaviors worth pinning.
 */
public final class GoldenCorpus {

    private GoldenCorpus() {}

    public static List<GoldenCase> defaultCases() {
        return List.of(
                new GoldenCase(
                        "orders-service",
                        "eval-orders",
                        List.of("OrderController", "OrderService", "OrderRepository", "PricingRule"),
                        Set.of("OrderController", "OrderService", "OrderRepository", "PricingRule")),
                new GoldenCase(
                        "billing-service",
                        "eval-billing",
                        List.of("InvoiceService", "TaxCalculator", "LedgerRepository"),
                        Set.of("InvoiceService", "TaxCalculator", "LedgerRepository")));
    }
}

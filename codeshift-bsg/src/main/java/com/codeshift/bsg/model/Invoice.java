package com.codeshift.bsg.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * A usage-based invoice for a tenant: one line item per project (aggregated token
 * spend), a total, and the payment intent from the configured provider. Generated
 * deterministically from persisted usage events — the metering-to-billing bridge.
 *
 * @param orgId    the billed tenant
 * @param currency ISO currency code (USD)
 * @param lineItems per-project aggregated spend
 * @param totalUsd  sum of the line items
 * @param payment   the provider's payment intent (dev/manual until a real gateway is wired)
 */
public record Invoice(String orgId, String currency, List<LineItem> lineItems,
        BigDecimal totalUsd, PaymentIntent payment) implements Serializable {

    /** One project's aggregated usage on the invoice. */
    public record LineItem(String projectId, String projectName, long inputTokens,
            long outputTokens, int calls, BigDecimal amountUsd) implements Serializable {}

    /**
     * A payment intent from a {@code PaymentProvider}. {@code status} is provider-defined
     * (e.g. MANUAL_PENDING for the dev provider, or a gateway's created/authorized state).
     */
    public record PaymentIntent(String id, String provider, String status,
            BigDecimal amountUsd, String checkoutRef) implements Serializable {}
}

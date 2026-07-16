package com.codeshift.bsg;

import com.codeshift.bsg.model.Invoice.PaymentIntent;
import java.math.BigDecimal;

/**
 * Port for a payment gateway (Razorpay in production). Kept behind an interface so
 * the platform bills without a hard dependency on any one provider, and so a
 * dev/manual implementation works with zero external integration.
 */
public interface PaymentProvider {

    /** Create a payment intent for a tenant invoice; never charges money by itself. */
    PaymentIntent createIntent(String orgId, BigDecimal amountUsd, String description);
}

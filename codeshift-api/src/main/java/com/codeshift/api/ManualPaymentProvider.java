package com.codeshift.api;

import com.codeshift.bsg.PaymentProvider;
import com.codeshift.bsg.model.Invoice.PaymentIntent;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Dev/manual {@link PaymentProvider}: issues a payment intent marked
 * {@code MANUAL_PENDING} without contacting any gateway. It's the honest default
 * until a real Razorpay integration (API key + webhook) is wired in — the invoice
 * flow works end-to-end, the charge is simply settled out of band.
 */
@Component
public class ManualPaymentProvider implements PaymentProvider {

    @Override
    public PaymentIntent createIntent(String orgId, BigDecimal amountUsd, String description) {
        String id = "pi_manual_" + UUID.randomUUID().toString().substring(0, 12);
        return new PaymentIntent(id, "manual", "MANUAL_PENDING", amountUsd, "manual://settle/" + id);
    }
}

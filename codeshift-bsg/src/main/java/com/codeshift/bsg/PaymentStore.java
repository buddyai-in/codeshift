package com.codeshift.bsg;

import com.codeshift.bsg.entity.PaymentEntity;
import com.codeshift.bsg.repo.PaymentRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists payments through their lifecycle: created at checkout, then advanced to
 * PAID/FAILED by the provider's (Razorpay) webhook. Correlated by a provider-agnostic
 * {@code reference} the webhook echoes back.
 */
public class PaymentStore {

    private final PaymentRepository payments;

    public PaymentStore(PaymentRepository payments) {
        this.payments = payments;
    }

    @Transactional
    public PaymentView create(UUID orgId, String reference, String provider, String status,
            BigDecimal amountUsd) {
        PaymentEntity p = new PaymentEntity();
        p.setOrgId(orgId);
        p.setReference(reference);
        p.setProvider(provider);
        p.setStatus(status);
        p.setAmountUsd(amountUsd);
        return toView(payments.save(p));
    }

    /** Advance a payment's status by reference. Empty if the reference is unknown. */
    @Transactional
    public Optional<PaymentView> updateStatus(String reference, String status) {
        return payments.findByReference(reference).map(p -> {
            p.setStatus(status);
            p.setUpdatedAt(OffsetDateTime.now());
            return toView(payments.save(p));
        });
    }

    @Transactional(readOnly = true)
    public List<PaymentView> listByOrg(UUID orgId) {
        return payments.findByOrgIdOrderByCreatedAtDesc(orgId).stream()
                .map(PaymentStore::toView).toList();
    }

    private static PaymentView toView(PaymentEntity p) {
        return new PaymentView(p.getReference(), p.getProvider(), p.getStatus(), p.getAmountUsd());
    }

    public record PaymentView(String reference, String provider, String status,
            BigDecimal amountUsd) {}
}

package com.codeshift.api;

import com.codeshift.bsg.PaymentStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Inbound provider webhooks. The Razorpay webhook advances a persisted payment to
 * PAID/FAILED — but only after verifying the {@code X-Razorpay-Signature} HMAC, so
 * a forged POST can't mark an invoice paid.
 *
 * <p>The signature secret comes from config ({@code codeshift.billing.razorpay.
 * webhook-secret}); with no secret set the endpoint refuses (503) rather than
 * trusting unauthenticated callers. No secret is committed — it's supplied per
 * environment.
 */
@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final ObjectProvider<PaymentStore> paymentStore;
    private final ObjectMapper json;
    private final String webhookSecret;

    public WebhookController(ObjectProvider<PaymentStore> paymentStore, ObjectMapper json,
            @Value("${codeshift.billing.razorpay.webhook-secret:}") String webhookSecret) {
        this.paymentStore = paymentStore;
        this.json = json;
        this.webhookSecret = webhookSecret;
    }

    public record WebhookAck(boolean handled, String reference, String status) {}

    @PostMapping("/billing/webhook/razorpay")
    public WebhookAck razorpay(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay webhook secret not configured.");
        }
        if (!RazorpaySignatures.verify(payload, signature, webhookSecret)) {
            // 400, not 401: a bad signature is a malformed/forged webhook, not auth.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature.");
        }

        PaymentStore payments = paymentStore.getIfAvailable();
        if (payments == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Persistence disabled (running with the nodb profile).");
        }

        JsonNode root;
        try {
            root = json.readTree(payload);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unparseable webhook body.");
        }

        String event = root.path("event").asText("");
        String status = STATUS_BY_EVENT.get(event);
        String reference = root.path("payload").path("payment").path("entity")
                .path("notes").path("reference").asText(null);

        if (status == null || reference == null || reference.isBlank()) {
            log.info("Razorpay webhook ignored (event={}, reference={})", event, reference);
            return new WebhookAck(false, reference, null);
        }

        return payments.updateStatus(reference, status)
                .map(p -> {
                    log.info("Razorpay webhook: payment {} -> {}", reference, status);
                    return new WebhookAck(true, reference, status);
                })
                .orElseGet(() -> new WebhookAck(false, reference, null));
    }

    /** Razorpay event -> internal payment status. */
    private static final Map<String, String> STATUS_BY_EVENT = Map.of(
            "payment.captured", "PAID",
            "order.paid", "PAID",
            "payment.failed", "FAILED");
}

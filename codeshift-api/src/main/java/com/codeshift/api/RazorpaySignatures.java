package com.codeshift.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Razorpay webhook signature verification — the real algorithm: the
 * {@code X-Razorpay-Signature} header is the hex-encoded HMAC-SHA256 of the raw
 * request body keyed with the webhook secret. Verifying it is what proves an
 * inbound webhook actually came from Razorpay and wasn't forged.
 *
 * <p>No Razorpay account or network is needed to exercise this: the same HMAC that
 * the gateway computes is computable in a test, so signature handling is fully
 * verifiable offline.
 */
public final class RazorpaySignatures {

    private RazorpaySignatures() {}

    /** Hex-encoded HMAC-SHA256 of {@code payload} under {@code secret}. */
    public static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    /** Constant-time verification of a webhook signature against the expected HMAC. */
    public static boolean verify(String payload, String signature, String secret) {
        if (signature == null || secret == null || secret.isBlank()) {
            return false;
        }
        String expected = sign(payload, secret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}

package com.codeshift.api;

import com.codeshift.bsg.SecretCipher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Local AES-256-GCM {@link SecretCipher} — the dev / self-host KMS. Encrypts tenant
 * secrets with authenticated encryption (a fresh random 96-bit IV per operation,
 * prepended to the ciphertext), so two encryptions of the same value differ and any
 * tampering is detected on decrypt.
 *
 * <p>The master key comes from {@code codeshift.security.master-key} (base64, 32
 * bytes). With none set it derives a process-stable dev key and warns loudly — real
 * deployments set the property or swap in a managed KMS adapter (AWS KMS per-tenant
 * CMK) behind the same {@link SecretCipher} port. No key material is committed.
 */
@Component
public class LocalAesGcmCipher implements SecretCipher {

    private static final Logger log = LoggerFactory.getLogger(LocalAesGcmCipher.class);
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public LocalAesGcmCipher(@Value("${codeshift.security.master-key:}") String masterKeyB64) {
        this.key = new SecretKeySpec(resolveKey(masterKeyB64), "AES");
    }

    private static byte[] resolveKey(String masterKeyB64) {
        if (masterKeyB64 != null && !masterKeyB64.isBlank()) {
            byte[] k = Base64.getDecoder().decode(masterKeyB64.trim());
            if (k.length != 32) {
                throw new IllegalArgumentException(
                        "codeshift.security.master-key must be 32 bytes (base64); got " + k.length);
            }
            return k;
        }
        log.warn("No codeshift.security.master-key set — deriving a DEV-ONLY key. "
                + "Set the property (or use a managed KMS) before production.");
        return sha256("codeshift-dev-master-key-do-not-use-in-prod");
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Override
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return "aesgcm:" + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String token) {
        try {
            String b64 = token.startsWith("aesgcm:") ? token.substring("aesgcm:".length()) : token;
            byte[] all = Base64.getDecoder().decode(b64);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(all, 0, iv, 0, IV_BYTES);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] pt = cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed (tampered or wrong key)", e);
        }
    }
}

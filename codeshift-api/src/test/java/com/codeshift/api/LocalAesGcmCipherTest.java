package com.codeshift.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** AES-256-GCM cipher: round-trips, is non-deterministic, and detects tampering. */
class LocalAesGcmCipherTest {

    private final LocalAesGcmCipher cipher = new LocalAesGcmCipher(""); // dev-derived key

    @Test
    void roundTripsPlaintext() {
        String secret = "sk-ant-api03-super-secret-byok-key";
        String token = cipher.encrypt(secret);

        assertThat(token).startsWith("aesgcm:").doesNotContain(secret);
        assertThat(cipher.decrypt(token)).isEqualTo(secret);
    }

    @Test
    void encryptionIsNonDeterministic() {
        // Fresh IV per call → two ciphertexts of the same plaintext differ.
        assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same"));
    }

    @Test
    void detectsTampering() {
        String token = cipher.encrypt("value");
        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("A") ? "B" : "A") + token.substring(token.length() - 1);

        assertThatThrownBy(() -> cipher.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWrongLengthMasterKey() {
        assertThatThrownBy(() -> new LocalAesGcmCipher("dG9vLXNob3J0")) // < 32 bytes
                .isInstanceOf(IllegalArgumentException.class);
    }
}

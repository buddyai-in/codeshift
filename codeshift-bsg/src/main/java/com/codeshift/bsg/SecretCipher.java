package com.codeshift.bsg;

/**
 * Envelope-encryption port for secrets at rest (per-tenant BYOK model keys, etc.).
 *
 * <p>Kept behind an interface so the platform encrypts tenant secrets without a hard
 * dependency on any one KMS: a local AES-GCM cipher backs dev/self-host, and a
 * managed KMS (AWS KMS, per-tenant CMK) is a drop-in production adapter. Callers
 * never see plaintext at rest — only ciphertext is persisted.
 */
public interface SecretCipher {

    /** Encrypt plaintext to an opaque, self-describing token (safe to store as text). */
    String encrypt(String plaintext);

    /** Decrypt a token produced by {@link #encrypt}. Throws if it was tampered with. */
    String decrypt(String ciphertext);
}

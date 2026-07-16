package com.codeshift.bsg;

import com.codeshift.bsg.entity.TenantSecretEntity;
import com.codeshift.bsg.repo.TenantSecretRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-tenant secret vault. Values are encrypted through a {@link SecretCipher}
 * before they hit the database and decrypted only on read, so a DB dump never
 * exposes a tenant's BYOK model keys. All access is org-scoped — one tenant can
 * never read another's secrets.
 */
public class TenantSecretStore {

    private final TenantSecretRepository secrets;
    private final SecretCipher cipher;

    public TenantSecretStore(TenantSecretRepository secrets, SecretCipher cipher) {
        this.secrets = secrets;
        this.cipher = cipher;
    }

    /** Store (or replace) a tenant secret, encrypting the plaintext at rest. */
    @Transactional
    public void put(UUID orgId, String name, String plaintext) {
        String ciphertext = cipher.encrypt(plaintext);
        TenantSecretEntity e = secrets.findByOrgIdAndName(orgId, name)
                .orElseGet(TenantSecretEntity::new);
        e.setOrgId(orgId);
        e.setName(name);
        e.setCiphertext(ciphertext);
        e.setUpdatedAt(OffsetDateTime.now());
        secrets.save(e);
    }

    /** Read and decrypt a tenant secret. Empty if the tenant has no such secret. */
    @Transactional(readOnly = true)
    public Optional<String> get(UUID orgId, String name) {
        return secrets.findByOrgIdAndName(orgId, name)
                .map(e -> cipher.decrypt(e.getCiphertext()));
    }

    /** The names of a tenant's secrets — never the values. */
    @Transactional(readOnly = true)
    public List<String> listNames(UUID orgId) {
        return secrets.findByOrgIdOrderByName(orgId).stream()
                .map(TenantSecretEntity::getName).toList();
    }

    @Transactional
    public void delete(UUID orgId, String name) {
        secrets.deleteByOrgIdAndName(orgId, name);
    }
}

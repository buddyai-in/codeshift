package com.codeshift.api;

import com.codeshift.bsg.TenantSecretStore;
import com.codeshift.bsg.TenantStore;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * BYOK (bring-your-own-key) model credentials per tenant. A tenant stores its own
 * provider API key; it's encrypted at rest by the {@link TenantSecretStore}'s cipher
 * and its plaintext is never returned by any endpoint — only the list of configured
 * providers. Tenant-scoped, persistence-gated (503 under {@code nodb}).
 */
@RestController
public class TenantKeyController {

    /** Secret-name prefix for BYOK model keys: {@code byok.model.<provider>}. */
    static final String BYOK_PREFIX = "byok.model.";

    private final ObjectProvider<TenantSecretStore> secretStore;
    private final ObjectProvider<TenantStore> tenantStore;

    public TenantKeyController(ObjectProvider<TenantSecretStore> secretStore,
            ObjectProvider<TenantStore> tenantStore) {
        this.secretStore = secretStore;
        this.tenantStore = tenantStore;
    }

    private TenantSecretStore secrets() {
        TenantSecretStore s = secretStore.getIfAvailable();
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Persistence disabled (running with the nodb profile). Start with a database.");
        }
        return s;
    }

    private UUID currentOrg() {
        return TenantContext.current().orElseGet(() -> {
            TenantStore ts = tenantStore.getIfAvailable();
            if (ts == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Persistence disabled (running with the nodb profile).");
            }
            return ts.defaultOrgId();
        });
    }

    public record PutKeyRequest(String apiKey) {}

    public record KeyStatus(String provider, boolean configured) {}

    /** Store (or replace) this tenant's BYOK API key for a model provider. */
    @PutMapping("/tenants/keys/{provider}")
    public KeyStatus put(@PathVariable String provider, @RequestBody PutKeyRequest req) {
        if (req.apiKey() == null || req.apiKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "apiKey is required.");
        }
        secrets().put(currentOrg(), BYOK_PREFIX + provider, req.apiKey());
        return new KeyStatus(provider, true);
    }

    /** List the providers this tenant has a BYOK key for — never the key values. */
    @GetMapping("/tenants/keys")
    public List<KeyStatus> list() {
        return secrets().listNames(currentOrg()).stream()
                .filter(n -> n.startsWith(BYOK_PREFIX))
                .map(n -> new KeyStatus(n.substring(BYOK_PREFIX.length()), true))
                .toList();
    }

    @DeleteMapping("/tenants/keys/{provider}")
    public void delete(@PathVariable String provider) {
        secrets().delete(currentOrg(), BYOK_PREFIX + provider);
    }
}

package com.codeshift.bsg;

import com.codeshift.bsg.entity.OrganizationEntity;
import com.codeshift.bsg.repo.OrganizationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists tenants (organizations) — the row-level owner of everything else.
 *
 * <p>Multi-tenancy is enforced by scoping every project read/write to an org id
 * (see {@link ProjectStore}); this store creates and resolves those orgs. A
 * singleton {@code default} org backs single-tenant / dev usage so requests with
 * no {@code X-Tenant-Id} header still work.
 */
public class TenantStore {

    /** Name of the implicit tenant used when no {@code X-Tenant-Id} header is present. */
    public static final String DEFAULT_ORG_NAME = "default";

    private final OrganizationRepository orgs;

    public TenantStore(OrganizationRepository orgs) {
        this.orgs = orgs;
    }

    @Transactional
    public UUID create(String name) {
        OrganizationEntity org = new OrganizationEntity();
        org.setName(name);
        return orgs.save(org).getId();
    }

    @Transactional(readOnly = true)
    public List<OrgSummary> list() {
        return orgs.findAll().stream()
                .map(o -> new OrgSummary(o.getId(), o.getName()))
                .toList();
    }

    /**
     * Resolve a tenant from an {@code X-Tenant-Id} header value: a valid org UUID is
     * used directly; a blank/absent/unknown value falls back to the singleton default
     * org (created on first use). Never throws — a bad header degrades to the default.
     */
    @Transactional
    public UUID resolveOrDefault(String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) {
            try {
                UUID id = UUID.fromString(headerValue.trim());
                if (orgs.existsById(id)) {
                    return id;
                }
            } catch (IllegalArgumentException ignored) {
                // Not a UUID — fall through to the default org.
            }
        }
        return defaultOrgId();
    }

    /** The id of the singleton default org, creating it if it doesn't exist yet. */
    @Transactional
    public UUID defaultOrgId() {
        return orgs.findByNameOrderByCreatedAt(DEFAULT_ORG_NAME).stream()
                .findFirst()
                .map(OrganizationEntity::getId)
                .orElseGet(() -> create(DEFAULT_ORG_NAME));
    }

    public record OrgSummary(UUID id, String name) {}
}

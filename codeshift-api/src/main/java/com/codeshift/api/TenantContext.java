package com.codeshift.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Holds the current request's tenant (org id) in a thread-local, set by
 * {@link TenantFilter} from the {@code X-Tenant-Id} header. Controllers and the
 * run runtime read it to scope every persistence call to the calling tenant.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID orgId) {
        CURRENT.set(orgId);
    }

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}

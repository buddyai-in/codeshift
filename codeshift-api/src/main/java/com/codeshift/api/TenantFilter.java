package com.codeshift.api;

import com.codeshift.bsg.TenantStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the tenant for every request from the {@code X-Tenant-Id} header and
 * publishes it on {@link TenantContext}. With no header (or the {@code nodb}
 * profile, where no {@link TenantStore} exists) the context is left empty and
 * callers fall back to the default org — single-tenant dev mode still works.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    /** Header carrying the caller's tenant (org UUID). */
    public static final String TENANT_HEADER = "X-Tenant-Id";

    private final ObjectProvider<TenantStore> tenantStore;

    public TenantFilter(ObjectProvider<TenantStore> tenantStore) {
        this.tenantStore = tenantStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        TenantStore store = tenantStore.getIfAvailable();
        try {
            if (store != null) {
                TenantContext.set(store.resolveOrDefault(request.getHeader(TENANT_HEADER)));
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

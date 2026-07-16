package com.codeshift.api;

import com.codeshift.bsg.ModelDeploymentStore;
import com.codeshift.bsg.TenantStore;
import com.codeshift.common.ModelDeploymentType;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Per-tenant model deployment: choose cloud (managed), on-prem, or in-VPC and point
 * at your own OpenAI-compatible endpoint + model. Combined with a BYOK key, this
 * lets a regulated tenant keep every model call inside their own network.
 * Tenant-scoped, persistence-gated (503 under {@code nodb}).
 */
@RestController
public class ModelDeploymentController {

    private final ObjectProvider<ModelDeploymentStore> deploymentStore;
    private final ObjectProvider<TenantStore> tenantStore;
    private final TenantModelResolver resolver;

    public ModelDeploymentController(ObjectProvider<ModelDeploymentStore> deploymentStore,
            ObjectProvider<TenantStore> tenantStore, TenantModelResolver resolver) {
        this.deploymentStore = deploymentStore;
        this.tenantStore = tenantStore;
        this.resolver = resolver;
    }

    private ModelDeploymentStore deployments() {
        ModelDeploymentStore s = deploymentStore.getIfAvailable();
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

    public record DeploymentRequest(ModelDeploymentType deploymentType, String provider,
            String baseUrl, String model) {}

    /** Set this tenant's model deployment (cloud / on-prem / in-VPC + endpoint + model). */
    @PutMapping("/tenants/model-deployment")
    public TenantModelResolver.ResolvedDeployment put(@RequestBody DeploymentRequest req) {
        if (req.deploymentType() == null || req.provider() == null || req.provider().isBlank()
                || req.model() == null || req.model().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "deploymentType, provider and model are required.");
        }
        if (req.deploymentType() != ModelDeploymentType.CLOUD
                && (req.baseUrl() == null || req.baseUrl().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "baseUrl is required for on-prem / in-VPC deployments.");
        }
        UUID org = currentOrg();
        deployments().put(org, req.deploymentType(), req.provider(), req.baseUrl(), req.model());
        return resolver.resolve(org);
    }

    /** The effective deployment this tenant's model calls route to (no key value). */
    @GetMapping("/tenants/model-deployment")
    public TenantModelResolver.ResolvedDeployment get() {
        return resolver.resolve(currentOrg());
    }
}

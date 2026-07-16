package com.codeshift.api;

import com.codeshift.bsg.ModelDeploymentStore;
import com.codeshift.bsg.TenantSecretStore;
import com.codeshift.common.ModelDeploymentType;
import com.codeshift.common.ModelProfile;
import com.codeshift.gateway.ModelGateway;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Resolves the effective model deployment for a tenant — the routing layer that
 * makes on-prem / in-VPC / BYOK real. A tenant with a configured deployment routes
 * to its own endpoint + model (and, if present, its BYOK key); everyone else falls
 * back to the platform's managed cloud default.
 *
 * <p>The gateway consults this to decide where a tenant's calls go. The resolved
 * view never carries the API key; {@link #apiKey} exposes the decrypted key only to
 * internal callers that actually place the request.
 */
@Component
public class TenantModelResolver {

    private final ObjectProvider<ModelDeploymentStore> deploymentStore;
    private final ObjectProvider<TenantSecretStore> secretStore;
    private final ModelGateway gateway;

    public TenantModelResolver(ObjectProvider<ModelDeploymentStore> deploymentStore,
            ObjectProvider<TenantSecretStore> secretStore, ModelGateway gateway) {
        this.deploymentStore = deploymentStore;
        this.secretStore = secretStore;
        this.gateway = gateway;
    }

    /** The effective deployment for a tenant (never includes the key value). */
    public ResolvedDeployment resolve(UUID orgId) {
        ModelDeploymentStore store = deploymentStore.getIfAvailable();
        Optional<ModelDeploymentStore.Deployment> configured =
                store == null ? Optional.empty() : store.get(orgId);

        if (configured.isPresent()) {
            ModelDeploymentStore.Deployment d = configured.get();
            boolean byok = hasByok(orgId, d.provider());
            return new ResolvedDeployment(d.type(), d.provider(), d.baseUrl(), d.model(), byok);
        }
        // Platform managed cloud default.
        return new ResolvedDeployment(ModelDeploymentType.CLOUD, "platform", null,
                gateway.resolvedModel(ModelProfile.REASONING), false);
    }

    /** The decrypted BYOK key for a tenant + provider, for the caller that places the request. */
    public Optional<String> apiKey(UUID orgId, String provider) {
        TenantSecretStore store = secretStore.getIfAvailable();
        return store == null ? Optional.empty()
                : store.get(orgId, TenantKeyController.BYOK_PREFIX + provider);
    }

    private boolean hasByok(UUID orgId, String provider) {
        return apiKey(orgId, provider).isPresent();
    }

    /** The effective deployment a tenant's calls route to. No secret material. */
    public record ResolvedDeployment(ModelDeploymentType type, String provider, String baseUrl,
            String model, boolean byokConfigured) {}
}

package com.codeshift.bsg;

import com.codeshift.bsg.entity.TenantModelDeploymentEntity;
import com.codeshift.bsg.repo.TenantModelDeploymentRepository;
import com.codeshift.common.ModelDeploymentType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Persists a tenant's model deployment (cloud / on-prem / in-VPC endpoint + model). */
public class ModelDeploymentStore {

    private final TenantModelDeploymentRepository deployments;

    public ModelDeploymentStore(TenantModelDeploymentRepository deployments) {
        this.deployments = deployments;
    }

    @Transactional
    public Deployment put(UUID orgId, ModelDeploymentType type, String provider,
            String baseUrl, String model) {
        TenantModelDeploymentEntity e = deployments.findByOrgId(orgId)
                .orElseGet(TenantModelDeploymentEntity::new);
        e.setOrgId(orgId);
        e.setDeploymentType(type);
        e.setProvider(provider);
        e.setBaseUrl(baseUrl);
        e.setModel(model);
        e.setUpdatedAt(OffsetDateTime.now());
        return toView(deployments.save(e));
    }

    @Transactional(readOnly = true)
    public Optional<Deployment> get(UUID orgId) {
        return deployments.findByOrgId(orgId).map(ModelDeploymentStore::toView);
    }

    private static Deployment toView(TenantModelDeploymentEntity e) {
        return new Deployment(e.getDeploymentType(), e.getProvider(), e.getBaseUrl(), e.getModel());
    }

    public record Deployment(ModelDeploymentType type, String provider, String baseUrl,
            String model) {}
}

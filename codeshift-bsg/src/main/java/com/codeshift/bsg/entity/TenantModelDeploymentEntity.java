package com.codeshift.bsg.entity;

import com.codeshift.common.ModelDeploymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * A tenant's model deployment: where its models run (cloud / on-prem / in-VPC) and
 * which provider + endpoint + model to route to. The BYOK API key lives separately
 * in the encrypted secret vault.
 */
@Entity
@Table(name = "tenant_model_deployments")
public class TenantModelDeploymentEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /** One deployment per tenant (unique). */
    @Column(name = "org_id", nullable = false, unique = true)
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "deployment_type", nullable = false)
    private ModelDeploymentType deploymentType = ModelDeploymentType.CLOUD;

    @Column(nullable = false)
    private String provider;

    /** Endpoint base URL for on-prem/in-VPC (null for CLOUD). */
    @Column(name = "base_url")
    private String baseUrl;

    @Column(nullable = false)
    private String model;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public ModelDeploymentType getDeploymentType() {
        return deploymentType;
    }

    public void setDeploymentType(ModelDeploymentType deploymentType) {
        this.deploymentType = deploymentType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

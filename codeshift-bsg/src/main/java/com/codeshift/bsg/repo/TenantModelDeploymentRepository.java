package com.codeshift.bsg.repo;

import com.codeshift.bsg.entity.TenantModelDeploymentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantModelDeploymentRepository
        extends JpaRepository<TenantModelDeploymentEntity, UUID> {

    Optional<TenantModelDeploymentEntity> findByOrgId(UUID orgId);
}

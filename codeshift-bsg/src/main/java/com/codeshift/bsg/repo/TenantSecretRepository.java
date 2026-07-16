package com.codeshift.bsg.repo;

import com.codeshift.bsg.entity.TenantSecretEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantSecretRepository extends JpaRepository<TenantSecretEntity, UUID> {

    Optional<TenantSecretEntity> findByOrgIdAndName(UUID orgId, String name);

    List<TenantSecretEntity> findByOrgIdOrderByName(UUID orgId);

    void deleteByOrgIdAndName(UUID orgId, String name);
}

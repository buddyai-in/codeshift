package com.codeshift.bsg.repo;

import com.codeshift.bsg.entity.OrganizationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

    List<OrganizationEntity> findByNameOrderByCreatedAt(String name);
}

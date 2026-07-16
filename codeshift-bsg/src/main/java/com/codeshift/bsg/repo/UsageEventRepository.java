package com.codeshift.bsg.repo;

import com.codeshift.bsg.entity.UsageEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageEventRepository extends JpaRepository<UsageEventEntity, UUID> {

    List<UsageEventEntity> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    List<UsageEventEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}

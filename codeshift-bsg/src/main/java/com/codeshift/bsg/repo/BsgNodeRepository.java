package com.codeshift.bsg.repo;

import com.codeshift.bsg.entity.BsgNodeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BsgNodeRepository extends JpaRepository<BsgNodeEntity, UUID> {
    List<BsgNodeEntity> findByVersionIdOrderByNodeRef(UUID versionId);
}

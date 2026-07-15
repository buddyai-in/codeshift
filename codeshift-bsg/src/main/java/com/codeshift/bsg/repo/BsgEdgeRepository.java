package com.codeshift.bsg.repo;

import com.codeshift.bsg.entity.BsgEdgeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BsgEdgeRepository extends JpaRepository<BsgEdgeEntity, UUID> {
    List<BsgEdgeEntity> findByVersionId(UUID versionId);
}

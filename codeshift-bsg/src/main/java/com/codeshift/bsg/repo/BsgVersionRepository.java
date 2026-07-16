package com.codeshift.bsg.repo;

import com.codeshift.bsg.entity.BsgVersionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BsgVersionRepository extends JpaRepository<BsgVersionEntity, UUID> {

    List<BsgVersionEntity> findByProjectIdOrderByVersionNumberDesc(UUID projectId);
}

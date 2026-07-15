package com.codeshift.bsg.repo;

import com.codeshift.bsg.entity.BsgEdgeEntity;
import com.codeshift.bsg.entity.BsgNodeEntity;
import com.codeshift.bsg.entity.BsgVersionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repositories for the BSG store. */
public final class BsgRepositories {

    private BsgRepositories() {}

    public interface BsgVersionRepository extends JpaRepository<BsgVersionEntity, UUID> {}

    public interface BsgNodeRepository extends JpaRepository<BsgNodeEntity, UUID> {
        List<BsgNodeEntity> findByVersionIdOrderByNodeRef(UUID versionId);
    }

    public interface BsgEdgeRepository extends JpaRepository<BsgEdgeEntity, UUID> {
        List<BsgEdgeEntity> findByVersionId(UUID versionId);
    }
}

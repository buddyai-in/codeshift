package com.codeshift.bsg.repo;

import com.codeshift.bsg.entity.BsgVersionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BsgVersionRepository extends JpaRepository<BsgVersionEntity, UUID> {}

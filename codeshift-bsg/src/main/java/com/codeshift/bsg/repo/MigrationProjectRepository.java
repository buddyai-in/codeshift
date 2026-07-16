package com.codeshift.bsg.repo;

import com.codeshift.bsg.entity.MigrationProjectEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationProjectRepository extends JpaRepository<MigrationProjectEntity, UUID> {}

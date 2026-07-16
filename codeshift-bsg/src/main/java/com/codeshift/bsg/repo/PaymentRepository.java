package com.codeshift.bsg.repo;

import com.codeshift.bsg.entity.PaymentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByReference(String reference);

    List<PaymentEntity> findByOrgIdOrderByCreatedAtDesc(UUID orgId);
}

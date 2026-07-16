package com.codeshift.bsg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/** A migration project — the owner of a chain of versioned BSGs, scoped to a tenant. */
@Entity
@Table(name = "migration_projects")
public class MigrationProjectEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /** Owning tenant (organization). The row-level scope for every read/write. */
    @Column(name = "org_id")
    private UUID orgId;

    @Column(nullable = false)
    private String name;

    @Column(name = "source_language")
    private String sourceLanguage;

    @Column(name = "target_stack")
    private String targetStack;

    @Column(nullable = false)
    private String status = "CREATED";

    /** Per-project USD token budget (the metering guardrail). */
    @Column(name = "budget_usd", nullable = false)
    private BigDecimal budgetUsd = new BigDecimal("25");

    /** USD spent so far against the budget (accumulated from usage events). */
    @Column(name = "spent_usd", nullable = false)
    private BigDecimal spentUsd = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public BigDecimal getBudgetUsd() {
        return budgetUsd;
    }

    public void setBudgetUsd(BigDecimal budgetUsd) {
        this.budgetUsd = budgetUsd;
    }

    public BigDecimal getSpentUsd() {
        return spentUsd;
    }

    public void setSpentUsd(BigDecimal spentUsd) {
        this.spentUsd = spentUsd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getTargetStack() {
        return targetStack;
    }

    public void setTargetStack(String targetStack) {
        this.targetStack = targetStack;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

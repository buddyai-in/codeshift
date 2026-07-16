package com.codeshift.bsg;

import com.codeshift.bsg.entity.MigrationProjectEntity;
import com.codeshift.bsg.repo.MigrationProjectRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists migration projects — the owner of a chain of versioned BSGs.
 *
 * <p>Every project belongs to a tenant (org). Reads are scoped to an org id so one
 * tenant never sees another's projects — application-level row-level security that
 * mirrors the DB's {@code org_id} column (Postgres RLS can enforce the same rule).
 */
public class ProjectStore {

    private final MigrationProjectRepository projects;

    public ProjectStore(MigrationProjectRepository projects) {
        this.projects = projects;
    }

    @Transactional
    public UUID create(UUID orgId, String name, String sourceLanguage, String targetStack) {
        MigrationProjectEntity p = new MigrationProjectEntity();
        p.setOrgId(orgId);
        p.setName(name);
        p.setSourceLanguage(sourceLanguage);
        p.setTargetStack(targetStack);
        return projects.save(p).getId();
    }

    /** List a tenant's projects only. */
    @Transactional(readOnly = true)
    public List<ProjectSummary> list(UUID orgId) {
        return projects.findByOrgIdOrderByCreatedAt(orgId).stream()
                .map(ProjectStore::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectSummary get(UUID id) {
        return toSummary(load(id));
    }

    /** Read a project's budget + spend for the metering guardrail. */
    @Transactional(readOnly = true)
    public Budget budget(UUID id) {
        MigrationProjectEntity p = load(id);
        return new Budget(p.getId(), p.getBudgetUsd(), p.getSpentUsd());
    }

    /** Add USD spend to a project's running total (called by the metering store). */
    @Transactional
    public void addSpend(UUID id, BigDecimal deltaUsd) {
        MigrationProjectEntity p = load(id);
        p.setSpentUsd(p.getSpentUsd().add(deltaUsd));
        projects.save(p);
    }

    /** True when the project still has budget headroom for another charge. */
    @Transactional(readOnly = true)
    public boolean withinBudget(UUID id, BigDecimal prospectiveUsd) {
        MigrationProjectEntity p = load(id);
        return p.getSpentUsd().add(prospectiveUsd).compareTo(p.getBudgetUsd()) <= 0;
    }

    private MigrationProjectEntity load(UUID id) {
        return projects.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown project " + id));
    }

    private static ProjectSummary toSummary(MigrationProjectEntity p) {
        return new ProjectSummary(p.getId(), p.getOrgId(), p.getName(), p.getSourceLanguage(),
                p.getTargetStack(), p.getStatus());
    }

    public record ProjectSummary(UUID id, UUID orgId, String name, String sourceLanguage,
            String targetStack, String status) {}

    public record Budget(UUID projectId, BigDecimal budgetUsd, BigDecimal spentUsd) {}
}

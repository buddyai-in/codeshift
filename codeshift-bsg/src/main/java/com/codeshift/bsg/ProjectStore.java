package com.codeshift.bsg;

import com.codeshift.bsg.entity.MigrationProjectEntity;
import com.codeshift.bsg.repo.MigrationProjectRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Persists migration projects — the owner of a chain of versioned BSGs. */
public class ProjectStore {

    private final MigrationProjectRepository projects;

    public ProjectStore(MigrationProjectRepository projects) {
        this.projects = projects;
    }

    @Transactional
    public UUID create(String name, String sourceLanguage, String targetStack) {
        MigrationProjectEntity p = new MigrationProjectEntity();
        p.setName(name);
        p.setSourceLanguage(sourceLanguage);
        p.setTargetStack(targetStack);
        return projects.save(p).getId();
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> list() {
        return projects.findAll().stream()
                .map(p -> new ProjectSummary(p.getId(), p.getName(), p.getSourceLanguage(),
                        p.getTargetStack(), p.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectSummary get(UUID id) {
        MigrationProjectEntity p = projects.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown project " + id));
        return new ProjectSummary(p.getId(), p.getName(), p.getSourceLanguage(),
                p.getTargetStack(), p.getStatus());
    }

    public record ProjectSummary(UUID id, String name, String sourceLanguage,
            String targetStack, String status) {}
}

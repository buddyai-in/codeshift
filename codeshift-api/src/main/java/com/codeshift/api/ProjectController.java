package com.codeshift.api;

import com.codeshift.bsg.BsgStore;
import com.codeshift.bsg.ProjectStore;
import com.codeshift.bsg.RequirementsProducer;
import com.codeshift.bsg.model.BsgGraph;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Projects + the versioned BSG audit trail + new-code addition (product doc §7).
 *
 * <p>These endpoints require persistence, so they return 503 under the {@code nodb}
 * profile. The Requirements Agent turns a plain-English feature request into new
 * {@code NEW_FEATURE} BSG nodes, persisted as a new version — the whole application
 * history is a traceable chain of human-approved requests.
 */
@RestController
@RequestMapping
public class ProjectController {

    private final ObjectProvider<ProjectStore> projectStore;
    private final ObjectProvider<BsgStore> bsgStore;
    private final RequirementsProducer requirements;

    public ProjectController(ObjectProvider<ProjectStore> projectStore,
            ObjectProvider<BsgStore> bsgStore, RequirementsProducer requirements) {
        this.projectStore = projectStore;
        this.bsgStore = bsgStore;
        this.requirements = requirements;
    }

    private ProjectStore projects() {
        ProjectStore s = projectStore.getIfAvailable();
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Persistence disabled (running with the nodb profile). Start with a database.");
        }
        return s;
    }

    private BsgStore bsg() {
        BsgStore s = bsgStore.getIfAvailable();
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Persistence disabled (running with the nodb profile). Start with a database.");
        }
        return s;
    }

    public record CreateProjectRequest(String name, String sourceLanguage, String targetStack) {}

    public record CreatedProject(String projectId) {}

    public record SavedVersion(String versionId, int versionNumber) {}

    public record FeatureRequestBody(String request) {}

    public record FeatureResponse(String versionId, int versionNumber, BsgGraph bsg) {}

    @PostMapping("/projects")
    public CreatedProject create(@RequestBody CreateProjectRequest req) {
        UUID id = projects().create(req.name(), req.sourceLanguage(), req.targetStack());
        return new CreatedProject(id.toString());
    }

    @GetMapping("/projects")
    public List<ProjectStore.ProjectSummary> list() {
        return projects().list();
    }

    @GetMapping("/projects/{projectId}/bsg/versions")
    public List<BsgStore.VersionSummary> versions(@PathVariable String projectId) {
        return bsg().listVersions(UUID.fromString(projectId));
    }

    @GetMapping("/bsg/versions/{versionId}")
    public BsgGraph version(@PathVariable String versionId) {
        return bsg().getVersion(UUID.fromString(versionId));
    }

    /** Persist a BSG version for a project (seed, or capture an approved run's BSG). */
    @PostMapping("/projects/{projectId}/bsg")
    public SavedVersion saveBsg(@PathVariable String projectId, @RequestBody BsgGraph body) {
        UUID pid = UUID.fromString(projectId);
        int next = bsg().nextVersionNumber(pid);
        UUID versionId = bsg().saveGraph(new BsgGraph(projectId, next, body.nodes(), body.edges()));
        return new SavedVersion(versionId.toString(), next);
    }

    /** New-code addition: a feature request → new NEW_FEATURE nodes → new version. */
    @PostMapping("/projects/{projectId}/feature-requests")
    public FeatureResponse addFeature(@PathVariable String projectId,
            @RequestBody FeatureRequestBody body) {
        UUID pid = UUID.fromString(projectId);
        UUID latest = bsg().latestVersionId(pid).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "No BSG yet for this project — persist an initial version first."));
        BsgGraph current = bsg().getVersion(latest);
        int next = bsg().nextVersionNumber(pid);
        BsgGraph updated = requirements.addFeature(current, body.request(), next);
        UUID versionId = bsg().saveGraph(new BsgGraph(projectId, next, updated.nodes(), updated.edges()));
        return new FeatureResponse(versionId.toString(), next, bsg().getVersion(versionId));
    }
}

package com.codeshift.api;

import com.codeshift.bsg.BsgStore;
import com.codeshift.bsg.DebtProducer;
import com.codeshift.bsg.PerformanceProducer;
import com.codeshift.bsg.ProjectStore;
import com.codeshift.bsg.RequirementsProducer;
import com.codeshift.bsg.TenantStore;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.DebtReport;
import com.codeshift.bsg.model.PerformanceReport;
import com.codeshift.bsg.model.PortfolioReport;
import com.codeshift.bsg.model.PortfolioReport.ProjectHealth;
import com.codeshift.common.NewCodeMode;
import java.util.ArrayList;
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
    private final ObjectProvider<TenantStore> tenantStore;
    private final RequirementsProducer requirements;
    private final DebtProducer debt;
    private final PerformanceProducer performance;

    public ProjectController(ObjectProvider<ProjectStore> projectStore,
            ObjectProvider<BsgStore> bsgStore, ObjectProvider<TenantStore> tenantStore,
            RequirementsProducer requirements, DebtProducer debt, PerformanceProducer performance) {
        this.projectStore = projectStore;
        this.bsgStore = bsgStore;
        this.tenantStore = tenantStore;
        this.requirements = requirements;
        this.debt = debt;
        this.performance = performance;
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

    private TenantStore tenants() {
        TenantStore s = tenantStore.getIfAvailable();
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Persistence disabled (running with the nodb profile). Start with a database.");
        }
        return s;
    }

    /** The calling tenant (from X-Tenant-Id), or the default org for header-less requests. */
    private UUID currentOrg() {
        return TenantContext.current().orElseGet(() -> tenants().defaultOrgId());
    }

    public record CreateProjectRequest(String name, String sourceLanguage, String targetStack) {}

    public record CreatedProject(String projectId) {}

    public record SavedVersion(String versionId, int versionNumber) {}

    public record FeatureRequestBody(String request, NewCodeMode mode) {}

    public record FeatureResponse(String versionId, int versionNumber, BsgGraph bsg) {}

    public record CreateOrgRequest(String name) {}

    public record CreatedOrg(String orgId) {}

    /** Create a tenant (organization) — step one of the self-serve onboarding wizard. */
    @PostMapping("/orgs")
    public CreatedOrg createOrg(@RequestBody CreateOrgRequest req) {
        return new CreatedOrg(tenants().create(req.name()).toString());
    }

    @GetMapping("/orgs")
    public List<TenantStore.OrgSummary> listOrgs() {
        return tenants().list();
    }

    @PostMapping("/projects")
    public CreatedProject create(@RequestBody CreateProjectRequest req) {
        UUID id = projects().create(currentOrg(), req.name(), req.sourceLanguage(), req.targetStack());
        return new CreatedProject(id.toString());
    }

    @GetMapping("/projects")
    public List<ProjectStore.ProjectSummary> list() {
        return projects().list(currentOrg());
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

    /** Technical Debt Intelligence: score the latest BSG + delta from the prior version. */
    @GetMapping("/projects/{projectId}/debt")
    public DebtReport debt(@PathVariable String projectId) {
        UUID pid = UUID.fromString(projectId);
        List<BsgStore.VersionSummary> vs = bsg().listVersions(pid);
        if (vs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No BSG yet for this project.");
        }
        BsgGraph current = bsg().getVersion(vs.get(0).versionId());
        BsgGraph previous = vs.size() > 1 ? bsg().getVersion(vs.get(1).versionId()) : null;
        return debt.analyze(current, previous);
    }

    /** Performance Agent: optimisation recommendations from the latest BSG. */
    @GetMapping("/projects/{projectId}/performance")
    public PerformanceReport performance(@PathVariable String projectId) {
        return performance.analyze(latestBsg(UUID.fromString(projectId)));
    }

    /** Portfolio Intelligence: CIO-level health across every project. */
    @GetMapping("/portfolio")
    public PortfolioReport portfolio() {
        List<ProjectHealth> health = new ArrayList<>();
        int scoreSum = 0;
        for (ProjectStore.ProjectSummary p : projects().list(currentOrg())) {
            List<BsgStore.VersionSummary> vs = bsg().listVersions(p.id());
            int score = 0;
            String grade = "A";
            int nodeCount = vs.isEmpty() ? 0 : vs.get(0).nodeCount();
            if (!vs.isEmpty()) {
                BsgGraph current = bsg().getVersion(vs.get(0).versionId());
                BsgGraph previous = vs.size() > 1 ? bsg().getVersion(vs.get(1).versionId()) : null;
                DebtReport d = debt.analyze(current, previous);
                score = d.debtScore();
                grade = d.grade();
            }
            scoreSum += score;
            health.add(new ProjectHealth(p.id().toString(), p.name(), vs.size(), nodeCount, score, grade));
        }
        int avg = health.isEmpty() ? 0 : scoreSum / health.size();
        return new PortfolioReport(health.size(), avg, health);
    }

    private BsgGraph latestBsg(UUID projectId) {
        List<BsgStore.VersionSummary> vs = bsg().listVersions(projectId);
        if (vs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No BSG yet for this project.");
        }
        return bsg().getVersion(vs.get(0).versionId());
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
        NewCodeMode mode = body.mode() != null ? body.mode() : NewCodeMode.FEATURE;
        BsgGraph updated = requirements.addFeature(current, body.request(), mode, next);
        UUID versionId = bsg().saveGraph(new BsgGraph(projectId, next, updated.nodes(), updated.edges()));
        return new FeatureResponse(versionId.toString(), next, bsg().getVersion(versionId));
    }
}

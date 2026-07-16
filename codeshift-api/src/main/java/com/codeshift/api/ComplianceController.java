package com.codeshift.api;

import com.codeshift.bsg.BsgStore;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.compliance.ComplianceReport;
import com.codeshift.compliance.ComplianceReporter;
import com.codeshift.compliance.ComplianceStandard;
import com.codeshift.compliance.ComplianceTemplates;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Vertical compliance packs (product doc Phase 6): PCI-DSS + HIPAA control
 * templates and BSG-coverage report packs.
 *
 * <p>The inline check ({@code POST /compliance/check}) needs no database, so it
 * works in every profile; the project check reads the latest persisted BSG.
 */
@RestController
public class ComplianceController {

    private final ObjectProvider<BsgStore> bsgStore;

    public ComplianceController(ObjectProvider<BsgStore> bsgStore) {
        this.bsgStore = bsgStore;
    }

    public record StandardSummary(String standard, String reference, String description,
            int controlCount) {}

    public record CheckRequest(ComplianceStandard standard, BsgGraph bsg) {}

    /** List the available compliance packs and their control counts. */
    @GetMapping("/compliance/standards")
    public List<StandardSummary> standards() {
        return Arrays.stream(ComplianceStandard.values())
                .map(s -> new StandardSummary(s.name(), s.reference(), s.description(),
                        ComplianceTemplates.controlsFor(s).size()))
                .toList();
    }

    /** The control pack for a standard as ready-to-seed BSG nodes. */
    @GetMapping("/compliance/standards/{standard}/template")
    public BsgGraph template(@PathVariable ComplianceStandard standard) {
        return new BsgGraph("template", 1, ComplianceTemplates.templateNodes(standard), List.of());
    }

    /** Assess an inline BSG against a standard — no database required. */
    @PostMapping("/compliance/check")
    public ComplianceReport check(@RequestBody CheckRequest req) {
        if (req.standard() == null || req.bsg() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Both 'standard' and 'bsg' are required.");
        }
        return ComplianceReporter.assess(req.bsg(), req.standard());
    }

    /** Assess a project's latest persisted BSG against a standard. */
    @GetMapping("/projects/{projectId}/compliance")
    public ComplianceReport projectCompliance(@PathVariable String projectId,
            @RequestParam ComplianceStandard standard) {
        BsgStore store = bsgStore.getIfAvailable();
        if (store == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Persistence disabled (running with the nodb profile). Use POST /compliance/check.");
        }
        UUID pid = UUID.fromString(projectId);
        UUID versionId = store.latestVersionId(pid).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "No BSG yet for this project."));
        return ComplianceReporter.assess(store.getVersion(versionId), standard);
    }
}

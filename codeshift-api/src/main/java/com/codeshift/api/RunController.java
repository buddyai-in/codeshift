package com.codeshift.api;

import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.HardeningResult;
import com.codeshift.bsg.model.TransformationResult;
import com.codeshift.bsg.model.ValidationReport;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Migration run lifecycle + live agent-log streaming.
 *
 * <ul>
 *   <li>{@code POST /runs} — start a run; returns the human-gate payload.</li>
 *   <li>{@code POST /runs/{threadId}/resume} — resume at the gate with a decision.</li>
 *   <li>{@code GET  /runs/stream?projectId=...} — SSE stream of per-node progress.</li>
 * </ul>
 */
@RestController
@RequestMapping("/runs")
public class RunController {

    private final GraphRuntime runtime;

    public RunController(GraphRuntime runtime) {
        this.runtime = runtime;
    }

    public record StartRequest(String projectId, List<String> moduleInventory, String projectPath) {}

    public record ResumeRequest(String decision) {}

    @PostMapping
    public GraphRuntime.StartResult start(@RequestBody StartRequest req) {
        return runtime.start(req.projectId(), req.moduleInventory(), req.projectPath());
    }

    /** Start a run from an uploaded source zip — the browser entry to the pipeline. */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GraphRuntime.StartResult startUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "projectName", required = false) String projectName) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty upload");
        }
        String name = projectName != null ? projectName : "uploaded-project";
        try {
            return runtime.startFromZip(name, file.getInputStream());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad zip: " + e.getMessage());
        }
    }

    /** Record a review decision + optional edits on one BSG node. */
    @PutMapping("/{threadId}/bsg/nodes/{nodeRef}")
    public BsgGraph reviewNode(
            @PathVariable String threadId,
            @PathVariable String nodeRef,
            @RequestBody NodeReview review) {
        return runtime.updateBsgNode(threadId, nodeRef, review.status(),
                review.title(), review.description());
    }

    public record NodeReview(String status, String title, String description) {}

    @PostMapping("/{threadId}/resume")
    public GraphRuntime.ResumeResult resume(
            @PathVariable String threadId, @RequestBody ResumeRequest req) {
        return runtime.resume(threadId, req.decision());
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String projectId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        runtime.stream(projectId, emitter);
        return emitter;
    }

    /** The BSG the Analysis Agent produced for a run, for the review gate. */
    @GetMapping("/{threadId}/bsg")
    public BsgGraph bsg(@PathVariable String threadId) {
        return runtime.bsgOf(threadId);
    }

    /** The architecture plan the Architecture Agent produced, for gate #2. */
    @GetMapping("/{threadId}/architecture")
    public ArchitecturePlan architecture(@PathVariable String threadId) {
        return runtime.architectureOf(threadId);
    }

    /** The generated code + tests (after both gates approved). */
    @GetMapping("/{threadId}/transformation")
    public TransformationResult transformation(@PathVariable String threadId) {
        return runtime.transformationOf(threadId);
    }

    /** The Validation Agent's report (compile + BSG coverage). */
    @GetMapping("/{threadId}/validation")
    public ValidationReport validation(@PathVariable String threadId) {
        return runtime.validationOf(threadId);
    }

    /** Hardening: security findings + DevOps bundle + Kafka plan. */
    @GetMapping("/{threadId}/hardening")
    public HardeningResult hardening(@PathVariable String threadId) {
        return runtime.hardeningOf(threadId);
    }
}

package com.codeshift.api;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    public record StartRequest(String projectId, List<String> moduleInventory) {}

    public record ResumeRequest(String decision) {}

    @PostMapping
    public GraphRuntime.StartResult start(@RequestBody StartRequest req) {
        return runtime.start(req.projectId(), req.moduleInventory());
    }

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
}

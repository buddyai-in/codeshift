package com.codeshift.api;

import com.codeshift.bsg.ArtifactStore;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Per-tenant artifact storage (migration outputs, reports, bundles). Backed by the
 * {@link ArtifactStore} port — a local filesystem in dev, S3 in production — with
 * every object namespaced by tenant, so one tenant can never read another's files.
 *
 * <p>Object storage needs no database, so these endpoints work in every profile
 * (including {@code nodb}); requests with no {@code X-Tenant-Id} use a default tenant.
 */
@RestController
public class ArtifactController {

    /** Tenant used for header-less requests (object storage doesn't require the DB). */
    private static final UUID DEFAULT_TENANT =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ArtifactStore artifacts;

    public ArtifactController(ArtifactStore artifacts) {
        this.artifacts = artifacts;
    }

    private UUID currentOrg() {
        return TenantContext.current().orElse(DEFAULT_TENANT);
    }

    public record ArtifactInfo(String key, int size, String contentType) {}

    @PutMapping("/artifacts/{key}")
    public ArtifactInfo put(@PathVariable String key, @RequestBody byte[] content,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {
        try {
            artifacts.put(currentOrg(), key, content, contentType);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return new ArtifactInfo(key, content.length,
                contentType == null ? "application/octet-stream" : contentType);
    }

    @GetMapping("/artifacts/{key}")
    public ResponseEntity<byte[]> get(@PathVariable String key) {
        ArtifactStore.Artifact a;
        try {
            a = artifacts.get(currentOrg(), key)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such artifact"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        MediaType type;
        try {
            type = MediaType.parseMediaType(a.contentType());
        } catch (RuntimeException e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok().contentType(type).body(a.content());
    }

    @GetMapping("/artifacts")
    public List<String> list() {
        return artifacts.list(currentOrg());
    }

    @DeleteMapping("/artifacts/{key}")
    public void delete(@PathVariable String key) {
        try {
            artifacts.delete(currentOrg(), key);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}

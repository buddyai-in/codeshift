package com.codeshift.bsg;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-tenant object storage port (S3 in production). Every object is namespaced by
 * org id, so one tenant can never read or overwrite another's artifacts — the same
 * isolation a per-tenant S3 prefix/bucket gives. A local filesystem adapter backs
 * dev/self-host; an S3 adapter is a drop-in behind this interface.
 */
public interface ArtifactStore {

    /** Store (or replace) an object for a tenant. */
    void put(UUID orgId, String key, byte[] content, String contentType);

    /** Read an object back for the owning tenant only. */
    Optional<Artifact> get(UUID orgId, String key);

    /** List a tenant's object keys. */
    List<String> list(UUID orgId);

    /** Delete a tenant's object. No-op if it doesn't exist. */
    void delete(UUID orgId, String key);

    /** An artifact's bytes + content type. */
    record Artifact(byte[] content, String contentType) {}
}

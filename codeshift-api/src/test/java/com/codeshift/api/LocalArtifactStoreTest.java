package com.codeshift.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeshift.bsg.ArtifactStore;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Filesystem artifact store: tenant isolation, round-trip, and key-traversal safety. */
class LocalArtifactStoreTest {

    private byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void roundTripsAndIsTenantIsolated(@TempDir Path dir) {
        LocalArtifactStore store = new LocalArtifactStore(dir.toString());
        UUID acme = UUID.randomUUID();
        UUID globex = UUID.randomUUID();

        store.put(acme, "report.json", bytes("{\"acme\":true}"), "application/json");

        // Owner reads it back with content type intact.
        ArtifactStore.Artifact a = store.get(acme, "report.json").orElseThrow();
        assertThat(new String(a.content(), StandardCharsets.UTF_8)).isEqualTo("{\"acme\":true}");
        assertThat(a.contentType()).isEqualTo("application/json");

        // Another tenant cannot see it.
        assertThat(store.get(globex, "report.json")).isEmpty();
        assertThat(store.list(globex)).isEmpty();

        // Listing shows the key, not the content-type sidecar.
        assertThat(store.list(acme)).containsExactly("report.json");

        store.delete(acme, "report.json");
        assertThat(store.get(acme, "report.json")).isEmpty();
    }

    @Test
    void rejectsPathTraversalKeys(@TempDir Path dir) {
        LocalArtifactStore store = new LocalArtifactStore(dir.toString());
        UUID org = UUID.randomUUID();

        assertThatThrownBy(() -> store.put(org, "../escape", bytes("x"), "text/plain"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.put(org, "a/b", bytes("x"), "text/plain"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.get(org, ".."))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

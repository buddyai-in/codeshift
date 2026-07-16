package com.codeshift.api;

import com.codeshift.bsg.ArtifactStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Filesystem-backed {@link ArtifactStore} — the dev / self-host adapter. Objects
 * live under {@code <root>/<orgId>/<key>}, so tenant isolation is a directory
 * boundary; a managed S3 adapter (bucket or per-tenant prefix) is a drop-in behind
 * the same port. Keys are restricted to a safe charset so nothing can traverse out
 * of its tenant directory.
 */
@Component
public class LocalArtifactStore implements ArtifactStore {

    private static final Logger log = LoggerFactory.getLogger(LocalArtifactStore.class);
    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9._-]{1,200}");
    private static final String CT_SUFFIX = ".__ct";

    private final Path root;

    public LocalArtifactStore(@Value("${codeshift.artifacts.dir:}") String dir) {
        try {
            this.root = (dir == null || dir.isBlank())
                    ? Files.createTempDirectory("codeshift-artifacts")
                    : Files.createDirectories(Path.of(dir));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot initialise artifact store", e);
        }
        log.info("Artifact store root: {}", root);
    }

    private static String safeKey(String key) {
        if (key == null || !SAFE_KEY.matcher(key).matches() || key.equals(".") || key.equals("..")
                || key.endsWith(CT_SUFFIX)) {
            throw new IllegalArgumentException(
                    "Invalid artifact key (allowed: letters, digits, . _ -, max 200 chars)");
        }
        return key;
    }

    private Path tenantDir(UUID orgId) {
        try {
            return Files.createDirectories(root.resolve(orgId.toString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void put(UUID orgId, String key, byte[] content, String contentType) {
        String k = safeKey(key);
        Path dir = tenantDir(orgId);
        try {
            Files.write(dir.resolve(k), content);
            Files.writeString(dir.resolve(k + CT_SUFFIX),
                    contentType == null ? "application/octet-stream" : contentType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store artifact " + key, e);
        }
    }

    @Override
    public Optional<Artifact> get(UUID orgId, String key) {
        String k = safeKey(key);
        Path file = tenantDir(orgId).resolve(k);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            byte[] content = Files.readAllBytes(file);
            Path ctFile = tenantDir(orgId).resolve(k + CT_SUFFIX);
            String ct = Files.exists(ctFile)
                    ? Files.readString(ctFile, StandardCharsets.UTF_8)
                    : "application/octet-stream";
            return Optional.of(new Artifact(content, ct));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read artifact " + key, e);
        }
    }

    @Override
    public List<String> list(UUID orgId) {
        Path dir = tenantDir(orgId);
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> !n.endsWith(CT_SUFFIX))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list artifacts", e);
        }
    }

    @Override
    public void delete(UUID orgId, String key) {
        String k = safeKey(key);
        Path dir = tenantDir(orgId);
        try {
            Files.deleteIfExists(dir.resolve(k));
            Files.deleteIfExists(dir.resolve(k + CT_SUFFIX));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete artifact " + key, e);
        }
    }
}

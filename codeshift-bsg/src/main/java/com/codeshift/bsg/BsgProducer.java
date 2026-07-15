package com.codeshift.bsg;

import com.codeshift.bsg.model.BsgGraph;
import java.util.List;

/**
 * Produces a Behavioral Specification Graph for a project. Implemented by the
 * Analysis Agent (LLM-backed, with a deterministic offline fallback).
 *
 * <p>Declared here — a neutral module both the orchestration graph and the agents
 * layer depend on — so the graph node can invoke analysis without depending on the
 * agents/model-gateway modules directly.
 */
public interface BsgProducer {

    /**
     * @param projectId   the run's project id
     * @param moduleIds   discovered module ids (leaf-first order recommended)
     * @param projectPath source root on disk, or {@code null} if unavailable
     * @return a BSG version (nodes + edges), pending human review
     */
    BsgGraph produce(String projectId, List<String> moduleIds, String projectPath);
}

package com.codeshift.agents;

import com.codeshift.bsg.BsgProducer;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import com.codeshift.common.ModelProfile;
import com.codeshift.gateway.ModelGateway;
import com.codeshift.parser.JavaProjectAnalyzer;
import com.codeshift.parser.ProjectAnalysis;
import com.codeshift.parser.SourceModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * Analysis Agent — the BSG builder (product doc §5, agent #2).
 *
 * <p>Reads the discovered modules and populates the Behavioral Specification Graph
 * with typed, confidence-scored nodes via <em>structured output</em> (never prose),
 * so a business analyst can review each rule and the AI's understanding is
 * auditable. Every rule enters {@code PENDING} human review — the trust boundary.
 *
 * <p>LLM-agnostic and offline-safe: when a model provider is configured it extracts
 * with the {@code REASONING} profile through the gateway; otherwise it falls back
 * to a deterministic skeleton so the pipeline (and {@code mvn verify}) runs with no
 * keys and no network.
 */
@Component
public class AnalysisAgent implements BsgProducer {

    private static final Logger log = LoggerFactory.getLogger(AnalysisAgent.class);
    private static final int MAX_FILES = 12;
    private static final int MAX_CHARS_PER_FILE = 1600;

    private final ModelGateway gateway;

    public AnalysisAgent(ModelGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public BsgGraph produce(String projectId, List<String> moduleIds, String projectPath) {
        if (gateway.isAvailable() && projectPath != null && !projectPath.isBlank()) {
            try {
                return llmExtract(projectId, projectPath);
            } catch (Exception e) {
                log.warn("LLM BSG extraction failed ({}); falling back to skeleton", e.toString());
            }
        }
        return skeleton(projectId, moduleIds);
    }

    // --- LLM path -----------------------------------------------------------

    private BsgGraph llmExtract(String projectId, String projectPath) {
        Path root = Path.of(projectPath);
        ProjectAnalysis analysis = JavaProjectAnalyzer.analyze(root);
        String sources = readSources(root, analysis);

        String prompt = """
                You are a software analyst extracting the *behavioral specification* of a legacy
                Java application: the business rules, data flows, state transitions, external
                contracts, edge cases and implicit rules that must be preserved during migration.

                For the sources below, extract discrete behavioral rules. For each: a stable
                nodeRef (BSG-001, BSG-002, ...), a nodeType (one of BusinessRule, DataFlow,
                StateTransition, ExternalContract, EdgeCase, ImplicitRule, MessagingContract),
                a short title, a plain-English description a business analyst can validate, the
                sourceLocation (file/class), and a confidence of HIGH, MEDIUM or LOW.

                Only state what the code actually does. Prefer fewer, high-quality rules.

                SOURCES:
                %s
                """.formatted(sources);

        List<ExtractedRule> rules = gateway.chat(ModelProfile.REASONING)
                .prompt()
                .user(prompt)
                .call()
                .entity(new ParameterizedTypeReference<List<ExtractedRule>>() {});

        if (rules == null || rules.isEmpty()) {
            return skeleton(projectId, analysis.moduleIds());
        }

        List<BsgNode> nodes = new ArrayList<>();
        int i = 1;
        for (ExtractedRule r : rules) {
            String ref = (r.nodeRef() == null || r.nodeRef().isBlank())
                    ? String.format("BSG-%03d", i) : r.nodeRef();
            nodes.add(BsgNode.extracted(ref, parseType(r.nodeType()), safe(r.title(), "Rule " + ref),
                    safe(r.description(), ""), r.sourceLocation(), parseConfidence(r.confidence())));
            i++;
        }
        log.info("Analysis Agent extracted {} BSG nodes for project {}", nodes.size(), projectId);
        return new BsgGraph(projectId, 1, nodes, List.of());
    }

    private String readSources(Path root, ProjectAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        analysis.modules().stream().limit(MAX_FILES).forEach(m -> {
            try {
                String body = Files.readString(root.resolve(m.filePath()));
                if (body.length() > MAX_CHARS_PER_FILE) {
                    body = body.substring(0, MAX_CHARS_PER_FILE) + "\n// ...truncated...";
                }
                sb.append("=== ").append(m.id()).append(" (").append(m.filePath()).append(") ===\n")
                        .append(body).append("\n\n");
            } catch (Exception ignored) {
                // skip unreadable file
            }
        });
        return sb.toString();
    }

    // --- Deterministic fallback --------------------------------------------

    /** One placeholder BusinessRule per module — a reviewable skeleton, no LLM. */
    private BsgGraph skeleton(String projectId, List<String> moduleIds) {
        List<BsgNode> nodes = new ArrayList<>();
        int i = 1;
        for (String id : moduleIds) {
            String simple = id.contains(".") ? id.substring(id.lastIndexOf('.') + 1) : id;
            nodes.add(BsgNode.extracted(
                    String.format("BSG-%03d", i++),
                    BsgNodeType.BUSINESS_RULE,
                    "Core behavior of " + simple,
                    "Placeholder rule for " + id + " — enable a model provider for full extraction.",
                    id,
                    BsgConfidence.LOW));
        }
        log.info("Analysis Agent produced a {}-node skeleton for project {} (no LLM)",
                nodes.size(), projectId);
        return new BsgGraph(projectId, 1, nodes, List.of());
    }

    // --- Mapping helpers ----------------------------------------------------

    private static BsgNodeType parseType(String raw) {
        if (raw == null) {
            return BsgNodeType.BUSINESS_RULE;
        }
        String norm = raw.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        for (BsgNodeType t : BsgNodeType.values()) {
            if (t.name().replace("_", "").equals(norm)) {
                return t;
            }
        }
        return BsgNodeType.BUSINESS_RULE;
    }

    private static BsgConfidence parseConfidence(String raw) {
        if (raw == null) {
            return BsgConfidence.MEDIUM;
        }
        try {
            return BsgConfidence.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BsgConfidence.MEDIUM;
        }
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

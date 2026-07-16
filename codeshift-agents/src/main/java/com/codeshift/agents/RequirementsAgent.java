package com.codeshift.agents;

import com.codeshift.bsg.RequirementsProducer;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import com.codeshift.common.BsgOrigin;
import com.codeshift.common.HumanStatus;
import com.codeshift.common.ModelProfile;
import com.codeshift.common.NewCodeMode;
import com.codeshift.gateway.ModelGateway;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * Requirements Agent — the entry point for new-code addition (product doc §7).
 *
 * <p>Reads the current BSG plus a plain-English feature request and produces new
 * {@code NEW_FEATURE} rules, appended as a new BSG version. Because the whole
 * application history lives in the BSG, every added feature is traceable to a
 * human-approved request — the compliance audit trail no other dev tool provides.
 *
 * <p>LLM-agnostic with a deterministic fallback: with a model provider it extracts
 * structured rules from the request; without one it records the request as a
 * single NEW_FEATURE node so the flow works offline.
 */
@Component
public class RequirementsAgent implements RequirementsProducer {

    private static final Logger log = LoggerFactory.getLogger(RequirementsAgent.class);

    private final ModelGateway gateway;

    public RequirementsAgent(ModelGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public BsgGraph addFeature(BsgGraph currentBsg, String featureRequest, NewCodeMode mode,
            int newVersionNumber) {
        int start = nextFeatureNumber(currentBsg);
        List<BsgNode> newNodes = gateway.isAvailable()
                ? llmRules(currentBsg, featureRequest, mode, start)
                : List.of();
        if (newNodes.isEmpty()) {
            newNodes = List.of(skeletonRule(featureRequest, mode, start));
        }

        List<BsgNode> merged = new ArrayList<>(currentBsg.nodes());
        merged.addAll(newNodes);
        log.info("Requirements Agent added {} {} node(s) as version {}",
                newNodes.size(), originFor(mode), newVersionNumber);
        return new BsgGraph(currentBsg.projectId(), newVersionNumber, merged, currentBsg.edges());
    }

    /** The BSG origin implied by a new-code mode (product doc §7.2 node origins). */
    private static BsgOrigin originFor(NewCodeMode mode) {
        return switch (mode) {
            case INTEGRATION -> BsgOrigin.INTEGRATION;
            case ARCHITECTURE -> BsgOrigin.REFACTORED;
            case FEATURE, GREENFIELD -> BsgOrigin.NEW_FEATURE;
        };
    }

    private static BsgNodeType defaultTypeFor(NewCodeMode mode) {
        return switch (mode) {
            case INTEGRATION -> BsgNodeType.EXTERNAL_CONTRACT;
            case ARCHITECTURE -> BsgNodeType.STATE_TRANSITION;
            case FEATURE, GREENFIELD -> BsgNodeType.BUSINESS_RULE;
        };
    }

    private List<BsgNode> llmRules(BsgGraph currentBsg, String featureRequest, NewCodeMode mode,
            int start) {
        try {
            String existing = currentBsg.nodes().stream()
                    .map(n -> n.nodeRef() + ": " + n.title()).limit(30)
                    .reduce((a, b) -> a + "\n" + b).orElse("(none)");
            String prompt = """
                    You are extending an existing application. Below is its current behavioral
                    specification (BSG) and a new feature request. Produce the NEW behavioral rules
                    the feature introduces — do not repeat existing rules. For each: a nodeType
                    (BusinessRule, DataFlow, StateTransition, ExternalContract, EdgeCase,
                    ImplicitRule, MessagingContract), a short title, a plain-English description,
                    a sourceLocation (target class), and a confidence (HIGH/MEDIUM/LOW).

                    CURRENT BSG:
                    %s

                    FEATURE REQUEST:
                    %s
                    """.formatted(existing, featureRequest);

            List<ExtractedRule> rules = gateway.chat(ModelProfile.REASONING)
                    .prompt().user(prompt).call()
                    .entity(new ParameterizedTypeReference<List<ExtractedRule>>() {});
            if (rules == null) {
                return List.of();
            }
            List<BsgNode> nodes = new ArrayList<>();
            int i = start;
            for (ExtractedRule r : rules) {
                nodes.add(new BsgNode(String.format("BSG-F%03d", i++), parseType(r.nodeType()),
                        safe(r.title(), "New feature"), safe(r.description(), featureRequest),
                        r.sourceLocation(), parseConfidence(r.confidence()),
                        HumanStatus.PENDING, originFor(mode), null, false));
            }
            return nodes;
        } catch (Exception e) {
            log.warn("LLM feature extraction failed ({}); using skeleton", e.toString());
            return List.of();
        }
    }

    private static BsgNode skeletonRule(String featureRequest, NewCodeMode mode, int number) {
        String title = featureRequest.length() > 60 ? featureRequest.substring(0, 60) + "…" : featureRequest;
        String prefix = switch (mode) {
            case INTEGRATION -> "Integration: ";
            case ARCHITECTURE -> "Refactor: ";
            case GREENFIELD -> "Greenfield: ";
            case FEATURE -> "Feature: ";
        };
        return new BsgNode(String.format("BSG-F%03d", number), defaultTypeFor(mode),
                prefix + title, featureRequest, null, BsgConfidence.MEDIUM,
                HumanStatus.PENDING, originFor(mode), null, false);
    }

    private static int nextFeatureNumber(BsgGraph bsg) {
        return bsg.nodes().stream()
                .map(BsgNode::nodeRef)
                .filter(r -> r.startsWith("BSG-F"))
                .map(r -> r.substring(5).replaceAll("\\D", ""))
                .filter(s -> !s.isBlank())
                .mapToInt(Integer::parseInt).max().orElse(0) + 1;
    }

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
        try {
            return raw == null ? BsgConfidence.MEDIUM
                    : BsgConfidence.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BsgConfidence.MEDIUM;
        }
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

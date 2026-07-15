package com.codeshift.agents;

import com.codeshift.bsg.ArchitectureProducer;
import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.ArchitecturePlan.MigrationPhase;
import com.codeshift.bsg.model.ArchitecturePlan.ModuleMapping;
import com.codeshift.bsg.model.ArchitecturePlan.ServiceBoundary;
import com.codeshift.bsg.model.BsgGraph;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/**
 * Architecture Agent (product doc §5, agent #3). Reads the approved BSG + target
 * stack and proposes a layered architecture: module→class mapping, microservice
 * boundaries (clustered by top-level package), and migration phases in dependency
 * order (repositories/domain first, then services, then controllers/web).
 *
 * <p>Deterministic layer inference from naming — the reliable, cheap baseline. An
 * LLM refinement (boundary proposals for ambiguous graphs) plugs in behind the
 * same {@link ArchitectureProducer} port exactly as the Analysis Agent does.
 */
@Component
public class ArchitectureAgent implements ArchitectureProducer {

    @Override
    public ArchitecturePlan produce(BsgGraph approvedBsg, List<String> topoOrder, String targetStack) {
        String stack = targetStack == null || targetStack.isBlank()
                ? "JAVA_21_SPRING_BOOT" : targetStack;

        List<ModuleMapping> mappings = new ArrayList<>();
        Map<String, List<String>> byPackage = new LinkedHashMap<>();
        Map<Integer, List<String>> byLayerRank = new TreeMap<>();

        for (String moduleId : topoOrder) {
            String simple = simpleName(moduleId);
            String layer = inferLayer(simple);
            mappings.add(new ModuleMapping(moduleId, simple, layer));
            byPackage.computeIfAbsent(topPackage(moduleId), k -> new ArrayList<>()).add(moduleId);
            byLayerRank.computeIfAbsent(layerRank(layer), k -> new ArrayList<>()).add(moduleId);
        }

        // Microservice boundaries: one per top-level package cluster.
        List<ServiceBoundary> services = new ArrayList<>();
        byPackage.forEach((pkg, ids) -> services.add(new ServiceBoundary(serviceName(pkg), ids)));

        // Migration phases: by layer rank (leaf layers first).
        List<MigrationPhase> phases = new ArrayList<>();
        int order = 1;
        for (Map.Entry<Integer, List<String>> e : byLayerRank.entrySet()) {
            phases.add(new MigrationPhase(order, layerName(e.getKey()), e.getValue()));
            order++;
        }

        return new ArchitecturePlan(stack, mappings, services, phases);
    }

    private static String inferLayer(String simpleName) {
        String n = simpleName.toLowerCase();
        if (n.endsWith("controller") || n.endsWith("resource") || n.endsWith("endpoint")) {
            return "CONTROLLER";
        }
        if (n.endsWith("service") || n.endsWith("manager") || n.endsWith("usecase")) {
            return "SERVICE";
        }
        if (n.endsWith("repository") || n.endsWith("dao") || n.endsWith("mapper")) {
            return "REPOSITORY";
        }
        if (n.endsWith("publisher") || n.endsWith("listener") || n.endsWith("consumer")
                || n.endsWith("producer")) {
            return "MESSAGING";
        }
        if (n.endsWith("controller") || n.endsWith("entity") || n.endsWith("model")
                || n.endsWith("dto") || n.endsWith("rule")) {
            return "DOMAIN";
        }
        return "DOMAIN";
    }

    // Lower rank = migrated earlier (fewer dependencies).
    private static int layerRank(String layer) {
        return switch (layer) {
            case "REPOSITORY", "DOMAIN" -> 1;
            case "MESSAGING" -> 2;
            case "SERVICE" -> 3;
            case "CONTROLLER" -> 4;
            default -> 5;
        };
    }

    private static String layerName(int rank) {
        return switch (rank) {
            case 1 -> "Data & domain layer";
            case 2 -> "Messaging layer";
            case 3 -> "Service layer";
            case 4 -> "Web / API layer";
            default -> "Remaining modules";
        };
    }

    private static String simpleName(String moduleId) {
        return moduleId.contains(".") ? moduleId.substring(moduleId.lastIndexOf('.') + 1) : moduleId;
    }

    private static String topPackage(String moduleId) {
        String[] parts = moduleId.split("\\.");
        if (parts.length <= 1) {
            return "(root)";
        }
        // Use the segment after a common com.acme.* prefix when present, else first segment.
        return parts.length >= 3 ? parts[parts.length - 2] : parts[0];
    }

    private static String serviceName(String pkg) {
        return pkg.substring(0, 1).toUpperCase() + pkg.substring(1) + "Service";
    }
}

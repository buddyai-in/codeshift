package com.codeshift.bsg.model;

import java.io.Serializable;
import java.util.List;

/**
 * The target architecture the Architecture Agent proposes from an approved BSG
 * (product doc §5, agent #3): module→class mapping, layered design, microservice
 * boundaries, and migration phases in dependency order. Reviewed at gate #2.
 *
 * <p>{@code Serializable} so it can live in durable run state.
 */
public record ArchitecturePlan(
        String targetStack,
        List<ModuleMapping> moduleMappings,
        List<ServiceBoundary> microservices,
        List<MigrationPhase> phases) implements Serializable {

    /** One legacy module mapped to a target class + layer. */
    public record ModuleMapping(String moduleId, String targetClass, String layer)
            implements Serializable {}

    /** A proposed microservice: a named cluster of modules. */
    public record ServiceBoundary(String name, List<String> moduleIds) implements Serializable {}

    /** A migration phase: modules migrated together, in dependency order. */
    public record MigrationPhase(int order, String name, List<String> moduleIds)
            implements Serializable {}
}

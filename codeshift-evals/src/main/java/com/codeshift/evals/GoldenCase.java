package com.codeshift.evals;

import java.util.List;
import java.util.Set;

/**
 * One labelled example in a BSG-extraction golden corpus: a known input and the
 * behavioral units a correct extraction must cover.
 *
 * <p>Coverage is scored on {@code expectedSourceLocations} — the classes/modules a
 * correct BSG must have at least one rule about. Keying on source location keeps the
 * eval robust to a provider legitimately producing more (or differently titled)
 * rules per unit, while still catching regressions that drop whole units.
 *
 * @param name                    human-readable case id
 * @param projectId               project id handed to the producer
 * @param moduleIds               discovered modules (the producer's input)
 * @param expectedSourceLocations units a correct extraction must cover
 */
public record GoldenCase(String name, String projectId, List<String> moduleIds,
        Set<String> expectedSourceLocations) {
}

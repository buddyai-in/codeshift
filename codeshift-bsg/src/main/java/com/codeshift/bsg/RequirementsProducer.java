package com.codeshift.bsg;

import com.codeshift.bsg.model.BsgGraph;

/**
 * New-code addition (product doc §7): turns a natural-language feature request
 * into new {@code NEW_FEATURE} BSG nodes appended to the current BSG as a new
 * version. Implemented by the Requirements Agent.
 */
public interface RequirementsProducer {

    /**
     * @param currentBsg       the project's current (approved) BSG
     * @param featureRequest   plain-English request, e.g. "when an order ships, send an SMS"
     * @param newVersionNumber the version number for the resulting BSG
     * @return the current BSG plus new NEW_FEATURE nodes, as a new version
     */
    BsgGraph addFeature(BsgGraph currentBsg, String featureRequest, int newVersionNumber);
}

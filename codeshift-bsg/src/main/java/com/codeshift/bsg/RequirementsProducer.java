package com.codeshift.bsg;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.common.NewCodeMode;

/**
 * New-code addition (product doc §7): turns a natural-language request into new
 * BSG nodes appended to the current BSG as a new version. Implemented by the
 * Requirements Agent. The {@link NewCodeMode} controls the origin/type of the
 * new nodes (feature / integration / architecture evolution / greenfield).
 */
public interface RequirementsProducer {

    BsgGraph addFeature(BsgGraph currentBsg, String featureRequest, NewCodeMode mode,
            int newVersionNumber);
}

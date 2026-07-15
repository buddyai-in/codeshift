package com.codeshift.bsg;

import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.TransformationResult;
import java.util.List;

/**
 * Produces migrated code + tests from an approved BSG and architecture.
 * Implemented by the Transformation + Test Generation agents.
 */
public interface TransformationProducer {

    TransformationResult produce(BsgGraph approvedBsg, ArchitecturePlan architecture,
            List<String> topoOrder);
}

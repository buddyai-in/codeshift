package com.codeshift.bsg;

import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.BsgGraph;
import java.util.List;

/**
 * Produces a target {@link ArchitecturePlan} from an approved BSG. Implemented by
 * the Architecture Agent. Lives here (the neutral module) so the graph can invoke
 * it without depending on the agents layer.
 */
public interface ArchitectureProducer {

    /**
     * @param approvedBsg the human-approved BSG
     * @param topoOrder   module ids in leaf-first order
     * @param targetStack e.g. "JAVA_21_SPRING_BOOT"
     */
    ArchitecturePlan produce(BsgGraph approvedBsg, List<String> topoOrder, String targetStack);
}

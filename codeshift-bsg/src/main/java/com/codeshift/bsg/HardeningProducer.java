package com.codeshift.bsg;

import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.HardeningResult;
import com.codeshift.bsg.model.TransformationResult;
import java.util.List;

/**
 * Runs the hardening branch — security scan, DevOps bundle, messaging plan.
 * Implemented by the Security / Cloud / Messaging agents.
 */
public interface HardeningProducer {

    HardeningResult produce(ArchitecturePlan architecture, TransformationResult transformation,
            String projectPath, List<String> messagingSystems);
}

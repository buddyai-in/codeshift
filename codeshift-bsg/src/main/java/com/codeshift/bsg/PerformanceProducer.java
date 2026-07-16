package com.codeshift.bsg;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.PerformanceReport;

/** Performance optimisation recommendations — implemented by the Performance Agent. */
public interface PerformanceProducer {

    PerformanceReport analyze(BsgGraph bsg);
}

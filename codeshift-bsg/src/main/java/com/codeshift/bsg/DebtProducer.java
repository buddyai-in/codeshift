package com.codeshift.bsg;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.DebtReport;

/** Technical Debt Intelligence — implemented by the Debt Agent. */
public interface DebtProducer {

    /** @param previous the prior BSG version, or {@code null} for the first version. */
    DebtReport analyze(BsgGraph current, BsgGraph previous);
}

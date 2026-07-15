package com.codeshift.bsg;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.TransformationResult;
import com.codeshift.bsg.model.ValidationReport;

/** Validates generated code against the BSG. Implemented by the Validation Agent. */
public interface ValidationProducer {

    ValidationReport validate(BsgGraph approvedBsg, TransformationResult transformation);
}

package com.codeshift.common;

/** The kinds of behavioral specification the BSG captures (product doc §4.1). */
public enum BsgNodeType {
    BUSINESS_RULE,
    DATA_FLOW,
    STATE_TRANSITION,
    EXTERNAL_CONTRACT,
    EDGE_CASE,
    IMPLICIT_RULE,
    MESSAGING_CONTRACT
}

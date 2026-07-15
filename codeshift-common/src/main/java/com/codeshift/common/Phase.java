package com.codeshift.common;

/** Where a migration run currently sits in the master graph. */
public enum Phase {
    DISCOVERY,
    ANALYSIS,
    BSG_REVIEW,
    ARCHITECTURE,
    ARCH_REVIEW,
    BUILD,
    VALIDATION,
    HARDENING,
    DELIVERY,
    DONE
}

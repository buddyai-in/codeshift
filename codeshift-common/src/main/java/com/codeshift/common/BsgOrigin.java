package com.codeshift.common;

/** Where a BSG node came from — powers the compliance audit trail (product doc §7.2). */
public enum BsgOrigin {
    MIGRATED,
    NEW_FEATURE,
    INTEGRATION,
    REFACTORED
}

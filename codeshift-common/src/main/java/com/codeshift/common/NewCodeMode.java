package com.codeshift.common;

/** The four modes of new code addition (product doc §7.1). */
public enum NewCodeMode {
    /** New capability on the existing system. */
    FEATURE,
    /** Connect to an external system not in the legacy app. */
    INTEGRATION,
    /** Structural change to the existing system. */
    ARCHITECTURE,
    /** Brand new business capability / module. */
    GREENFIELD
}

package com.codeshift.compliance;

/** A vertical compliance standard the platform ships a control pack for. */
public enum ComplianceStandard {

    PCI_DSS("PCI-DSS v4.0", "Payment Card Industry Data Security Standard"),
    HIPAA("HIPAA Security Rule", "Health Insurance Portability and Accountability Act");

    private final String reference;
    private final String description;

    ComplianceStandard(String reference, String description) {
        this.reference = reference;
        this.description = description;
    }

    public String reference() {
        return reference;
    }

    public String description() {
        return description;
    }
}

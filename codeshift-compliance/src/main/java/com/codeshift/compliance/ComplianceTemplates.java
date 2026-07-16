package com.codeshift.compliance;

import com.codeshift.bsg.model.BsgNode;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import java.util.ArrayList;
import java.util.List;

/**
 * The built-in compliance control packs — banking (PCI-DSS) and healthcare (HIPAA).
 *
 * <p>Two uses: {@link #controlsFor} drives the coverage report (does the migrated
 * BSG reflect each required control?), and {@link #templateNodes} emits ready-made
 * BSG nodes a vertical project can seed so a regulated system starts with its
 * mandatory behavioral contracts already in the spec.
 */
public final class ComplianceTemplates {

    private ComplianceTemplates() {}

    public static List<ComplianceControl> controlsFor(ComplianceStandard standard) {
        return switch (standard) {
            case PCI_DSS -> pciControls();
            case HIPAA -> hipaaControls();
        };
    }

    /** Emit each control as a BSG node (an external contract) for seeding a project. */
    public static List<BsgNode> templateNodes(ComplianceStandard standard) {
        List<BsgNode> nodes = new ArrayList<>();
        int i = 1;
        for (ComplianceControl c : controlsFor(standard)) {
            nodes.add(BsgNode.extracted(
                    String.format("BSG-%s-%02d", standard.name().replace("_", ""), i++),
                    BsgNodeType.EXTERNAL_CONTRACT,
                    c.title() + " (" + c.id() + ")",
                    c.requirement(),
                    standard.reference(),
                    BsgConfidence.HIGH));
        }
        return nodes;
    }

    private static List<ComplianceControl> pciControls() {
        return List.of(
                new ComplianceControl("PCI-3.4", "Render PAN unreadable",
                        "Stored cardholder data (PAN) must be rendered unreadable, e.g. strong encryption.",
                        List.of("pan", "cardholder", "encrypt stored", "card number"),
                        "Add an encryption-at-rest rule for the cardholder data store."),
                new ComplianceControl("PCI-3.3", "Mask PAN on display",
                        "PAN must be masked when displayed; only authorised roles see the full number.",
                        List.of("mask", "masked pan", "truncate card"),
                        "Add a data-flow rule masking the card number on read/display."),
                new ComplianceControl("PCI-4.1", "Encrypt PAN in transit",
                        "Cardholder data must be encrypted over open/public networks (TLS).",
                        List.of("tls", "encrypt in transit", "https", "transport encryption"),
                        "Add an external-contract rule requiring TLS for card data endpoints."),
                new ComplianceControl("PCI-8.2", "Authenticate all access",
                        "Every user is uniquely identified and authenticated before access to card data.",
                        List.of("authenticate", "authentication", "login", "credential"),
                        "Add an authentication rule guarding card-data operations."),
                new ComplianceControl("PCI-10.2", "Audit trail of access",
                        "All access to cardholder data is logged to an immutable audit trail.",
                        List.of("audit", "audit log", "access log", "immutable log"),
                        "Add an audit-logging rule for every cardholder-data access."));
    }

    private static List<ComplianceControl> hipaaControls() {
        return List.of(
                new ComplianceControl("HIPAA-164.312(a)(1)", "Access control on ePHI",
                        "Technical access controls restrict ePHI to authorised persons/software.",
                        List.of("access control", "authorization", "role", "ephi", "phi access"),
                        "Add an access-control rule scoping ePHI to authorised roles."),
                new ComplianceControl("HIPAA-164.312(a)(2)(iv)", "Encrypt ePHI at rest",
                        "ePHI is encrypted at rest so stored records are unreadable if exfiltrated.",
                        List.of("encrypt", "encryption at rest", "ephi", "phi encryption"),
                        "Add an encryption-at-rest rule for the ePHI store."),
                new ComplianceControl("HIPAA-164.312(b)", "Audit controls",
                        "Record and examine activity in systems that contain or use ePHI.",
                        List.of("audit", "audit log", "activity log", "access log"),
                        "Add an audit-logging rule for ePHI activity."),
                new ComplianceControl("HIPAA-164.312(e)(1)", "Transmission security",
                        "Guard against unauthorised access to ePHI transmitted over a network.",
                        List.of("tls", "encrypt in transit", "transmission security", "https"),
                        "Add a transmission-security rule (TLS) for ePHI in transit."),
                new ComplianceControl("HIPAA-164.312(c)(1)", "Integrity of ePHI",
                        "Protect ePHI from improper alteration or destruction.",
                        List.of("integrity", "checksum", "tamper", "hash"),
                        "Add an integrity-verification rule for ePHI records."));
    }
}

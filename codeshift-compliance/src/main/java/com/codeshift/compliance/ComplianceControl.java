package com.codeshift.compliance;

import java.util.List;

/**
 * One required control in a compliance pack.
 *
 * @param id          standard control id (e.g. PCI-3.4, HIPAA-164.312(a))
 * @param title       short control name
 * @param requirement plain-English requirement a BSG rule must reflect
 * @param signals     lowercase keywords whose presence in a BSG node satisfies the control
 * @param remediation what to add when the control is missing
 */
public record ComplianceControl(String id, String title, String requirement,
        List<String> signals, String remediation) {
}

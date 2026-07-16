package com.codeshift.common;

/**
 * Where a tenant's model runs.
 *
 * <ul>
 *   <li>{@link #CLOUD} — the platform's managed provider (default).</li>
 *   <li>{@link #ON_PREM} — a tenant-hosted, OpenAI-compatible endpoint on their own hardware.</li>
 *   <li>{@link #IN_VPC} — a private endpoint inside the tenant's cloud VPC (no public egress).</li>
 * </ul>
 *
 * <p>Because every call goes through the model gateway (Spring AI over one
 * {@code ChatModel} interface), routing to a self-hosted/in-VPC endpoint is a
 * base-URL + model change, not a code change.
 */
public enum ModelDeploymentType {
    CLOUD,
    ON_PREM,
    IN_VPC
}

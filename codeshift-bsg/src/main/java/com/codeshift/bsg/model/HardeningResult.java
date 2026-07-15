package com.codeshift.bsg.model;

import java.io.Serializable;
import java.util.List;

/**
 * Output of the hardening branch (product doc §9): Security Agent findings, the
 * Cloud/DevOps deployment bundle, and the Messaging Agent's Kafka plan. These run
 * on every validated migration.
 */
public record HardeningResult(
        SecurityReport security,
        DevOpsBundle devops,
        MessagingPlan messaging) implements Serializable {

    public record SecurityReport(List<Finding> findings, int highCount) implements Serializable {
        public record Finding(String severity, String message, String location)
                implements Serializable {}
    }

    /** Generated deployment artifacts (Cloud Agent). */
    public record DevOpsBundle(String dockerfile, String kubernetesManifest, String ciPipeline)
            implements Serializable {}

    /** MQ → Kafka proposal (Messaging Agent). */
    public record MessagingPlan(List<String> sourceSystems, List<TopicProposal> topics)
            implements Serializable {
        public record TopicProposal(String name, int partitions, String partitionKey,
                String consumerGroup) implements Serializable {}
    }
}

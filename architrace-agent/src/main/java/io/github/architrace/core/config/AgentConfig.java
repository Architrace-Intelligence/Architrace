/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.core.config;

import io.github.architrace.span.AttributeMapping;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record AgentConfig(
        String project,
        String environment,
        String cluster,
        String agentName,
        ControlPlaneSettings controlPlane,
        OtlpSettings otlp,
        SnapshotSettings snapshot,
        BufferSettings buffers,
        MetricsSettings metrics,
        TopicSettings topics,
        AttributeMapping attributeMapping) {

    public static final String DEFAULT_PROJECT = "default";
    public static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(5);
    public static final int DEFAULT_OTLP_PORT = 4319;
    public static final Duration DEFAULT_SNAPSHOT_INTERVAL = Duration.ofSeconds(60);
    public static final int DEFAULT_QUEUE_SIZE = 64;
    public static final int DEFAULT_RING_SIZE = 65_536;
    public static final Duration DEFAULT_PENDING_TTL = Duration.ofSeconds(120);
    public static final int DEFAULT_METRICS_PORT = 9464;
    public static final List<String> DEFAULT_IGNORED_TOPICS = List.of(
            "*-changelog", "*-repartition", "*-subscription-registration-*topic", "*-subscription-response-*topic");

    public AgentConfig {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(cluster, "cluster");
        Objects.requireNonNull(agentName, "agentName");
        Objects.requireNonNull(controlPlane, "controlPlane");
        Objects.requireNonNull(otlp, "otlp");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(buffers, "buffers");
        Objects.requireNonNull(metrics, "metrics");
        Objects.requireNonNull(topics, "topics");
        Objects.requireNonNull(attributeMapping, "attributeMapping");
    }

    public record ControlPlaneSettings(String server, Duration retryDelay) {
        public ControlPlaneSettings {
            Objects.requireNonNull(server, "server");
            Objects.requireNonNull(retryDelay, "retryDelay");
        }
    }

    public record OtlpSettings(int port) {}

    public record SnapshotSettings(Duration interval, int queueSize) {
        public SnapshotSettings {
            Objects.requireNonNull(interval, "interval");
        }
    }

    public record BufferSettings(int ringSize, Duration pendingTtl) {
        public BufferSettings {
            Objects.requireNonNull(pendingTtl, "pendingTtl");
        }
    }

    public record MetricsSettings(int port) {}

    public record TopicSettings(List<String> ignore) {
        public TopicSettings {
            ignore = List.copyOf(ignore);
        }
    }
}

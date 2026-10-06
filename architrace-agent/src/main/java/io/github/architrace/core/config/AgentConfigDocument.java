/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.core.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.architrace.core.config.AgentConfig.BufferSettings;
import io.github.architrace.core.config.AgentConfig.ControlPlaneSettings;
import io.github.architrace.core.config.AgentConfig.MetricsSettings;
import io.github.architrace.core.config.AgentConfig.OtlpSettings;
import io.github.architrace.core.config.AgentConfig.SnapshotSettings;
import io.github.architrace.core.config.AgentConfig.TopicSettings;
import io.github.architrace.grpc.GrpcAddressParser;
import io.github.architrace.span.AttributeMapping;
import io.github.architrace.span.MappedField;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonInclude(JsonInclude.Include.NON_NULL)
record AgentConfigDocument(
        String project,
        String environment,
        String cluster,
        AgentSection agent,
        @JsonProperty("control-plane") ControlPlaneSection controlPlane,
        OtlpSection otlp,
        SnapshotSection snapshot,
        BuffersSection buffers,
        MetricsSection metrics,
        TopicsSection topics,
        @JsonProperty("attribute-mapping") Map<String, List<String>> attributeMapping) {

    private static final int MAX_PORT = 65_535;

    record AgentSection(String name) {}

    record ControlPlaneSection(
            String server, @JsonProperty("retry-seconds") Long retrySeconds) {}

    record OtlpSection(Integer port) {}

    record SnapshotSection(
            @JsonProperty("interval-seconds") Long intervalSeconds,
            @JsonProperty("queue-size") Integer queueSize) {}

    record BuffersSection(
            @JsonProperty("ring-size") Integer ringSize,
            @JsonProperty("pending-ttl-seconds") Long pendingTtlSeconds) {}

    record MetricsSection(Integer port) {}

    record TopicsSection(List<String> ignore) {}

    static AgentConfigDocument of(AgentConfig config) {
        return new AgentConfigDocument(
                config.project(),
                config.environment(),
                config.cluster(),
                new AgentSection(config.agentName()),
                new ControlPlaneSection(
                        config.controlPlane().server(),
                        config.controlPlane().retryDelay().toSeconds()),
                new OtlpSection(config.otlp().port()),
                new SnapshotSection(
                        config.snapshot().interval().toSeconds(),
                        config.snapshot().queueSize()),
                new BuffersSection(
                        config.buffers().ringSize(),
                        config.buffers().pendingTtl().toSeconds()),
                new MetricsSection(config.metrics().port()),
                new TopicsSection(config.topics().ignore()),
                config.attributeMapping().byConfigKey());
    }

    List<String> problems() {
        String server = section(controlPlane, ControlPlaneSection::server);
        Stream<Optional<String>> checks = Stream.of(
                missingIfBlank(environment, "environment"),
                missingIfBlank(cluster, "cluster"),
                missingIfBlank(section(agent, AgentSection::name), "agent.name"),
                missingIfBlank(server, "control-plane.server"),
                invalidIf(project != null && project.isBlank(), "project must not be blank"),
                invalidIf(!isHostPort(server), "control-plane.server must be host:port"),
                positive(section(controlPlane, ControlPlaneSection::retrySeconds), "control-plane.retry-seconds"),
                port(section(otlp, OtlpSection::port), "otlp.port"),
                positive(section(snapshot, SnapshotSection::intervalSeconds), "snapshot.interval-seconds"),
                positive(section(snapshot, SnapshotSection::queueSize), "snapshot.queue-size"),
                positive(section(buffers, BuffersSection::ringSize), "buffers.ring-size"),
                positive(section(buffers, BuffersSection::pendingTtlSeconds), "buffers.pending-ttl-seconds"),
                port(section(metrics, MetricsSection::port), "metrics.port"),
                ignoredTopicsProblem());
        return Stream.concat(checks, mappingProblems())
                .flatMap(Optional::stream)
                .toList();
    }

    AgentConfig toConfig() {
        return new AgentConfig(
                Objects.requireNonNullElse(project, AgentConfig.DEFAULT_PROJECT),
                environment,
                cluster,
                agent.name(),
                new ControlPlaneSettings(
                        controlPlane.server(), seconds(controlPlane.retrySeconds(), AgentConfig.DEFAULT_RETRY_DELAY)),
                new OtlpSettings(orDefault(section(otlp, OtlpSection::port), AgentConfig.DEFAULT_OTLP_PORT)),
                new SnapshotSettings(
                        seconds(
                                section(snapshot, SnapshotSection::intervalSeconds),
                                AgentConfig.DEFAULT_SNAPSHOT_INTERVAL),
                        orDefault(section(snapshot, SnapshotSection::queueSize), AgentConfig.DEFAULT_QUEUE_SIZE)),
                new BufferSettings(
                        orDefault(section(buffers, BuffersSection::ringSize), AgentConfig.DEFAULT_RING_SIZE),
                        seconds(section(buffers, BuffersSection::pendingTtlSeconds), AgentConfig.DEFAULT_PENDING_TTL)),
                new MetricsSettings(
                        orDefault(section(metrics, MetricsSection::port), AgentConfig.DEFAULT_METRICS_PORT)),
                new TopicSettings(
                        orDefault(section(topics, TopicsSection::ignore), AgentConfig.DEFAULT_IGNORED_TOPICS)),
                AttributeMapping.defaults().with(mappingOverrides()));
    }

    private Optional<String> ignoredTopicsProblem() {
        List<String> patterns = section(topics, TopicsSection::ignore);
        boolean blank = patterns != null && patterns.stream().anyMatch(AgentConfigDocument::isBlank);
        return invalidIf(blank, "topics.ignore must not contain blank patterns");
    }

    private Stream<Optional<String>> mappingProblems() {
        return attributeMapping == null
                ? Stream.empty()
                : attributeMapping.entrySet().stream().map(entry -> mappingProblem(entry.getKey(), entry.getValue()));
    }

    private Map<MappedField, List<String>> mappingOverrides() {
        return attributeMapping == null
                ? Map.of()
                : attributeMapping.entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> MappedField.byConfigKey(entry.getKey()).orElseThrow(),
                                Map.Entry::getValue,
                                (first, _) -> first,
                                () -> new EnumMap<>(MappedField.class)));
    }

    private static Optional<String> mappingProblem(String field, List<String> keys) {
        if (MappedField.byConfigKey(field).isEmpty()) {
            return Optional.of(
                    "Unknown attribute-mapping field: " + field + " (known fields: " + MappedField.configKeys() + ")");
        }
        boolean usable = keys != null && !keys.isEmpty() && keys.stream().noneMatch(AgentConfigDocument::isBlank);
        return invalidIf(!usable, "attribute-mapping." + field + " must list at least one attribute key");
    }

    private static <S, T> T section(S section, Function<S, T> getter) {
        return section == null ? null : getter.apply(section);
    }

    private static Duration seconds(Long value, Duration fallback) {
        return value == null ? fallback : Duration.ofSeconds(value);
    }

    private static <T> T orDefault(T value, T fallback) {
        return Objects.requireNonNullElse(value, fallback);
    }

    private static Optional<String> missingIfBlank(String value, String field) {
        return isBlank(value) ? Optional.of("Missing required config field: " + field) : Optional.empty();
    }

    private static Optional<String> invalidIf(boolean invalid, String problem) {
        return invalid ? Optional.of("Invalid config field: " + problem) : Optional.empty();
    }

    private static Optional<String> positive(Number value, String field) {
        return invalidIf(value != null && value.longValue() <= 0, field + " must be > 0");
    }

    private static Optional<String> port(Integer value, String field) {
        boolean outOfRange = value != null && (value < 1 || value > MAX_PORT);
        return invalidIf(outOfRange, field + " must be between 1 and " + MAX_PORT);
    }

    private static boolean isHostPort(String server) {
        if (isBlank(server)) {
            return true;
        }
        try {
            GrpcAddressParser.parseHostPort(server);
            return true;
        } catch (IllegalArgumentException _) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

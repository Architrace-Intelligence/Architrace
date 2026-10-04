/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.architrace.span.AttributeMapping;
import io.github.architrace.span.MappedField;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentConfigLoaderTest {

    private static final String FULL = """
            project: webshop
            environment: PROD
            cluster: eu-1
            agent:
              name: eu-1-agent
            control-plane:
              server: control-plane:9090
              retry-seconds: 7
            otlp:
              port: 4317
            snapshot:
              interval-seconds: 30
              queue-size: 128
            buffers:
              ring-size: 1024
              pending-ttl-seconds: 45
            metrics:
              port: 9999
            attribute-mapping:
              domain: [service.namespace, team]
            """;

    private static final String MINIMAL = """
            environment: DEV
            cluster: local
            agent:
              name: dev-agent
            control-plane:
              server: localhost:9090
            """;

    @TempDir
    Path tempDir;

    private final AgentConfigLoader sut = new AgentConfigLoader();

    @Test
    void loadsEveryFieldOfAFullConfiguration() throws IOException {
        AgentConfig config = sut.load(write("full.yaml", FULL), Map.of());

        assertThat(config.project()).isEqualTo("webshop");
        assertThat(config.environment()).isEqualTo("PROD");
        assertThat(config.cluster()).isEqualTo("eu-1");
        assertThat(config.agentName()).isEqualTo("eu-1-agent");
        assertThat(config.controlPlane())
                .isEqualTo(new AgentConfig.ControlPlaneSettings("control-plane:9090", Duration.ofSeconds(7)));
        assertThat(config.otlp()).isEqualTo(new AgentConfig.OtlpSettings(4317));
        assertThat(config.snapshot()).isEqualTo(new AgentConfig.SnapshotSettings(Duration.ofSeconds(30), 128));
        assertThat(config.buffers()).isEqualTo(new AgentConfig.BufferSettings(1024, Duration.ofSeconds(45)));
        assertThat(config.metrics()).isEqualTo(new AgentConfig.MetricsSettings(9999));
        assertThat(config.attributeMapping().keys(MappedField.DOMAIN)).containsExactly("service.namespace", "team");
        assertThat(config.attributeMapping().keys(MappedField.SERVICE)).isEqualTo(MappedField.SERVICE.defaultKeys());
    }

    @Test
    void appliesDefaultsToAMinimalConfiguration() throws IOException {
        AgentConfig config = sut.load(write("minimal.yaml", MINIMAL), Map.of());

        assertThat(config.project()).isEqualTo(AgentConfig.DEFAULT_PROJECT);
        assertThat(config.controlPlane().retryDelay()).isEqualTo(AgentConfig.DEFAULT_RETRY_DELAY);
        assertThat(config.otlp().port()).isEqualTo(AgentConfig.DEFAULT_OTLP_PORT);
        assertThat(config.snapshot())
                .isEqualTo(new AgentConfig.SnapshotSettings(
                        AgentConfig.DEFAULT_SNAPSHOT_INTERVAL, AgentConfig.DEFAULT_QUEUE_SIZE));
        assertThat(config.buffers())
                .isEqualTo(
                        new AgentConfig.BufferSettings(AgentConfig.DEFAULT_RING_SIZE, AgentConfig.DEFAULT_PENDING_TTL));
        assertThat(config.metrics().port()).isEqualTo(AgentConfig.DEFAULT_METRICS_PORT);
        assertThat(config.attributeMapping()).isEqualTo(AttributeMapping.defaults());
    }

    @Test
    void reportsEveryMissingRequiredFieldAtOnce() throws IOException {
        Path path = write("empty.yaml", "");

        assertThatExceptionOfType(AgentConfigException.class)
                .isThrownBy(() -> sut.load(path, Map.of()))
                .extracting(AgentConfigException::problems, list())
                .containsExactly(
                        "Missing required config field: environment",
                        "Missing required config field: cluster",
                        "Missing required config field: agent.name",
                        "Missing required config field: control-plane.server");
    }

    @Test
    void reportsEveryInvalidValueAtOnce() throws IOException {
        Path path = write("invalid.yaml", """
                project: " "
                environment: DEV
                cluster: local
                agent:
                  name: dev-agent
                control-plane:
                  server: localhost
                  retry-seconds: 0
                otlp:
                  port: 70000
                snapshot:
                  interval-seconds: -1
                  queue-size: 0
                buffers:
                  ring-size: 1000
                  pending-ttl-seconds: 0
                metrics:
                  port: 0
                attribute-mapping:
                  domain: []
                  colour: [a]
                """);

        assertThatExceptionOfType(AgentConfigException.class)
                .isThrownBy(() -> sut.load(path, Map.of()))
                .extracting(AgentConfigException::problems, list())
                .containsExactly(
                        "Invalid config field: project must not be blank",
                        "Invalid config field: control-plane.server must be host:port",
                        "Invalid config field: control-plane.retry-seconds must be > 0",
                        "Invalid config field: otlp.port must be between 1 and 65535",
                        "Invalid config field: snapshot.interval-seconds must be > 0",
                        "Invalid config field: snapshot.queue-size must be > 0",
                        "Invalid config field: buffers.ring-size must be a power of two",
                        "Invalid config field: buffers.pending-ttl-seconds must be > 0",
                        "Invalid config field: metrics.port must be between 1 and 65535",
                        "Invalid config field: attribute-mapping.domain must list at least one attribute key",
                        "Unknown attribute-mapping field: colour (known fields: " + MappedField.configKeys() + ")");
    }

    @Test
    void rejectsUnknownFieldsWithTheirPath() throws IOException {
        Path path = write("unknown.yaml", MINIMAL + "control-plane:\n  server: a:1\n  bootstrap: {}\n");

        assertThatExceptionOfType(AgentConfigException.class)
                .isThrownBy(() -> sut.load(path, Map.of()))
                .withMessage("Unknown config field: control-plane.bootstrap");
    }

    @Test
    void rejectsValuesOfTheWrongType() throws IOException {
        Path path = write("wrong-type.yaml", MINIMAL + "otlp:\n  port: not-a-number\n");

        assertThatExceptionOfType(AgentConfigException.class)
                .isThrownBy(() -> sut.load(path, Map.of()))
                .withMessageStartingWith("Invalid config field: otlp.port (");
    }

    @Test
    void rejectsMissingFilesNonMappingDocumentsAndBrokenYaml() throws IOException {
        Path missing = tempDir.resolve("missing.yaml");
        Path sequence = write("sequence.yaml", "- environment: DEV\n");
        Path broken = write("broken.yaml", "environment: [DEV\n");

        assertThatExceptionOfType(AgentConfigException.class)
                .isThrownBy(() -> sut.load(missing, Map.of()))
                .withMessage("Config file does not exist: " + missing);
        assertThatExceptionOfType(AgentConfigException.class)
                .isThrownBy(() -> sut.load(sequence, Map.of()))
                .withMessage("Config must be a YAML mapping: " + sequence);
        assertThatExceptionOfType(AgentConfigException.class)
                .isThrownBy(() -> sut.load(broken, Map.of()))
                .withMessageStartingWith("Config file is not valid YAML: ");
    }

    @Test
    void overridesReplaceScalarsCreateSectionsAndParseLists() throws IOException {
        Path path = write("minimal.yaml", MINIMAL);
        Map<String, String> overrides = Map.of(
                "otlp.port", "4320",
                "metrics.port", "9000",
                "project", "webshop",
                "attribute-mapping.domain", "[team, service.namespace]");

        AgentConfig config = sut.load(path, overrides);

        assertThat(config.otlp().port()).isEqualTo(4320);
        assertThat(config.metrics().port()).isEqualTo(9000);
        assertThat(config.project()).isEqualTo("webshop");
        assertThat(config.attributeMapping().keys(MappedField.DOMAIN)).containsExactly("team", "service.namespace");
    }

    @Test
    void overridesAreValidatedLikeFileValues() throws IOException {
        Path path = write("minimal.yaml", MINIMAL);
        Map<String, String> zeroPort = Map.of("otlp.port", "0");
        Map<String, String> emptySegment = Map.of("otlp..port", "1");
        Map<String, String> brokenValue = Map.of("otlp.port", "[1");
        Map<String, String> blankName = Map.of("agent.name", "");

        assertThatExceptionOfType(AgentConfigException.class)
                .isThrownBy(() -> sut.load(path, zeroPort))
                .withMessage("Invalid config field: otlp.port must be between 1 and 65535");
        assertThatExceptionOfType(AgentConfigException.class)
                .isThrownBy(() -> sut.load(path, emptySegment))
                .withMessage("Invalid override key: 'otlp..port'");
        assertThatExceptionOfType(AgentConfigException.class)
                .isThrownBy(() -> sut.load(path, brokenValue))
                .withMessageStartingWith("Invalid override value for 'otlp.port': ");
        assertThatExceptionOfType(AgentConfigException.class)
                .isThrownBy(() -> sut.load(path, blankName))
                .withMessage("Missing required config field: agent.name");
    }

    @Test
    void renderedConfigurationLoadsBackToTheSameValues() throws IOException {
        AgentConfig config = sut.load(write("full.yaml", FULL), Map.of());

        String rendered = sut.render(config);
        AgentConfig reloaded = sut.load(write("rendered.yaml", rendered), Map.of());

        assertThat(rendered)
                .startsWith("project: webshop\n")
                .contains("attribute-mapping:\n")
                .doesNotContain("---");
        assertThat(reloaded).isEqualTo(config);
    }

    private Path write(String name, String content) throws IOException {
        return Files.writeString(tempDir.resolve(name), content);
    }

    private static org.assertj.core.api.InstanceOfAssertFactory<List, org.assertj.core.api.ListAssert<String>> list() {
        return org.assertj.core.api.InstanceOfAssertFactories.list(String.class);
    }
}

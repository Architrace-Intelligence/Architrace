/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.core.config;


import io.github.architrace.testsupport.TestDataProvider;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AgentConfigLoaderTest {

  @TempDir
  Path tempDir;

  @InjectMocks
  private AgentConfigLoader sut;

  @Test
  void loadShouldParseValidJsonConfig() throws Exception {
    Path configPath =
        TestDataProvider.copyResourceToTemp(
            tempDir,
            "testdata/config/valid-agent-config.json",
            "valid-agent-config.json");

    AgentConfig config = sut.load(configPath);

    assertThat(config.clusterId()).isEqualTo("cluster-1");
    assertThat(config.agent().name()).isEqualTo("demo-agent");
    assertThat(config.controlPlane().bootstrap().server()).isEqualTo("localhost:50051");
  }

  @Test
  void loadShouldFailWhenFileDoesNotExist() {
    Path missing = tempDir.resolve("missing.json");

    assertThatThrownBy(() -> sut.load(missing))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Config file does not exist: " + missing);
  }

  @Test
  void loadShouldFailWhenConfigIsNull() throws Exception {
    Path configPath =
        TestDataProvider.copyResourceToTemp(
            tempDir,
            "testdata/config/null-config.json",
            "null-config.json");

    assertThatThrownBy(() -> sut.load(configPath))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Config is empty.");
  }

  @Test
  void loadShouldFailWhenClusterIdIsBlank() throws Exception {
    assertMissingField("testdata/config/blank-cluster-id.json", "Missing required config field: clusterId");
  }

  @Test
  void loadShouldFailWhenAgentNameIsMissing() throws Exception {
    assertMissingField("testdata/config/missing-agent-name.json", "Missing required config field: agent.name");
  }

  @Test
  void loadShouldFailWhenControlPlaneServerIsMissing() throws Exception {
    assertMissingField(
        "testdata/config/missing-control-server.json",
        "Missing required config field: control-plane.bootstrap.server");
  }

  private void assertMissingField(String resourcePath, String expectedMessage) throws Exception {
    Path configPath = TestDataProvider.copyResourceToTemp(tempDir, resourcePath, Path.of(resourcePath).getFileName().toString());

    assertThatThrownBy(() -> sut.load(configPath))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }
}

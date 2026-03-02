/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class AgentConfigLoader {

  private final ObjectMapper objectMapper;

  @Inject
  public AgentConfigLoader() {
    this.objectMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
  }

  public AgentConfig load(Path configPath) {
    Objects.requireNonNull(configPath, "configPath is required");
    if (!Files.exists(configPath)) {
      throw new IllegalArgumentException("Config file does not exist: " + configPath);
    }

    try (InputStream inputStream = Files.newInputStream(configPath)) {
      AgentConfig config = objectMapper.readValue(inputStream, AgentConfig.class);
      validate(config);

      return config;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read config file: " + configPath, e);
    }
  }

  private static void validate(AgentConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Config is empty.");
    }

    if (isBlank(config.clusterId())) {
      throw new IllegalArgumentException("Missing required config field: clusterId");
    }

    if (config.agent() == null || isBlank(config.agent().name())) {
      throw new IllegalArgumentException("Missing required config field: agent.name");
    }

    if (config.controlPlane() == null
        || config.controlPlane().bootstrap() == null
        || isBlank(config.controlPlane().bootstrap().server())) {
      throw new IllegalArgumentException(
          "Missing required config field: control-plane.bootstrap.server");
    }

    if (config.otlpReceiverPort() != null && config.otlpReceiverPort() <= 0) {
      throw new IllegalArgumentException(
          "Invalid config field: otlp-receiver-port must be > 0");
    }

    if (config.controlPlaneRetrySeconds() != null
        && config.controlPlaneRetrySeconds() <= 0) {
      throw new IllegalArgumentException(
          "Invalid config field: control-plane-retry-seconds must be > 0");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

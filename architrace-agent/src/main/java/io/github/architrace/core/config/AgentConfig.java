/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentConfig(
    String clusterId,
    Agent agent,
    @JsonProperty("control-plane") ControlPlane controlPlane,
    @JsonProperty("otlp-receiver-port")
    @JsonAlias("oltp-receiver-port")
    Integer otlpReceiverPort,
    @JsonProperty("control-plane-retry-seconds") Long controlPlaneRetrySeconds) {

  public enum Environment {
    DEV,
    TEST,
    STG,
    PROD
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Agent(String name) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ControlPlane(@JsonProperty("bootstrap") Bootstrap bootstrap) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Bootstrap(String server) {
  }
}

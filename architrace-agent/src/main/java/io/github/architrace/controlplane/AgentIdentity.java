/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.controlplane;

import io.github.architrace.core.BuildVersion;
import io.github.architrace.core.config.AgentConfig;
import java.util.Objects;

public record AgentIdentity(
    String name, String version, String project, String environment, String cluster) {

  static final String DEFAULT_PROJECT = "default";

  public AgentIdentity {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(project, "project");
    Objects.requireNonNull(environment, "environment");
    Objects.requireNonNull(cluster, "cluster");
  }

  public static AgentIdentity from(AgentConfig config) {
    String project = config.project() == null || config.project().isBlank()
        ? DEFAULT_PROJECT
        : config.project();
    return new AgentIdentity(
        config.agent().name(),
        BuildVersion.current(),
        project,
        config.environment(),
        config.clusterId());
  }
}

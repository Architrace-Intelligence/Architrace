/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.architrace.core.config.AgentConfig;
import org.junit.jupiter.api.Test;

class AgentIdentityTest {

  @Test
  void fromConfigCarriesScopeAndDefaultsProject() {
    AgentIdentity identity = AgentIdentity.from(config(null));

    assertThat(identity.name()).isEqualTo("demo-agent");
    assertThat(identity.environment()).isEqualTo("DEV");
    assertThat(identity.cluster()).isEqualTo("cluster-1");
    assertThat(identity.project()).isEqualTo(AgentIdentity.DEFAULT_PROJECT);
    assertThat(identity.version()).isNotBlank();
  }

  @Test
  void fromConfigKeepsAnExplicitProject() {
    assertThat(AgentIdentity.from(config("webshop")).project()).isEqualTo("webshop");
    assertThat(AgentIdentity.from(config(" ")).project()).isEqualTo(AgentIdentity.DEFAULT_PROJECT);
  }

  @Test
  void rejectsMissingParts() {
    assertThatNullPointerException()
        .isThrownBy(() -> new AgentIdentity("a", "1", "p", null, "c"))
        .withMessage("environment");
  }

  private static AgentConfig config(String project) {
    return new AgentConfig(
        "cluster-1",
        new AgentConfig.Agent("demo-agent"),
        new AgentConfig.ControlPlane(new AgentConfig.Bootstrap("localhost:9090")),
        4319,
        5L,
        "DEV",
        project);
  }
}

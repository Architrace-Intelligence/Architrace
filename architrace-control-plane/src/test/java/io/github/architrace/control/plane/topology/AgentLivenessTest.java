/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AgentLivenessTest {

  private static final Instant T0 = Instant.parse("2026-10-01T12:00:00Z");
  private static final Agent AGENT =
      new Agent(new AgentId(1), "a", "0.1.0", new Scope("p", "DEV", "c"), T0, T0);

  @Test
  void liveUntilThreeHeartbeatsAreMissed() {
    AgentLiveness liveness = new AgentLiveness(Duration.ofSeconds(30));

    assertThat(liveness.isLive(AGENT, T0.plusSeconds(89))).isTrue();
    assertThat(liveness.isLive(AGENT, T0.plusSeconds(90))).isFalse();
  }

  @Test
  void intervalMustBePositive() {
    assertThatIllegalArgumentException().isThrownBy(() -> new AgentLiveness(Duration.ZERO));
  }
}

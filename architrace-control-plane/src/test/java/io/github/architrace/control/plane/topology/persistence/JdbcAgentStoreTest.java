/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentId;
import io.github.architrace.control.plane.topology.AgentRegistration;
import io.github.architrace.control.plane.topology.Scope;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JdbcAgentStoreTest extends JdbcStoreTest {

  private static final Scope SCOPE = new Scope("webshop", "PROD", "k8s-prod-eu1");
  private static final Instant FIRST = Instant.parse("2026-10-01T12:00:00Z");
  private static final Instant LATER = FIRST.plusSeconds(90);

  @Autowired JdbcAgentStore agents;

  @Test
  void registersANewAgentOnce() {
    Agent created = agents.register(new AgentRegistration("prod-eu1-a", "0.4.0", SCOPE), FIRST);
    Agent again = agents.register(new AgentRegistration("prod-eu1-a", "0.4.1", SCOPE), LATER);

    assertThat(again.id()).isEqualTo(created.id());
    assertThat(again.version()).isEqualTo("0.4.1");
    assertThat(again.firstSeenAt()).isEqualTo(FIRST);
    assertThat(again.lastSeenAt()).isEqualTo(LATER);
    assertThat(agents.all()).filteredOn(a -> a.name().equals("prod-eu1-a")).hasSize(1);
  }

  @Test
  void sameNameInAnotherScopeIsAnotherAgent() {
    Agent prod = agents.register(new AgentRegistration("collector", "0.4.0", SCOPE), FIRST);
    Agent stage =
        agents.register(
            new AgentRegistration("collector", "0.4.0", new Scope("webshop", "STAGE", "k8s-stage-eu1")),
            FIRST);

    assertThat(stage.id()).isNotEqualTo(prod.id());
    assertThat(stage.scope().environment()).isEqualTo("STAGE");
  }

  @Test
  void touchMovesLastSeenOnly() {
    Agent agent = agents.register(new AgentRegistration("prod-eu2", "0.4.0", SCOPE), FIRST);

    agents.touch(agent.id(), LATER);

    assertThat(agents.find(agent.id()))
        .get()
        .satisfies(
            found -> {
              assertThat(found.firstSeenAt()).isEqualTo(FIRST);
              assertThat(found.lastSeenAt()).isEqualTo(LATER);
            });
    assertThat(agents.find(new AgentId(Long.MAX_VALUE))).isEmpty();
  }
}

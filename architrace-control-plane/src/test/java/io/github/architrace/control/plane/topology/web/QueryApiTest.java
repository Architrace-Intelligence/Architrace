/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.web;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.service;
import static io.github.architrace.control.plane.topology.TestTopology.snapshot;
import static io.github.architrace.control.plane.topology.TestTopology.topic;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentLiveness;
import io.github.architrace.control.plane.topology.AgentRegistration;
import io.github.architrace.control.plane.topology.InMemoryAgentStore;
import io.github.architrace.control.plane.topology.InMemorySnapshotStore;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.TopologyMetrics;
import io.github.architrace.control.plane.topology.TopologyQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = {ScopesController.class, AgentsController.class})
@Import(QueryApiTest.Fixture.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class QueryApiTest {

  @TestConfiguration(proxyBeanMethods = false)
  static class Fixture {

    @Bean
    InMemoryAgentStore agentStore() {
      return new InMemoryAgentStore();
    }

    @Bean
    InMemorySnapshotStore snapshotStore() {
      return new InMemorySnapshotStore();
    }

    @Bean
    TopologyQuery topologyQuery(InMemoryAgentStore agents, InMemorySnapshotStore snapshots) {
      return new TopologyQuery(
          agents,
          snapshots,
          new AgentLiveness(Duration.ofSeconds(30)),
          new TopologyMetrics(new SimpleMeterRegistry()),
          Clock.fixed(NOW, ZoneOffset.UTC));
    }
  }

  @Autowired MockMvcTester mvc;
  @Autowired InMemoryAgentStore agents;
  @Autowired InMemorySnapshotStore snapshots;

  @Test
  void listsScopeSummariesAsJson() {
    Agent live = agents.register(new AgentRegistration("prod-eu1-a", "0.4.0", SCOPE), NOW);
    agents.register(
        new AgentRegistration("prod-eu1-b", "0.4.0", SCOPE), NOW.minus(Duration.ofMinutes(5)));
    snapshots.save(
        snapshot(
            live.id(),
            NOW.minusSeconds(60),
            List.of(service("orders", "2.8.1", "orders"), database("orders"), topic("order-events")),
            List.of()));

    assertThat(mvc.get().uri("/api/v1/scopes"))
        .hasStatusOk()
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            [
              {
                "scope": {"project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1"},
                "agents": 2,
                "liveAgents": 1,
                "services": 1,
                "dataStreams": 1,
                "namespaces": 1,
                "lastSnapshotAt": "2026-10-01T11:59:00Z"
              }
            ]
            """);
  }

  @Test
  void listsAgentsOrderedByScopeAndNameWithLiveness() {
    Scope dev = new Scope("webshop", "DEV", "k8s-dev");
    agents.register(new AgentRegistration("prod-eu1-b", "0.4.0", SCOPE), NOW.minusSeconds(100));
    agents.register(new AgentRegistration("prod-eu1-a", "0.4.1", SCOPE), NOW);
    agents.register(new AgentRegistration("dev-a", "dev", dev), NOW.minusSeconds(10));

    assertThat(mvc.get().uri("/api/v1/agents"))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            [
              {
                "id": 3, "name": "dev-a", "version": "dev",
                "scope": {"project": "webshop", "environment": "DEV", "cluster": "k8s-dev"},
                "firstSeenAt": "2026-10-01T11:59:50Z", "lastSeenAt": "2026-10-01T11:59:50Z",
                "live": true
              },
              {
                "id": 2, "name": "prod-eu1-a", "version": "0.4.1",
                "scope": {"project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1"},
                "firstSeenAt": "2026-10-01T12:00:00Z", "lastSeenAt": "2026-10-01T12:00:00Z",
                "live": true
              },
              {
                "id": 1, "name": "prod-eu1-b", "version": "0.4.0",
                "scope": {"project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1"},
                "firstSeenAt": "2026-10-01T11:58:20Z", "lastSeenAt": "2026-10-01T11:58:20Z",
                "live": false
              }
            ]
            """);
  }

  @Test
  void unknownPathsAnswerWithProblemDetails() {
    assertThat(mvc.get().uri("/api/v1/nothing"))
        .hasStatus(404)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo(404);
  }
}

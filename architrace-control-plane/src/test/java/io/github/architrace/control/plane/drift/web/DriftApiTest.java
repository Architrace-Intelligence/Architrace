/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.drift.web;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.service;
import static io.github.architrace.control.plane.topology.TestTopology.snapshot;
import static io.github.architrace.control.plane.topology.TestTopology.topic;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.control.plane.drift.DriftQuery;
import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentLiveness;
import io.github.architrace.control.plane.topology.AgentRegistration;
import io.github.architrace.control.plane.topology.Deployment;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.InMemoryAgentStore;
import io.github.architrace.control.plane.topology.InMemorySnapshotStore;
import io.github.architrace.control.plane.topology.NodeAttributes;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.PlatformHosts;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.TopologyMetrics;
import io.github.architrace.control.plane.topology.TopologyNode;
import io.github.architrace.control.plane.topology.TopologyQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(controllers = DriftController.class)
@Import(DriftApiTest.Fixture.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DriftApiTest {

    private static final String PROD = "/api/v1/scopes/webshop/PROD/k8s-prod-eu1";
    private static final Scope DEV = new Scope("webshop", "DEV", "k8s-dev");

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
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        DriftQuery driftQuery(InMemoryAgentStore agents, InMemorySnapshotStore snapshots, Clock clock) {
            return new DriftQuery(new TopologyQuery(
                    agents,
                    snapshots,
                    PlatformHosts.none(),
                    new AgentLiveness(Duration.ofSeconds(30)),
                    new TopologyMetrics(new SimpleMeterRegistry()),
                    clock));
        }
    }

    @Autowired
    MockMvcTester mvc;

    @Autowired
    InMemoryAgentStore agents;

    @Autowired
    InMemorySnapshotStore snapshots;

    @Test
    void servesTheEnvironmentDriftBetweenTwoScopesOfAProject() {
        Agent dev = register("dev-a", DEV);
        Agent prod = register("prod-eu1-a", SCOPE);
        TopologyNode devSearch = new TopologyNode(
                "service:search",
                NodeType.SERVICE,
                "search",
                new NodeAttributes(Set.of("2.3.0"), Set.of(new Deployment("k8s-dev", "search")), Map.of()));
        TopologyNode prodSearch = service("search", "2.2.1", "search");
        TopologyNode catalogDb = database("catalog");
        TopologyNode ordersDb = database("orders");
        TopologyNode reporting = service("reporting", "1.2.0", "reporting");
        snapshots.save(snapshot(
                dev.id(),
                DEV,
                NOW.minusSeconds(60),
                List.of(devSearch, catalogDb, ordersDb),
                List.of(edge(devSearch, catalogDb, EdgeKind.SYNC, 10))));
        snapshots.save(snapshot(
                prod.id(),
                NOW.minusSeconds(60),
                List.of(prodSearch, ordersDb, reporting),
                List.of(edge(reporting, ordersDb, EdgeKind.SYNC, 20))));

        assertThat(mvc.get()
                        .uri(PROD + "/diff/environments")
                        .queryParam("leftEnvironment", "DEV")
                        .queryParam("leftCluster", "k8s-dev"))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
            {
              "left": {"scope": {"project": "webshop", "environment": "DEV", "cluster": "k8s-dev"},
                       "at": "2026-10-01T12:00:00Z"},
              "right": {"scope": {"project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1"},
                        "at": "2026-10-01T12:00:00Z"},
              "nodesAdded": [
                {"id": "service:reporting", "type": "SERVICE", "name": "reporting", "versions": ["1.2.0"],
                 "deployments": [{"cluster": "k8s-prod-eu1", "namespace": "reporting"}], "labels": {}}
              ],
              "nodesRemoved": [
                {"id": "db:postgresql/catalog", "type": "DATABASE", "name": "catalog",
                 "versions": [], "deployments": [], "labels": {}}
              ],
              "nodesChanged": [
                {"id": "service:search", "type": "SERVICE", "name": "search",
                 "versionsBefore": ["2.3.0"], "versionsAfter": ["2.2.1"],
                 "deploymentsBefore": [{"cluster": "k8s-dev", "namespace": "search"}],
                 "deploymentsAfter": [{"cluster": "k8s-prod-eu1", "namespace": "search"}]}
              ],
              "edgesAdded": [
                {"sourceId": "service:reporting", "targetId": "db:postgresql/orders", "kind": "SYNC"}
              ],
              "edgesRemoved": [
                {"sourceId": "service:search", "targetId": "db:postgresql/catalog", "kind": "SYNC"}
              ]
            }
            """);
    }

    @Test
    void servesTheTimelineDriftOfAScopeAndDefaultsToToNow() {
        Agent prod = register("prod-eu1-a", SCOPE);
        TopologyNode before = service("orders", "2.8.0", "orders");
        TopologyNode after = service("orders", "2.8.1", "orders");
        TopologyNode events = topic("order-events");
        snapshots.save(snapshot(prod.id(), NOW.minusSeconds(3_600), List.of(before), List.of()));
        snapshots.save(
                snapshot(prod.id(), NOW, List.of(after, events), List.of(edge(after, events, EdgeKind.PUBLISH, 5))));

        MvcTestResult result = mvc.get()
                .uri(PROD + "/diff/timeline")
                .queryParam("from", "2026-10-01T11:00:00Z")
                .exchange();

        assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        assertThat(result).bodyJson().extractingPath("$.left.at").isEqualTo("2026-10-01T11:00:00Z");
        assertThat(result).bodyJson().extractingPath("$.right.at").isEqualTo("2026-10-01T12:00:00Z");
        assertThat(result).bodyJson().extractingPath("$.nodesAdded[0].id").isEqualTo("topic:kafka/order-events");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.nodesChanged[0].versionsBefore")
                .isEqualTo(List.of("2.8.0"));
        assertThat(result)
                .bodyJson()
                .extractingPath("$.nodesChanged[0].versionsAfter")
                .isEqualTo(List.of("2.8.1"));
        assertThat(result).bodyJson().extractingPath("$.edgesAdded[0].kind").isEqualTo("PUBLISH");
        assertThat(result).bodyJson().extractingPath("$.edgesRemoved").isEqualTo(List.of());
    }

    @Test
    void invalidAndUnknownSidesAnswerWithTypedProblems() {
        register("prod-eu1-a", SCOPE);

        assertThat(mvc.get()
                        .uri(PROD + "/diff/timeline")
                        .queryParam("from", "2026-10-01T12:00:00Z")
                        .queryParam("to", "2026-10-01T11:00:00Z"))
                .hasStatus(400)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
            {
              "type": "urn:architrace:problem:invalid-query",
              "title": "Invalid query",
              "status": 400,
              "detail": "from 2026-10-01T12:00:00Z must not be after to 2026-10-01T11:00:00Z",
              "instance": "/api/v1/scopes/webshop/PROD/k8s-prod-eu1/diff/timeline"
            }
            """);
        assertThat(mvc.get().uri(PROD + "/diff/timeline"))
                .hasStatus(400)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Bad Request");
        assertThat(mvc.get()
                        .uri(PROD + "/diff/environments")
                        .queryParam("leftEnvironment", "DEV")
                        .queryParam("leftCluster", "k8s-dev"))
                .hasStatus(404)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
            {
              "type": "urn:architrace:problem:scope-not-found",
              "title": "Scope not found",
              "status": 404,
              "detail": "no agent has registered for scope webshop/DEV/k8s-dev",
              "instance": "/api/v1/scopes/webshop/PROD/k8s-prod-eu1/diff/environments"
            }
            """);
    }

    private Agent register(String name, Scope scope) {
        return agents.register(new AgentRegistration(name, "0.4.0", scope), NOW);
    }
}

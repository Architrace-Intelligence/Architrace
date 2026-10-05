/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology.web;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.service;
import static io.github.architrace.control.plane.topology.TestTopology.snapshot;
import static io.github.architrace.control.plane.topology.TestTopology.topic;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.control.plane.rules.Finding;
import io.github.architrace.control.plane.rules.InMemoryFindingStore;
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
import java.time.Instant;
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

@WebMvcTest(controllers = {ScopesController.class, AgentsController.class, SnapshotsController.class})
@Import(QueryApiTest.Fixture.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class QueryApiTest {

    private static final String PROD = "/api/v1/scopes/webshop/PROD/k8s-prod-eu1";

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
        InMemoryFindingStore findingStore() {
            return new InMemoryFindingStore();
        }

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        TopologyQuery topologyQuery(InMemoryAgentStore agents, InMemorySnapshotStore snapshots, Clock clock) {
            return new TopologyQuery(
                    agents,
                    snapshots,
                    PlatformHosts.none(),
                    new AgentLiveness(Duration.ofSeconds(30)),
                    new TopologyMetrics(new SimpleMeterRegistry()),
                    clock);
        }
    }

    @Autowired
    MockMvcTester mvc;

    @Autowired
    InMemoryAgentStore agents;

    @Autowired
    InMemorySnapshotStore snapshots;

    @Autowired
    InMemoryFindingStore findings;

    @Test
    void listsScopeSummariesAsJson() {
        Agent live = register("prod-eu1-a", SCOPE, NOW);
        register("prod-eu1-b", SCOPE, NOW.minus(Duration.ofMinutes(5)));
        snapshots.save(snapshot(
                live.id(),
                NOW.minusSeconds(60),
                List.of(service("orders", "2.8.1", "orders"), database("orders"), topic("order-events")),
                List.of()));

        findings.replace(
                SCOPE,
                List.of(
                        finding("shared-database", Finding.Severity.HIGH, "db:postgresql/orders"),
                        finding("unknown-external", Finding.Severity.LOW, "ext:api.stripe.com"),
                        finding("unknown-external", Finding.Severity.LOW, "ext:api.github.com")));

        assertThat(mvc.get().uri("/api/v1/scopes"))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
            [
              {
                "scope": {"project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1"},
                "agents": 2,
                "liveAgents": 1,
                "services": 1,
                "dataStreams": 1,
                "namespaces": 1,
                "findings": {"high": 1, "medium": 0, "low": 2},
                "lastSnapshotAt": "2026-10-01T11:59:00Z"
              }
            ]
            """);
    }

    @Test
    void listsAgentsOrderedByScopeAndNameWithLiveness() {
        Scope dev = new Scope("webshop", "DEV", "k8s-dev");
        register("prod-eu1-b", SCOPE, NOW.minusSeconds(100));
        agents.register(new AgentRegistration("prod-eu1-a", "0.4.1", SCOPE), NOW);
        agents.register(new AgentRegistration("dev-a", "dev", dev), NOW.minusSeconds(10));

        assertThat(mvc.get().uri("/api/v1/agents")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
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
    void servesTheCurrentGraphOfAScopeAtAPointInTime() {
        Agent agent = register("prod-eu1-a", SCOPE, NOW);
        TopologyNode orders = new TopologyNode(
                "service:orders",
                NodeType.SERVICE,
                "orders",
                new NodeAttributes(
                        Set.of("2.8.1", "2.8.0"),
                        Set.of(new Deployment("k8s-prod-eu1", "orders")),
                        Map.of("team", "orders")));
        TopologyNode db = database("orders");
        snapshots.save(snapshot(
                agent.id(),
                NOW.minusSeconds(60),
                List.of(orders, db),
                List.of(edge(orders, db, EdgeKind.SYNC, 24000))));
        snapshots.save(snapshot(agent.id(), NOW, List.of(service("later", "1.0", "later")), List.of()));

        assertThat(mvc.get().uri(PROD + "/graph").queryParam("at", "2026-10-01T11:59:30Z"))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
            {
              "scope": {"project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1"},
              "at": "2026-10-01T11:59:30Z",
              "nodes": [
                {"id": "db:postgresql/orders", "type": "DATABASE", "name": "orders",
                 "versions": [], "deployments": [], "labels": {}},
                {"id": "service:orders", "type": "SERVICE", "name": "orders",
                 "versions": ["2.8.0", "2.8.1"],
                 "deployments": [{"cluster": "k8s-prod-eu1", "namespace": "orders"}],
                 "labels": {"team": "orders"}}
              ],
              "edges": [
                {"sourceId": "service:orders", "targetId": "db:postgresql/orders", "kind": "SYNC",
                 "metrics": {"calls": 24000, "errors": 0, "p50Millis": 5, "p95Millis": 10,
                             "p99Millis": 20, "maxMillis": 50}}
              ]
            }
            """);
    }

    @Test
    void graphDefaultsToNow() {
        Agent agent = register("prod-eu1-a", SCOPE, NOW);
        snapshots.save(snapshot(agent.id(), NOW, List.of(service("orders", "2.8.1", "orders")), List.of()));

        MvcTestResult result = mvc.get().uri(PROD + "/graph").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.at").isEqualTo("2026-10-01T12:00:00Z");
        assertThat(result).bodyJson().extractingPath("$.nodes[0].id").isEqualTo("service:orders");
    }

    @Test
    void servesServicesWithTheirDependencies() {
        Agent agent = register("prod-eu1-a", SCOPE, NOW);
        TopologyNode checkout = service("checkout", "4.1.0", "checkout");
        TopologyNode orders = service("orders", "2.8.1", "orders");
        TopologyNode db = database("orders");
        TopologyNode events = topic("order-events");
        snapshots.save(snapshot(
                agent.id(),
                NOW,
                List.of(checkout, orders, db, events),
                List.of(
                        edge(checkout, orders, EdgeKind.SYNC, 100),
                        edge(orders, db, EdgeKind.SYNC, 200),
                        edge(orders, events, EdgeKind.PUBLISH, 10),
                        edge(events, checkout, EdgeKind.CONSUME, 10))));

        MvcTestResult result = mvc.get().uri(PROD + "/services").exchange();

        assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$[0].node.id").isEqualTo("service:checkout");
        assertThat(result).bodyJson().extractingPath("$[0].inbound[0].node.id").isEqualTo("topic:kafka/order-events");
        assertThat(result)
                .bodyJson()
                .extractingPath("$[0].inbound[0].edge.kind")
                .isEqualTo("CONSUME");
        assertThat(result).bodyJson().extractingPath("$[0].outbound[0].node.id").isEqualTo("service:orders");
        assertThat(result).bodyJson().extractingPath("$[1].node.id").isEqualTo("service:orders");
        assertThat(result)
                .bodyJson()
                .extractingPath("$[1].inbound[0].edge.sourceId")
                .isEqualTo("service:checkout");
        assertThat(result)
                .bodyJson()
                .extractingPath("$[1].outbound[0].node.type")
                .isEqualTo("DATABASE");
        assertThat(result)
                .bodyJson()
                .extractingPath("$[1].outbound[1].edge.kind")
                .isEqualTo("PUBLISH");
        assertThat(result)
                .bodyJson()
                .extractingPath("$[1].outbound[1].edge.metrics.calls")
                .isEqualTo(10);
    }

    @Test
    void listsTheSnapshotHistoryOfAScopeNewestFirstInPages() {
        Agent agent = register("prod-eu1-a", SCOPE, NOW);
        snapshots.save(
                snapshot(agent.id(), NOW.minusSeconds(180), List.of(service("orders", "1.0", "orders")), List.of()));
        snapshots.save(snapshot(agent.id(), NOW.minusSeconds(120), List.of(), List.of()));
        snapshots.save(snapshot(agent.id(), NOW.minusSeconds(60), List.of(), List.of()));

        assertThat(mvc.get().uri(PROD + "/snapshots").queryParam("size", "2"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
            {
              "items": [
                {"id": 3, "agentId": 1,
                 "scope": {"project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1"},
                 "window": {"start": "2026-10-01T11:58:00Z", "end": "2026-10-01T11:59:00Z"},
                 "receivedAt": "2026-10-01T11:59:01Z", "nodeCount": 0, "edgeCount": 0},
                {"id": 2, "agentId": 1,
                 "scope": {"project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1"},
                 "window": {"start": "2026-10-01T11:57:00Z", "end": "2026-10-01T11:58:00Z"},
                 "receivedAt": "2026-10-01T11:58:01Z", "nodeCount": 0, "edgeCount": 0}
              ],
              "page": 0, "size": 2, "totalItems": 3, "totalPages": 2
            }
            """);

        MvcTestResult bounded = mvc.get()
                .uri(PROD + "/snapshots")
                .queryParam("from", "2026-10-01T11:56:30Z")
                .queryParam("to", "2026-10-01T11:57:30Z")
                .exchange();

        assertThat(bounded).hasStatusOk();
        assertThat(bounded).bodyJson().extractingPath("$.items[0].id").isEqualTo(1);
        assertThat(bounded).bodyJson().extractingPath("$.items[0].nodeCount").isEqualTo(1);
        assertThat(bounded).bodyJson().extractingPath("$.totalItems").isEqualTo(1);
        assertThat(bounded).bodyJson().extractingPath("$.size").isEqualTo(50);
    }

    @Test
    void servesOneSnapshotWithItsNodesAndEdges() {
        Agent agent = register("prod-eu1-a", SCOPE, NOW);
        TopologyNode orders = service("orders", "2.8.1", "orders");
        TopologyNode events = topic("order-events");
        snapshots.save(snapshot(
                agent.id(), NOW, List.of(orders, events), List.of(edge(orders, events, EdgeKind.PUBLISH, 8700))));

        assertThat(mvc.get().uri("/api/v1/snapshots/1"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
            {
              "id": 1, "agentId": 1,
              "scope": {"project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1"},
              "window": {"start": "2026-10-01T11:59:00Z", "end": "2026-10-01T12:00:00Z"},
              "receivedAt": "2026-10-01T12:00:01Z",
              "nodes": [
                {"id": "service:orders", "type": "SERVICE", "name": "orders", "versions": ["2.8.1"],
                 "deployments": [{"cluster": "k8s-prod-eu1", "namespace": "orders"}], "labels": {}},
                {"id": "topic:kafka/order-events", "type": "TOPIC", "name": "order-events",
                 "versions": [], "deployments": [], "labels": {}}
              ],
              "edges": [
                {"sourceId": "service:orders", "targetId": "topic:kafka/order-events",
                 "kind": "PUBLISH",
                 "metrics": {"calls": 8700, "errors": 0, "p50Millis": 5, "p95Millis": 10,
                             "p99Millis": 20, "maxMillis": 50}}
              ]
            }
            """);
    }

    @Test
    void unknownScopeAndSnapshotAnswerWithTypedProblems() {
        assertThat(mvc.get().uri("/api/v1/scopes/webshop/PROD/k8s-prod-eu2/graph"))
                .hasStatus(404)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
            {
              "type": "urn:architrace:problem:scope-not-found",
              "title": "Scope not found",
              "status": 404,
              "detail": "no agent has registered for scope webshop/PROD/k8s-prod-eu2",
              "instance": "/api/v1/scopes/webshop/PROD/k8s-prod-eu2/graph"
            }
            """);
        assertThat(mvc.get().uri("/api/v1/snapshots/7"))
                .hasStatus(404)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("urn:architrace:problem:snapshot-not-found");
    }

    @Test
    void invalidQueriesAnswerWithBadRequest() {
        register("prod-eu1-a", SCOPE, NOW);

        assertThat(mvc.get().uri(PROD + "/snapshots").queryParam("size", "0"))
                .hasStatus(400)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
            {
              "type": "urn:architrace:problem:invalid-query",
              "title": "Invalid query",
              "status": 400,
              "detail": "size must be between 1 and 200: 0",
              "instance": "/api/v1/scopes/webshop/PROD/k8s-prod-eu1/snapshots"
            }
            """);
        assertThat(mvc.get()
                        .uri(PROD + "/snapshots")
                        .queryParam("from", "2026-10-01T12:00:00Z")
                        .queryParam("to", "2026-10-01T11:00:00Z"))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("from 2026-10-01T12:00:00Z must not be after to 2026-10-01T11:00:00Z");
        assertThat(mvc.get().uri(PROD + "/graph").queryParam("at", "yesterday"))
                .hasStatus(400)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Bad Request");
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

    private Agent register(String name, Scope scope, Instant lastSeen) {
        return agents.register(new AgentRegistration(name, "0.4.0", scope), lastSeen);
    }

    private static Finding finding(String ruleId, Finding.Severity severity, String subject) {
        return new Finding(
                ruleId, severity, SCOPE, List.of(subject), ruleId + " on " + subject, "detail", List.of(), NOW);
    }
}

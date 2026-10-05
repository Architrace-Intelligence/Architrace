/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules.web;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.service;
import static io.github.architrace.control.plane.topology.TestTopology.snapshot;
import static io.github.architrace.control.plane.topology.TestTopology.topic;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.control.plane.rules.Finding;
import io.github.architrace.control.plane.rules.FindingsQuery;
import io.github.architrace.control.plane.rules.InMemoryFindingStore;
import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentLiveness;
import io.github.architrace.control.plane.topology.AgentRegistration;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.InMemoryAgentStore;
import io.github.architrace.control.plane.topology.InMemorySnapshotStore;
import io.github.architrace.control.plane.topology.PlatformHosts;
import io.github.architrace.control.plane.topology.TopologyMetrics;
import io.github.architrace.control.plane.topology.TopologyNode;
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

@WebMvcTest(controllers = FindingsController.class)
@Import(FindingsApiTest.Fixture.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class FindingsApiTest {

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
        FindingsQuery findingsQuery(
                InMemoryAgentStore agents,
                InMemorySnapshotStore snapshots,
                InMemoryFindingStore findings,
                Clock clock) {
            TopologyQuery topology = new TopologyQuery(
                    agents,
                    snapshots,
                    PlatformHosts.none(),
                    new AgentLiveness(Duration.ofSeconds(30)),
                    new TopologyMetrics(new SimpleMeterRegistry()),
                    clock);
            return new FindingsQuery(topology, findings);
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
    void servesTheStoredFindingsOfAScopeHighSeverityFirstAndFiltered() {
        agents.register(new AgentRegistration("prod-eu1-a", "0.4.0", SCOPE), NOW);
        Finding external = new Finding(
                "unknown-external",
                Finding.Severity.LOW,
                SCOPE,
                List.of("ext:api.stripe.com"),
                "Unknown external system api.stripe.com",
                "checkout call api.stripe.com, which is not on the allowlist of known external systems",
                List.of("service:checkout"),
                NOW);
        Finding cycle = new Finding(
                "cyclic-dependency",
                Finding.Severity.HIGH,
                SCOPE,
                List.of("service:orders", "service:payments"),
                "Cyclic dependency between 2 services",
                "orders -> payments -> orders over synchronous calls",
                List.of("service:orders", "service:payments"),
                NOW);
        findings.replace(SCOPE, List.of(external, cycle));

        assertThat(mvc.get().uri(PROD + "/findings"))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
            [
              {"ruleId": "cyclic-dependency", "severity": "HIGH",
               "scope": {"project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1"},
               "subjectNodeIds": ["service:orders", "service:payments"],
               "title": "Cyclic dependency between 2 services",
               "detail": "orders -> payments -> orders over synchronous calls",
               "evidence": ["service:orders", "service:payments"],
               "evaluatedAt": "2026-10-01T12:00:00Z"},
              {"ruleId": "unknown-external", "severity": "LOW",
               "scope": {"project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1"},
               "subjectNodeIds": ["ext:api.stripe.com"],
               "title": "Unknown external system api.stripe.com",
               "detail": "checkout call api.stripe.com, which is not on the allowlist of known external systems",
               "evidence": ["service:checkout"],
               "evaluatedAt": "2026-10-01T12:00:00Z"}
            ]
            """);
        assertThat(mvc.get().uri(PROD + "/findings").queryParam("severity", "LOW"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[*].ruleId")
                .isEqualTo(List.of("unknown-external"));
        assertThat(mvc.get().uri(PROD + "/findings").queryParam("rule", "cyclic-dependency"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[*].ruleId")
                .isEqualTo(List.of("cyclic-dependency"));
        assertThat(mvc.get().uri(PROD + "/findings").queryParam("severity", "MEDIUM"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("[]");
    }

    @Test
    void servesTheImpactOfANodeOnTheCurrentGraph() {
        Agent agent = agents.register(new AgentRegistration("prod-eu1-a", "0.4.0", SCOPE), NOW);
        TopologyNode orders = service("orders", "2.8.1", "orders");
        TopologyNode reporting = service("reporting", "1.2.0", "reporting");
        TopologyNode ordersDb = database("orders");
        TopologyNode events = topic("order-events");
        snapshots.save(snapshot(
                agent.id(),
                NOW.minusSeconds(60),
                List.of(orders, reporting, ordersDb, events),
                List.of(
                        edge(orders, ordersDb, EdgeKind.SYNC, 10),
                        edge(orders, events, EdgeKind.PUBLISH, 5),
                        edge(events, reporting, EdgeKind.CONSUME, 5))));

        assertThat(mvc.get().uri(PROD + "/impact").queryParam("node", "db:postgresql/orders"))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
            {
              "subject": {"id": "db:postgresql/orders", "type": "DATABASE", "name": "orders",
                          "versions": [], "deployments": [], "labels": {}},
              "at": "2026-10-01T12:00:00Z",
              "impaired": [
                {"node": {"id": "service:orders", "type": "SERVICE", "name": "orders", "versions": ["2.8.1"],
                          "deployments": [{"cluster": "k8s-prod-eu1", "namespace": "orders"}], "labels": {}},
                 "distance": 1, "path": ["service:orders", "db:postgresql/orders"]}
              ],
              "delayed": [
                {"node": {"id": "service:reporting", "type": "SERVICE", "name": "reporting", "versions": ["1.2.0"],
                          "deployments": [{"cluster": "k8s-prod-eu1", "namespace": "reporting"}], "labels": {}},
                 "distance": 3,
                 "path": ["service:reporting", "topic:kafka/order-events", "service:orders", "db:postgresql/orders"]}
              ],
              "services": 1,
              "servicesTotal": 2
            }
            """);
    }

    @Test
    void unknownScopeAndUnknownNodeAnswerWithTypedProblems() {
        agents.register(new AgentRegistration("prod-eu1-a", "0.4.0", SCOPE), NOW);

        assertThat(mvc.get().uri("/api/v1/scopes/webshop/DEV/k8s-dev/findings"))
                .hasStatus(404)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("urn:architrace:problem:scope-not-found");
        assertThat(mvc.get().uri(PROD + "/impact").queryParam("node", "service:nothing"))
                .hasStatus(404)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
            {
              "type": "urn:architrace:problem:node-not-found",
              "title": "Node not found",
              "status": 404,
              "detail": "no node service:nothing in the graph of scope webshop/PROD/k8s-prod-eu1",
              "instance": "/api/v1/scopes/webshop/PROD/k8s-prod-eu1/impact"
            }
            """);
        assertThat(mvc.get().uri(PROD + "/impact"))
                .hasStatus(400)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Bad Request");
    }
}

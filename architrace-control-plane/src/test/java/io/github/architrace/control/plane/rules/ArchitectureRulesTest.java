/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.topic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.NodeAttributes;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.TestTopology;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

    private final TopologyNode checkout = service("checkout");
    private final TopologyNode orders = service("orders");
    private final TopologyNode payments = service("payments");
    private final TopologyNode inventory = service("inventory");
    private final TopologyNode billing = service("billing");
    private final TopologyNode reporting = service("reporting");
    private final TopologyNode ordersDb = database("orders");
    private final TopologyNode events = topic("order-events");
    private final TopologyNode stripe = external("api.stripe.com");
    private final TopologyNode github = external("api.github.com");

    private static TopologyNode service(String name) {
        return TestTopology.service(name, "1.0.0", name);
    }

    private static TopologyNode external(String address) {
        return new TopologyNode("ext:" + address, NodeType.EXTERNAL, address, NodeAttributes.none());
    }

    private static TopologyEdge sync(TopologyNode source, TopologyNode target) {
        return edge(source, target, EdgeKind.SYNC, 10);
    }

    private static TopologyGraph graph(List<TopologyNode> nodes, List<TopologyEdge> edges) {
        return new TopologyGraph(SCOPE, NOW, nodes, edges);
    }

    @Nested
    class CyclicDependencyRule {

        private final CyclicDependency rule = new CyclicDependency();

        @Test
        void reportsEveryStronglyConnectedComponentWithItsShortestCycleAsEvidence() {
            TopologyNode auth = service("auth");
            TopologyNode users = service("users");
            TopologyGraph graph = graph(
                    List.of(checkout, orders, payments, inventory, auth, users),
                    List.of(
                            sync(checkout, orders),
                            sync(orders, payments),
                            sync(payments, orders),
                            sync(orders, inventory),
                            sync(inventory, payments),
                            sync(auth, users),
                            sync(users, auth)));

            List<Finding> findings = rule.evaluate(graph);

            assertThat(findings).hasSize(2);
            Finding authCycle = findings.getFirst();
            assertThat(authCycle.ruleId()).isEqualTo("cyclic-dependency");
            assertThat(authCycle.severity()).isEqualTo(Finding.Severity.HIGH);
            assertThat(authCycle.scope()).isEqualTo(SCOPE);
            assertThat(authCycle.evaluatedAt()).isEqualTo(NOW);
            assertThat(authCycle.subjectNodeIds()).containsExactly("service:auth", "service:users");
            assertThat(authCycle.evidence()).containsExactly("service:auth", "service:users");
            assertThat(authCycle.title()).isEqualTo("Cyclic dependency between 2 services");
            assertThat(authCycle.detail()).startsWith("auth -> users -> auth over synchronous calls");
            Finding orderCycle = findings.getLast();
            assertThat(orderCycle.subjectNodeIds())
                    .containsExactly("service:inventory", "service:orders", "service:payments");
            assertThat(orderCycle.evidence())
                    .containsExactly("service:inventory", "service:payments", "service:orders");
            assertThat(orderCycle.detail()).startsWith("inventory -> payments -> orders -> inventory");
        }

        @Test
        void ignoresSelfCallsAndCyclesThatCloseThroughATopic() {
            TopologyGraph graph = graph(
                    List.of(checkout, orders, ordersDb, events, reporting),
                    List.of(
                            sync(checkout, orders),
                            sync(orders, orders),
                            sync(orders, ordersDb),
                            edge(orders, events, EdgeKind.PUBLISH, 5),
                            edge(events, reporting, EdgeKind.CONSUME, 5),
                            sync(reporting, orders)));

            assertThat(rule.evaluate(graph)).isEmpty();
        }
    }

    @Nested
    class SharedDatabaseRule {

        @Test
        void reportsADatabaseUsedByAtLeastTheConfiguredNumberOfServices() {
            TopologyNode checkoutDb = database("checkout");
            TopologyGraph graph = graph(
                    List.of(orders, billing, reporting, checkout, ordersDb, checkoutDb),
                    List.of(
                            sync(reporting, ordersDb),
                            sync(orders, ordersDb),
                            sync(billing, ordersDb),
                            sync(checkout, checkoutDb),
                            sync(checkout, orders)));

            List<Finding> findings = new SharedDatabase(2).evaluate(graph);

            assertThat(findings).hasSize(1);
            Finding finding = findings.getFirst();
            assertThat(finding.ruleId()).isEqualTo("shared-database");
            assertThat(finding.severity()).isEqualTo(Finding.Severity.HIGH);
            assertThat(finding.subjectNodeIds()).containsExactly("db:postgresql/orders");
            assertThat(finding.evidence()).containsExactly("service:billing", "service:orders", "service:reporting");
            assertThat(finding.title()).isEqualTo("Database orders is shared by 3 services");
            assertThat(finding.detail()).startsWith("billing, orders, reporting use the database orders directly");
        }

        @Test
        void countsOnlyServicesBehindSynchronousEdgesAgainstTheThreshold() {
            TopologyGraph graph = graph(
                    List.of(orders, billing, reporting, ordersDb, events, stripe),
                    List.of(
                            sync(orders, ordersDb),
                            sync(billing, ordersDb),
                            edge(reporting, ordersDb, EdgeKind.PUBLISH, 1),
                            edge(orders, events, EdgeKind.PUBLISH, 1),
                            edge(billing, events, EdgeKind.PUBLISH, 1),
                            sync(orders, stripe),
                            sync(billing, stripe)));

            assertThat(new SharedDatabase(3).evaluate(graph)).isEmpty();
            assertThat(new SharedDatabase(2).evaluate(graph))
                    .extracting(Finding::subjectNodeIds)
                    .containsExactly(List.of("db:postgresql/orders"));
        }

        @Test
        void rejectsAThresholdBelowTwo() {
            assertThatThrownBy(() -> new SharedDatabase(1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("minServices must be at least 2");
        }
    }

    @Nested
    class UnknownExternalRule {

        @Test
        void reportsExternalsOutsideTheAllowlistWithTheirCallers() {
            TopologyGraph graph = graph(
                    List.of(checkout, payments, orders, stripe, github),
                    List.of(sync(payments, stripe), sync(checkout, stripe), sync(orders, github)));

            List<Finding> findings = new UnknownExternal(Set.of("api.github.com")).evaluate(graph);

            assertThat(findings).hasSize(1);
            Finding finding = findings.getFirst();
            assertThat(finding.ruleId()).isEqualTo("unknown-external");
            assertThat(finding.severity()).isEqualTo(Finding.Severity.LOW);
            assertThat(finding.subjectNodeIds()).containsExactly("ext:api.stripe.com");
            assertThat(finding.evidence()).containsExactly("service:checkout", "service:payments");
            assertThat(finding.title()).isEqualTo("Unknown external system api.stripe.com");
            assertThat(finding.detail())
                    .isEqualTo("checkout, payments call api.stripe.com, which is not on the allowlist of known external"
                            + " systems");
        }

        @Test
        void reportsEveryExternalInIdOrderWhenTheAllowlistIsEmpty() {
            TopologyGraph graph = graph(
                    List.of(orders, stripe, github, ordersDb), List.of(sync(orders, stripe), sync(orders, ordersDb)));

            List<Finding> findings = new UnknownExternal(Set.of()).evaluate(graph);

            assertThat(findings)
                    .extracting(Finding::subjectNodeIds)
                    .containsExactly(List.of("ext:api.github.com"), List.of("ext:api.stripe.com"));
            assertThat(findings.getFirst().evidence()).isEmpty();
            assertThat(findings.getFirst().detail())
                    .isEqualTo("api.github.com is not on the allowlist of known external systems");
        }
    }
}

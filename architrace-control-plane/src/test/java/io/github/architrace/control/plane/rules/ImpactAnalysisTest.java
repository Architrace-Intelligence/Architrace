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
import static org.assertj.core.groups.Tuple.tuple;

import io.github.architrace.control.plane.rules.Impact.ImpactedNode;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.NodeAttributes;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.TestTopology;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImpactAnalysisTest {

    private final TopologyNode gateway = service("gateway");
    private final TopologyNode orders = service("orders");
    private final TopologyNode payments = service("payments");
    private final TopologyNode billing = service("billing");
    private final TopologyNode reporting = service("reporting");
    private final TopologyNode notifier = service("notifier");
    private final TopologyNode ordersDb = database("orders");
    private final TopologyNode events = topic("order-events");
    private final TopologyNode invoices = topic("invoices");
    private final TopologyNode stripe =
            new TopologyNode("ext:api.stripe.com", NodeType.EXTERNAL, "api.stripe.com", NodeAttributes.none());

    private static TopologyNode service(String name) {
        return TestTopology.service(name, "1.0.0", name);
    }

    private static TopologyEdge sync(TopologyNode source, TopologyNode target) {
        return edge(source, target, EdgeKind.SYNC, 10);
    }

    private static TopologyEdge publish(TopologyNode source, TopologyNode target) {
        return edge(source, target, EdgeKind.PUBLISH, 10);
    }

    private static TopologyEdge consume(TopologyNode source, TopologyNode target) {
        return edge(source, target, EdgeKind.CONSUME, 10);
    }

    private static TopologyGraph graph(List<TopologyNode> nodes, List<TopologyEdge> edges) {
        return new TopologyGraph(SCOPE, NOW, nodes, edges);
    }

    @Test
    void chainImpairsEveryCallerWithItsDistanceAndPath() {
        TopologyGraph graph = graph(
                List.of(gateway, orders, payments, stripe),
                List.of(sync(gateway, orders), sync(orders, payments), sync(payments, stripe)));

        Impact impact = ImpactAnalysis.of(graph, stripe.id()).orElseThrow();

        assertThat(impact.subject()).isEqualTo(stripe);
        assertThat(impact.at()).isEqualTo(NOW);
        assertThat(impact.impaired())
                .extracting(ImpactedNode::node, ImpactedNode::distance, ImpactedNode::path)
                .containsExactly(
                        tuple(payments, 1, List.of("service:payments", "ext:api.stripe.com")),
                        tuple(orders, 2, List.of("service:orders", "service:payments", "ext:api.stripe.com")),
                        tuple(
                                gateway,
                                3,
                                List.of(
                                        "service:gateway",
                                        "service:orders",
                                        "service:payments",
                                        "ext:api.stripe.com")));
        assertThat(impact.delayed()).isEmpty();
        assertThat(impact.services()).isEqualTo(3);
        assertThat(impact.servicesTotal()).isEqualTo(3);
    }

    @Test
    void diamondKeepsTheShortestPathThroughTheSmallestId() {
        TopologyGraph graph = graph(
                List.of(gateway, orders, billing, payments),
                List.of(
                        sync(gateway, orders),
                        sync(gateway, billing),
                        sync(orders, payments),
                        sync(billing, payments),
                        sync(payments, orders)));

        Impact impact = ImpactAnalysis.of(graph, payments.id()).orElseThrow();

        assertThat(impact.impaired())
                .extracting(ImpactedNode::node, ImpactedNode::distance, ImpactedNode::path)
                .containsExactly(
                        tuple(billing, 1, List.of("service:billing", "service:payments")),
                        tuple(orders, 1, List.of("service:orders", "service:payments")),
                        tuple(gateway, 2, List.of("service:gateway", "service:billing", "service:payments")));
        assertThat(impact.services()).isEqualTo(3);
        assertThat(impact.servicesTotal()).isEqualTo(4);
    }

    @Test
    void databaseImpairsItsClientsAndDelaysTheConsumersOfTheirTopics() {
        TopologyGraph graph = graph(
                List.of(orders, billing, reporting, notifier, ordersDb, events, invoices),
                List.of(
                        sync(orders, ordersDb),
                        sync(billing, ordersDb),
                        publish(orders, events),
                        consume(events, reporting),
                        publish(billing, invoices),
                        consume(invoices, notifier),
                        consume(invoices, reporting)));

        Impact impact = ImpactAnalysis.of(graph, ordersDb.id()).orElseThrow();

        assertThat(impact.impaired()).extracting(ImpactedNode::node).containsExactly(billing, orders);
        assertThat(impact.delayed())
                .extracting(ImpactedNode::node, ImpactedNode::distance, ImpactedNode::path)
                .containsExactly(
                        tuple(
                                notifier,
                                3,
                                List.of(
                                        "service:notifier",
                                        "topic:kafka/invoices",
                                        "service:billing",
                                        "db:postgresql/orders")),
                        tuple(
                                reporting,
                                3,
                                List.of(
                                        "service:reporting",
                                        "topic:kafka/invoices",
                                        "service:billing",
                                        "db:postgresql/orders")));
        assertThat(impact.services()).isEqualTo(2);
        assertThat(impact.servicesTotal()).isEqualTo(4);
    }

    @Test
    void topicImpairsItsProducersWithTheirCallersAndDelaysItsConsumers() {
        TopologyGraph graph = graph(
                List.of(gateway, orders, reporting, notifier, events, invoices),
                List.of(
                        sync(gateway, orders),
                        publish(orders, events),
                        publish(orders, invoices),
                        consume(events, reporting),
                        consume(invoices, notifier)));

        Impact impact = ImpactAnalysis.of(graph, events.id()).orElseThrow();

        assertThat(impact.impaired())
                .extracting(ImpactedNode::node, ImpactedNode::distance, ImpactedNode::path)
                .containsExactly(
                        tuple(orders, 1, List.of("service:orders", "topic:kafka/order-events")),
                        tuple(gateway, 2, List.of("service:gateway", "service:orders", "topic:kafka/order-events")));
        assertThat(impact.delayed())
                .extracting(ImpactedNode::node, ImpactedNode::distance, ImpactedNode::path)
                .containsExactly(
                        tuple(reporting, 1, List.of("service:reporting", "topic:kafka/order-events")),
                        tuple(
                                notifier,
                                3,
                                List.of(
                                        "service:notifier",
                                        "topic:kafka/invoices",
                                        "service:orders",
                                        "topic:kafka/order-events")));
        assertThat(impact.services()).isEqualTo(2);
    }

    @Test
    void serviceDelaysTheConsumersOfItsOwnTopicsButNeverAnImpairedOne() {
        TopologyGraph graph = graph(
                List.of(orders, reporting, notifier, events),
                List.of(
                        publish(orders, events),
                        consume(events, reporting),
                        consume(events, notifier),
                        sync(notifier, orders)));

        Impact impact = ImpactAnalysis.of(graph, orders.id()).orElseThrow();

        assertThat(impact.impaired()).extracting(ImpactedNode::node).containsExactly(notifier);
        assertThat(impact.delayed())
                .extracting(ImpactedNode::node, ImpactedNode::distance, ImpactedNode::path)
                .containsExactly(tuple(
                        reporting, 2, List.of("service:reporting", "topic:kafka/order-events", "service:orders")));
    }

    @Test
    void nodeNobodyDependsOnHasAnEmptyImpactAndIsNeverInItsOwnLists() {
        TopologyGraph graph = graph(
                List.of(gateway, orders, reporting),
                List.of(sync(gateway, orders), sync(orders, gateway), sync(reporting, orders)));

        Impact nobody = ImpactAnalysis.of(graph, reporting.id()).orElseThrow();
        Impact cycle = ImpactAnalysis.of(graph, orders.id()).orElseThrow();

        assertThat(nobody.impaired()).isEmpty();
        assertThat(nobody.delayed()).isEmpty();
        assertThat(nobody.services()).isZero();
        assertThat(nobody.servicesTotal()).isEqualTo(3);
        assertThat(cycle.impaired()).extracting(ImpactedNode::node).containsExactly(gateway, reporting);
    }

    @Test
    void anUnknownNodeHasNoImpact() {
        TopologyGraph graph = graph(List.of(orders), List.of());

        assertThat(ImpactAnalysis.of(graph, "service:missing")).isEmpty();
    }

    @Test
    void impactedNodeRequiresAPathFromTheNodeToTheSubject() {
        List<String> tooShort = List.of("service:orders");
        List<String> otherNode = List.of("service:gateway", "service:orders");
        List<ImpactedNode> none = List.of();

        assertThatThrownBy(() -> new ImpactedNode(orders, 1, tooShort)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ImpactedNode(orders, 1, otherNode)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Impact(orders, NOW, none, none, 2, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

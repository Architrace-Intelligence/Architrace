/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.drift;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.service;
import static io.github.architrace.control.plane.topology.TestTopology.topic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.architrace.control.plane.topology.Deployment;
import io.github.architrace.control.plane.topology.EdgeKey;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.EdgeMetrics;
import io.github.architrace.control.plane.topology.NodeAttributes;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GraphDifferTest {

    private static final Scope DEV = new Scope("webshop", "DEV", "k8s-dev");
    private static final Instant YESTERDAY = NOW.minusSeconds(86_400);

    private final TopologyNode orders = service("orders", "2.8.1", "orders");
    private final TopologyNode checkout = service("checkout", "4.1.0", "checkout");
    private final TopologyNode ordersDb = database("orders");
    private final TopologyNode events = topic("order-events");

    @Test
    void graphComparedWithItselfHasNoDrift() {
        TopologyGraph graph = graph(
                SCOPE,
                NOW,
                List.of(orders, checkout, ordersDb, events),
                List.of(
                        edge(checkout, orders, EdgeKind.SYNC, 100),
                        edge(orders, ordersDb, EdgeKind.SYNC, 200),
                        edge(orders, events, EdgeKind.PUBLISH, 10)));

        TopologyDiff diff = GraphDiffer.diff(graph, graph, DiffMode.TIMELINE);

        assertThat(diff.isEmpty()).isTrue();
        assertThat(diff.left()).isEqualTo(new GraphRef(SCOPE, NOW));
        assertThat(diff.right()).isEqualTo(new GraphRef(SCOPE, NOW));
    }

    @Test
    void listsNodesAndEdgesOnlyOnOneSideInOrder() {
        TopologyNode search = service("search", "2.3.0", "search");
        TopologyNode catalogDb = database("catalog");
        TopologyNode reporting = service("reporting", "1.2.0", "reporting");
        TopologyGraph left = graph(
                DEV,
                NOW,
                List.of(search, orders, catalogDb, ordersDb),
                List.of(
                        edge(search, catalogDb, EdgeKind.SYNC, 10),
                        edge(orders, ordersDb, EdgeKind.SYNC, 10),
                        edge(search, ordersDb, EdgeKind.SYNC, 10)));
        TopologyGraph right = graph(
                SCOPE,
                NOW,
                List.of(reporting, orders, ordersDb, events),
                List.of(
                        edge(orders, ordersDb, EdgeKind.SYNC, 900),
                        edge(reporting, ordersDb, EdgeKind.SYNC, 20),
                        edge(orders, events, EdgeKind.PUBLISH, 5)));

        TopologyDiff diff = GraphDiffer.diff(left, right, DiffMode.ENVIRONMENTS);

        assertThat(diff.left()).isEqualTo(new GraphRef(DEV, NOW));
        assertThat(diff.right()).isEqualTo(new GraphRef(SCOPE, NOW));
        assertThat(diff.nodesAdded()).containsExactly(reporting, events);
        assertThat(diff.nodesRemoved()).containsExactly(catalogDb, search);
        assertThat(diff.nodesChanged()).isEmpty();
        assertThat(diff.edgesAdded())
                .containsExactly(
                        new EdgeKey("service:orders", "topic:kafka/order-events", EdgeKind.PUBLISH),
                        new EdgeKey("service:reporting", "db:postgresql/orders", EdgeKind.SYNC));
        assertThat(diff.edgesRemoved())
                .containsExactly(
                        new EdgeKey("service:search", "db:postgresql/catalog", EdgeKind.SYNC),
                        new EdgeKey("service:search", "db:postgresql/orders", EdgeKind.SYNC));
    }

    @Test
    void versionChangeIsAChangedNodeNotARemovedPlusAnAddedOne() {
        TopologyNode before = service("orders", "2.8.0", "orders");
        TopologyGraph left = graph(SCOPE, YESTERDAY, List.of(before), List.of());
        TopologyGraph right = graph(SCOPE, NOW, List.of(orders), List.of());

        TopologyDiff diff = GraphDiffer.diff(left, right, DiffMode.TIMELINE);

        assertThat(diff.nodesAdded()).isEmpty();
        assertThat(diff.nodesRemoved()).isEmpty();
        assertThat(diff.nodesChanged()).containsExactly(new NodeChange(before, orders));
    }

    @Test
    void environmentModeComparesVersionsOnlyWhileTimelineModeComparesDeploymentsToo() {
        TopologyNode devOrders = new TopologyNode(
                "service:orders",
                NodeType.SERVICE,
                "orders",
                new NodeAttributes(Set.of("2.8.1"), Set.of(new Deployment("k8s-dev", "orders")), Map.of()));
        TopologyNode movedOrders = new TopologyNode(
                "service:orders",
                NodeType.SERVICE,
                "orders",
                new NodeAttributes(
                        Set.of("2.8.1"), Set.of(new Deployment("k8s-prod-eu1", "orders-v2")), Map.of("team", "x")));
        TopologyGraph left = graph(DEV, NOW, List.of(devOrders), List.of());
        TopologyGraph right = graph(SCOPE, NOW, List.of(orders), List.of());
        TopologyGraph later = graph(SCOPE, NOW.plusSeconds(60), List.of(movedOrders), List.of());

        assertThat(GraphDiffer.diff(left, right, DiffMode.ENVIRONMENTS).isEmpty())
                .isTrue();
        assertThat(GraphDiffer.diff(right, later, DiffMode.ENVIRONMENTS).isEmpty())
                .isTrue();
        assertThat(GraphDiffer.diff(right, later, DiffMode.TIMELINE).nodesChanged())
                .containsExactly(new NodeChange(orders, movedOrders));
    }

    @Test
    void edgeMetricsAreNeverDrift() {
        TopologyEdge quiet =
                new TopologyEdge(orders.id(), ordersDb.id(), EdgeKind.SYNC, new EdgeMetrics(1, 0, 1, 1, 1, 1));
        TopologyEdge busy = new TopologyEdge(
                orders.id(), ordersDb.id(), EdgeKind.SYNC, new EdgeMetrics(90_000, 900, 50, 90, 200, 900));
        TopologyGraph left = graph(SCOPE, YESTERDAY, List.of(orders, ordersDb), List.of(quiet));
        TopologyGraph right = graph(SCOPE, NOW, List.of(orders, ordersDb), List.of(busy));

        assertThat(GraphDiffer.diff(left, right, DiffMode.TIMELINE).isEmpty()).isTrue();
    }

    @Test
    void sameEndpointsWithAnotherKindAreDifferentEdges() {
        TopologyGraph left =
                graph(SCOPE, YESTERDAY, List.of(orders, events), List.of(edge(orders, events, EdgeKind.PUBLISH, 1)));
        TopologyGraph right =
                graph(SCOPE, NOW, List.of(orders, events), List.of(edge(orders, events, EdgeKind.SYNC, 1)));

        TopologyDiff diff = GraphDiffer.diff(left, right, DiffMode.TIMELINE);

        assertThat(diff.edgesAdded()).containsExactly(new EdgeKey(orders.id(), events.id(), EdgeKind.SYNC));
        assertThat(diff.edgesRemoved()).containsExactly(new EdgeKey(orders.id(), events.id(), EdgeKind.PUBLISH));
    }

    @Test
    void nodeChangeRejectsTwoDifferentNodes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new NodeChange(orders, checkout))
                .withMessageContaining("service:orders")
                .withMessageContaining("service:checkout");
    }

    private static TopologyGraph graph(Scope scope, Instant at, List<TopologyNode> nodes, List<TopologyEdge> edges) {
        return new TopologyGraph(scope, at, nodes, edges);
    }
}

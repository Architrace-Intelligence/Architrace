/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.snapshot;
import static io.github.architrace.control.plane.topology.TestTopology.topic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GraphMergerTest {

  private static final AgentId A = new AgentId(1);
  private static final AgentId B = new AgentId(2);

  @Test
  void mergesSharedNodesAndEdgesAndKeepsTheRest() {
    TopologyNode ordersA =
        new TopologyNode(
            "service:orders",
            NodeType.SERVICE,
            "orders",
            new NodeAttributes(
                Set.of("2.8.1"),
                Set.of(new Deployment("k8s-prod-eu1", "orders")),
                Map.of("team", "checkout", "tier", "gold")));
    TopologyNode ordersB =
        new TopologyNode(
            "service:orders",
            NodeType.SERVICE,
            "orders",
            new NodeAttributes(
                Set.of("2.8.0"),
                Set.of(new Deployment("k8s-prod-eu1", "orders-canary")),
                Map.of("team", "payments", "owner", "bob")));
    TopologyNode db = database("orders");
    TopologyNode events = topic("order-events");
    TopologyEdge callsA =
        new TopologyEdge(
            ordersA.id(), db.id(), EdgeKind.SYNC, new EdgeMetrics(100, 1, 5, 10, 20, 50));
    TopologyEdge callsB =
        new TopologyEdge(
            ordersB.id(), db.id(), EdgeKind.SYNC, new EdgeMetrics(50, 2, 7, 8, 30, 40));
    TopologyEdge publishes = edge(ordersB, events, EdgeKind.PUBLISH, 10);

    TopologyGraph graph =
        GraphMerger.merge(
            SCOPE,
            NOW,
            List.of(
                snapshot(A, NOW, List.of(ordersA, db), List.of(callsA)),
                snapshot(B, NOW, List.of(events, ordersB, db), List.of(publishes, callsB))));

    assertThat(graph.scope()).isEqualTo(SCOPE);
    assertThat(graph.at()).isEqualTo(NOW);
    assertThat(graph.nodes())
        .extracting(TopologyNode::id)
        .containsExactly("db:postgresql/orders", "service:orders", "topic:kafka/order-events");
    NodeAttributes orders = graph.nodes().get(1).attributes();
    assertThat(orders.versions()).containsExactlyInAnyOrder("2.8.0", "2.8.1");
    assertThat(orders.deployments())
        .containsExactlyInAnyOrder(
            new Deployment("k8s-prod-eu1", "orders"),
            new Deployment("k8s-prod-eu1", "orders-canary"));
    assertThat(orders.labels())
        .containsOnly(entry("team", "checkout"), entry("tier", "gold"), entry("owner", "bob"));
    assertThat(graph.edges())
        .containsExactly(
            new TopologyEdge(
                "service:orders",
                "db:postgresql/orders",
                EdgeKind.SYNC,
                new EdgeMetrics(150, 3, 7, 10, 30, 50)),
            publishes);
  }

  @Test
  void noSnapshotsGiveAnEmptyGraph() {
    TopologyGraph graph = GraphMerger.merge(SCOPE, NOW, List.of());

    assertThat(graph.nodes()).isEmpty();
    assertThat(graph.edges()).isEmpty();
  }
}

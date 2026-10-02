/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.service;
import static io.github.architrace.control.plane.topology.TestTopology.snapshot;
import static io.github.architrace.control.plane.topology.TestTopology.topic;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NodeViewsTest {

    private final TopologyNode checkout = service("checkout", "4.1.0", "checkout");
    private final TopologyNode orders = service("orders", "2.8.1", "orders");
    private final TopologyNode db = database("orders");
    private final TopologyNode events = topic("order-events");
    private final TopologyEdge call = edge(checkout, orders, EdgeKind.SYNC, 100);
    private final TopologyEdge read = edge(orders, db, EdgeKind.SYNC, 200);
    private final TopologyEdge publish = edge(orders, events, EdgeKind.PUBLISH, 10);
    private final TopologyEdge consume = edge(events, checkout, EdgeKind.CONSUME, 10);
    private final TopologyGraph graph = GraphMerger.merge(
            SCOPE,
            NOW,
            List.of(snapshot(
                    new AgentId(1),
                    NOW,
                    List.of(checkout, orders, db, events),
                    List.of(call, read, publish, consume))));

    @Test
    void viewsEveryNodeOfTheTypeWithItsInboundAndOutboundEdges() {
        List<NodeView> services = NodeViews.of(graph, NodeType.SERVICE);

        assertThat(services).extracting(view -> view.node().id()).containsExactly("service:checkout", "service:orders");
        assertThat(services.get(0).inbound()).containsExactly(new Dependency(events, consume));
        assertThat(services.get(0).outbound()).containsExactly(new Dependency(orders, call));
        assertThat(services.get(1).inbound()).containsExactly(new Dependency(checkout, call));
        assertThat(services.get(1).outbound())
                .containsExactly(new Dependency(db, read), new Dependency(events, publish));
    }

    @Test
    void dataStreamsSeeTheirProducersInboundAndConsumersOutbound() {
        List<NodeView> topics = NodeViews.of(graph, NodeType.TOPIC);

        assertThat(topics)
                .singleElement()
                .isEqualTo(new NodeView(
                        events, List.of(new Dependency(orders, publish)), List.of(new Dependency(checkout, consume))));
        assertThat(NodeViews.of(graph, NodeType.EXTERNAL)).isEmpty();
    }
}

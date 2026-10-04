/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.testsupport;

import io.github.architrace.graph.EdgeKey;
import io.github.architrace.graph.EdgeKind;
import io.github.architrace.graph.EdgeMetricsSummary;
import io.github.architrace.graph.GraphNode;
import io.github.architrace.graph.GraphNode.DatabaseNode;
import io.github.architrace.graph.GraphNode.ExternalNode;
import io.github.architrace.graph.GraphNode.ServiceNode;
import io.github.architrace.graph.GraphNode.TopicNode;
import io.github.architrace.graph.GraphSnapshot;
import io.github.architrace.graph.Placement;
import io.github.architrace.graph.SnapshotEdge;
import io.github.architrace.graph.SnapshotNode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class TestSnapshots {

    public static final Instant START = Instant.parse("2026-10-04T10:00:00Z");
    public static final Instant END = START.plusSeconds(60);
    public static final ServiceNode CHECKOUT = new ServiceNode("shop", "checkout");
    public static final ServiceNode ORDERS = new ServiceNode("shop", "orders");
    public static final DatabaseNode ORDERS_DB = new DatabaseNode("postgresql", Optional.of("orders"));
    public static final TopicNode ORDERS_TOPIC = new TopicNode("kafka", "orders");
    public static final ExternalNode PAYMENTS = new ExternalNode("payments.example.com");

    private TestSnapshots() {}

    public static GraphSnapshot full() {
        return new GraphSnapshot(
                START,
                END,
                List.of(
                        new SnapshotNode(
                                CHECKOUT,
                                Set.of("1.4.2", "1.4.1"),
                                Set.of(
                                        new Placement("eu-1", Optional.of("checkout")),
                                        new Placement("eu-2", Optional.empty()))),
                        new SnapshotNode(ORDERS, Set.of("2.0"), Set.of()),
                        new SnapshotNode(ORDERS_DB, Set.of(), Set.of()),
                        new SnapshotNode(ORDERS_TOPIC, Set.of(), Set.of()),
                        new SnapshotNode(PAYMENTS, Set.of(), Set.of())),
                List.of(
                        edge(CHECKOUT, ORDERS, EdgeKind.SYNC, new EdgeMetricsSummary(10, 1, 8, 32, 64, 70)),
                        edge(ORDERS, ORDERS_DB, EdgeKind.SYNC, new EdgeMetricsSummary(5, 0, 2, 4, 4, 4)),
                        edge(CHECKOUT, ORDERS_TOPIC, EdgeKind.PUBLISH, new EdgeMetricsSummary(3, 0, 1, 1, 1, 1)),
                        edge(ORDERS_TOPIC, ORDERS, EdgeKind.CONSUME, new EdgeMetricsSummary(3, 0, 1, 1, 1, 1)),
                        edge(CHECKOUT, PAYMENTS, EdgeKind.SYNC, new EdgeMetricsSummary(1, 1, 500, 500, 500, 500))));
    }

    private static SnapshotEdge edge(GraphNode source, GraphNode target, EdgeKind kind, EdgeMetricsSummary metrics) {
        return new SnapshotEdge(new EdgeKey(source, target, kind), metrics);
    }

    public static GraphSnapshot empty(Instant end) {
        return new GraphSnapshot(end.minusSeconds(60), end, List.of(), List.of());
    }
}

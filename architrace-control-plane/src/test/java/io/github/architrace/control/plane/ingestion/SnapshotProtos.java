/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.ingestion;

import io.github.architrace.grpc.proto.Deployment;
import io.github.architrace.grpc.proto.EdgeKind;
import io.github.architrace.grpc.proto.EdgeMetrics;
import io.github.architrace.grpc.proto.GraphSnapshot;
import io.github.architrace.grpc.proto.NodeType;
import io.github.architrace.grpc.proto.SnapshotEdge;
import io.github.architrace.grpc.proto.SnapshotNode;
import java.time.Instant;

public final class SnapshotProtos {

  public static final Instant WINDOW_END = Instant.parse("2026-10-01T12:00:00Z");

  private SnapshotProtos() {}

  public static GraphSnapshot.Builder window() {
    return GraphSnapshot.newBuilder()
        .setWindowStartEpochMs(WINDOW_END.minusSeconds(60).toEpochMilli())
        .setWindowEndEpochMs(WINDOW_END.toEpochMilli());
  }

  public static GraphSnapshot ordersSnapshot() {
    return window()
        .addNodes(
            node("service:orders/orders-service", NodeType.NODE_TYPE_SERVICE, "orders-service")
                .addVersions("2.8.1")
                .addDeployments(
                    Deployment.newBuilder().setCluster("k8s-prod-eu1").setNamespace("orders"))
                .putLabels("replicas", "3"))
        .addNodes(node("db:postgresql/orders", NodeType.NODE_TYPE_DATABASE, "orders"))
        .addNodes(node("topic:kafka/order-events", NodeType.NODE_TYPE_TOPIC, "order-events"))
        .addEdges(
            edge(
                "service:orders/orders-service",
                "db:postgresql/orders",
                EdgeKind.EDGE_KIND_SYNC,
                24000,
                3))
        .addEdges(
            edge(
                "service:orders/orders-service",
                "topic:kafka/order-events",
                EdgeKind.EDGE_KIND_PUBLISH,
                8700,
                0))
        .build();
  }

  public static SnapshotNode.Builder node(String id, NodeType type, String name) {
    return SnapshotNode.newBuilder().setId(id).setType(type).setName(name);
  }

  public static SnapshotEdge.Builder edge(
      String source, String target, EdgeKind kind, long calls, long errors) {
    return SnapshotEdge.newBuilder()
        .setSourceId(source)
        .setTargetId(target)
        .setKind(kind)
        .setMetrics(
            EdgeMetrics.newBuilder()
                .setCalls(calls)
                .setErrors(errors)
                .setP50Millis(3)
                .setP95Millis(9)
                .setP99Millis(22)
                .setMaxMillis(140));
  }
}

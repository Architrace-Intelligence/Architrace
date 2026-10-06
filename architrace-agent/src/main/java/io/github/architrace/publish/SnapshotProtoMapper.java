/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.publish;

import io.github.architrace.graph.EdgeKind;
import io.github.architrace.graph.EdgeMetricsSummary;
import io.github.architrace.graph.GraphNode;
import io.github.architrace.graph.GraphSnapshot;
import io.github.architrace.graph.Placement;
import io.github.architrace.graph.SnapshotEdge;
import io.github.architrace.graph.SnapshotNode;
import io.github.architrace.grpc.proto.Deployment;
import io.github.architrace.grpc.proto.EdgeMetrics;
import io.github.architrace.grpc.proto.NodeType;

public final class SnapshotProtoMapper {

    private SnapshotProtoMapper() {}

    public static io.github.architrace.grpc.proto.GraphSnapshot toProto(GraphSnapshot snapshot) {
        return io.github.architrace.grpc.proto.GraphSnapshot.newBuilder()
                .setWindowStartEpochMs(snapshot.windowStart().toEpochMilli())
                .setWindowEndEpochMs(snapshot.windowEnd().toEpochMilli())
                .addAllNodes(snapshot.nodes().stream()
                        .map(SnapshotProtoMapper::toProto)
                        .toList())
                .addAllEdges(snapshot.edges().stream()
                        .map(SnapshotProtoMapper::toProto)
                        .toList())
                .build();
    }

    private static io.github.architrace.grpc.proto.SnapshotNode toProto(SnapshotNode node) {
        return io.github.architrace.grpc.proto.SnapshotNode.newBuilder()
                .setId(node.node().id())
                .setType(typeOf(node.node()))
                .setName(node.node().name())
                .addAllVersions(node.versions().stream().sorted().toList())
                .addAllDeployments(node.deployments().stream()
                        .map(SnapshotProtoMapper::toProto)
                        .sorted(SnapshotProtoMapper::compare)
                        .toList())
                .build();
    }

    private static io.github.architrace.grpc.proto.SnapshotEdge toProto(SnapshotEdge edge) {
        EdgeMetricsSummary metrics = edge.metrics();
        return io.github.architrace.grpc.proto.SnapshotEdge.newBuilder()
                .setSourceId(edge.key().source().id())
                .setTargetId(edge.key().target().id())
                .setKind(kindOf(edge.key().kind()))
                .setMetrics(EdgeMetrics.newBuilder()
                        .setCalls(metrics.calls())
                        .setErrors(metrics.errors())
                        .setP50Micros(metrics.p50Micros())
                        .setP95Micros(metrics.p95Micros())
                        .setP99Micros(metrics.p99Micros())
                        .setMaxMicros(metrics.maxMicros()))
                .build();
    }

    private static Deployment toProto(Placement placement) {
        return Deployment.newBuilder()
                .setCluster(placement.cluster())
                .setNamespace(placement.namespace().orElse(""))
                .build();
    }

    private static int compare(Deployment left, Deployment right) {
        int byCluster = left.getCluster().compareTo(right.getCluster());
        return byCluster != 0 ? byCluster : left.getNamespace().compareTo(right.getNamespace());
    }

    private static NodeType typeOf(GraphNode node) {
        return switch (node) {
            case GraphNode.ServiceNode _ -> NodeType.NODE_TYPE_SERVICE;
            case GraphNode.DatabaseNode _ -> NodeType.NODE_TYPE_DATABASE;
            case GraphNode.TopicNode _ -> NodeType.NODE_TYPE_TOPIC;
            case GraphNode.ExternalNode _ -> NodeType.NODE_TYPE_EXTERNAL;
        };
    }

    private static io.github.architrace.grpc.proto.EdgeKind kindOf(EdgeKind kind) {
        return switch (kind) {
            case SYNC -> io.github.architrace.grpc.proto.EdgeKind.EDGE_KIND_SYNC;
            case PUBLISH -> io.github.architrace.grpc.proto.EdgeKind.EDGE_KIND_PUBLISH;
            case CONSUME -> io.github.architrace.grpc.proto.EdgeKind.EDGE_KIND_CONSUME;
        };
    }
}

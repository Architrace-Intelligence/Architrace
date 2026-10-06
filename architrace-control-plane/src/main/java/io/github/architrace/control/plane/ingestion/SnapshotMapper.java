/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.ingestion;

import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.Deployment;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.EdgeMetrics;
import io.github.architrace.control.plane.topology.NodeAttributes;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.Snapshot;
import io.github.architrace.control.plane.topology.TimeWindow;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyNode;
import io.github.architrace.grpc.proto.GraphSnapshot;
import io.github.architrace.grpc.proto.SnapshotEdge;
import io.github.architrace.grpc.proto.SnapshotNode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SnapshotMapper {

    private static final long MICROS_PER_MILLI = 1_000L;

    public Snapshot toSnapshot(Agent agent, GraphSnapshot proto, Instant receivedAt) {
        try {
            TimeWindow window = new TimeWindow(
                    Instant.ofEpochMilli(proto.getWindowStartEpochMs()),
                    Instant.ofEpochMilli(proto.getWindowEndEpochMs()));
            List<TopologyNode> nodes =
                    proto.getNodesList().stream().map(SnapshotMapper::toNode).toList();
            List<TopologyEdge> edges =
                    proto.getEdgesList().stream().map(SnapshotMapper::toEdge).toList();
            requireUniqueNodes(nodes);
            requireKnownEndpoints(nodes, edges);
            return new Snapshot(agent.id(), agent.scope(), window, receivedAt, nodes, edges);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidSnapshotException(e.getMessage(), e);
        }
    }

    private static TopologyNode toNode(SnapshotNode node) {
        NodeAttributes attributes = new NodeAttributes(
                Set.copyOf(node.getVersionsList()),
                node.getDeploymentsList().stream()
                        .map(d -> new Deployment(d.getCluster(), d.getNamespace()))
                        .collect(Collectors.toUnmodifiableSet()),
                node.getLabelsMap());
        return new TopologyNode(node.getId(), toType(node.getType()), node.getName(), attributes);
    }

    private static TopologyEdge toEdge(SnapshotEdge edge) {
        io.github.architrace.grpc.proto.EdgeMetrics m = edge.getMetrics();
        return new TopologyEdge(
                edge.getSourceId(),
                edge.getTargetId(),
                toKind(edge.getKind()),
                new EdgeMetrics(
                        m.getCalls(),
                        m.getErrors(),
                        millis(m.getP50Micros()),
                        millis(m.getP95Micros()),
                        millis(m.getP99Micros()),
                        millis(m.getMaxMicros())));
    }

    private static long millis(long micros) {
        return micros / MICROS_PER_MILLI;
    }

    private static NodeType toType(io.github.architrace.grpc.proto.NodeType type) {
        return switch (type) {
            case NODE_TYPE_SERVICE -> NodeType.SERVICE;
            case NODE_TYPE_DATABASE -> NodeType.DATABASE;
            case NODE_TYPE_TOPIC -> NodeType.TOPIC;
            case NODE_TYPE_EXTERNAL -> NodeType.EXTERNAL;
            case NODE_TYPE_UNSPECIFIED, UNRECOGNIZED ->
                throw new InvalidSnapshotException("node type is not specified");
        };
    }

    private static EdgeKind toKind(io.github.architrace.grpc.proto.EdgeKind kind) {
        return switch (kind) {
            case EDGE_KIND_SYNC -> EdgeKind.SYNC;
            case EDGE_KIND_PUBLISH -> EdgeKind.PUBLISH;
            case EDGE_KIND_CONSUME -> EdgeKind.CONSUME;
            case EDGE_KIND_UNSPECIFIED, UNRECOGNIZED ->
                throw new InvalidSnapshotException("edge kind is not specified");
        };
    }

    private static void requireUniqueNodes(List<TopologyNode> nodes) {
        firstDuplicate(nodes.stream().map(TopologyNode::id)).ifPresent(id -> {
            throw new InvalidSnapshotException("duplicate node id " + id);
        });
    }

    private static void requireKnownEndpoints(List<TopologyNode> nodes, List<TopologyEdge> edges) {
        Set<String> ids = nodes.stream().map(TopologyNode::id).collect(Collectors.toSet());
        edges.stream()
                .filter(edge -> !ids.contains(edge.sourceId()) || !ids.contains(edge.targetId()))
                .findFirst()
                .ifPresent(edge -> {
                    throw new InvalidSnapshotException(
                            "edge " + edge.sourceId() + " -> " + edge.targetId() + " references an unknown node");
                });
        firstDuplicate(edges.stream().map(SnapshotMapper::edgeKey)).ifPresent(key -> {
            throw new InvalidSnapshotException("duplicate edge " + key);
        });
    }

    private static String edgeKey(TopologyEdge edge) {
        return edge.sourceId() + " -> " + edge.targetId() + " " + edge.kind();
    }

    private static <T> Optional<T> firstDuplicate(Stream<T> values) {
        return values
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .findFirst();
    }
}

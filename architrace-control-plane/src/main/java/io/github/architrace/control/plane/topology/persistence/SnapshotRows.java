/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import io.github.architrace.control.plane.topology.AgentId;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.EdgeMetrics;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.Snapshot;
import io.github.architrace.control.plane.topology.SnapshotId;
import io.github.architrace.control.plane.topology.SnapshotSummary;
import io.github.architrace.control.plane.topology.TimeWindow;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class SnapshotRows {

  private final NodeAttributesCodec codec;

  SnapshotRows(NodeAttributesCodec codec) {
    this.codec = codec;
  }

  SnapshotRow toRow(Snapshot snapshot) {
    Set<SnapshotNodeRow> nodes =
        snapshot.nodes().stream().map(this::toRow).collect(Collectors.toUnmodifiableSet());
    Set<SnapshotEdgeRow> edges =
        snapshot.edges().stream().map(SnapshotRows::toRow).collect(Collectors.toUnmodifiableSet());
    return new SnapshotRow(
        null,
        snapshot.agentId().value(),
        snapshot.scope().project(),
        snapshot.scope().environment(),
        snapshot.scope().cluster(),
        snapshot.window().start(),
        snapshot.window().end(),
        snapshot.receivedAt(),
        nodes.size(),
        edges.size(),
        nodes,
        edges);
  }

  Snapshot toSnapshot(SnapshotRow row) {
    List<TopologyNode> nodes =
        row.nodes().stream()
            .map(this::toNode)
            .sorted(Comparator.comparing(TopologyNode::id))
            .toList();
    List<TopologyEdge> edges =
        row.edges().stream()
            .map(SnapshotRows::toEdge)
            .sorted(
                Comparator.comparing(TopologyEdge::sourceId)
                    .thenComparing(TopologyEdge::targetId)
                    .thenComparing(TopologyEdge::kind))
            .toList();
    return new Snapshot(
        new AgentId(row.agentId()),
        new Scope(row.project(), row.environment(), row.cluster()),
        new TimeWindow(row.windowStart(), row.windowEnd()),
        row.receivedAt(),
        nodes,
        edges);
  }

  SnapshotSummary toSummary(SnapshotSummaryRow row) {
    return new SnapshotSummary(
        new SnapshotId(row.id()),
        new AgentId(row.agentId()),
        new Scope(row.project(), row.environment(), row.cluster()),
        new TimeWindow(row.windowStart(), row.windowEnd()),
        row.receivedAt(),
        row.nodeCount(),
        row.edgeCount());
  }

  private SnapshotNodeRow toRow(TopologyNode node) {
    return new SnapshotNodeRow(
        node.id(), node.type().name(), node.name(), codec.encode(node.attributes()));
  }

  private TopologyNode toNode(SnapshotNodeRow row) {
    return new TopologyNode(
        row.nodeId(), NodeType.valueOf(row.type()), row.name(), codec.decode(row.attributes()));
  }

  private static SnapshotEdgeRow toRow(TopologyEdge edge) {
    EdgeMetrics m = edge.metrics();
    return new SnapshotEdgeRow(
        edge.sourceId(),
        edge.targetId(),
        edge.kind().name(),
        m.calls(),
        m.errors(),
        m.p50Millis(),
        m.p95Millis(),
        m.p99Millis(),
        m.maxMillis());
  }

  private static TopologyEdge toEdge(SnapshotEdgeRow row) {
    return new TopologyEdge(
        row.sourceId(),
        row.targetId(),
        EdgeKind.valueOf(row.kind()),
        new EdgeMetrics(
            row.calls(),
            row.errors(),
            row.latencyP50Ms(),
            row.latencyP95Ms(),
            row.latencyP99Ms(),
            row.latencyMaxMs()));
  }
}

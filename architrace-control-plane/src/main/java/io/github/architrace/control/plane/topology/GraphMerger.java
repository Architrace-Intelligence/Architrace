/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GraphMerger {

  private static final Comparator<TopologyEdge> EDGE_ORDER =
      Comparator.comparing(TopologyEdge::sourceId)
          .thenComparing(TopologyEdge::targetId)
          .thenComparing(TopologyEdge::kind);

  private GraphMerger() {}

  public static TopologyGraph merge(Scope scope, Instant at, List<Snapshot> snapshots) {
    Map<String, TopologyNode> nodes = new LinkedHashMap<>();
    Map<EdgeKey, TopologyEdge> edges = new LinkedHashMap<>();
    for (Snapshot snapshot : snapshots) {
      for (TopologyNode node : snapshot.nodes()) {
        nodes.merge(node.id(), node, GraphMerger::mergeNodes);
      }
      for (TopologyEdge edge : snapshot.edges()) {
        edges.merge(EdgeKey.of(edge), edge, GraphMerger::mergeEdges);
      }
    }
    return new TopologyGraph(
        scope,
        at,
        nodes.values().stream().sorted(Comparator.comparing(TopologyNode::id)).toList(),
        edges.values().stream().sorted(EDGE_ORDER).toList());
  }

  private static TopologyNode mergeNodes(TopologyNode first, TopologyNode other) {
    Set<String> versions = new HashSet<>(first.attributes().versions());
    versions.addAll(other.attributes().versions());
    Set<Deployment> deployments = new HashSet<>(first.attributes().deployments());
    deployments.addAll(other.attributes().deployments());
    Map<String, String> labels = new LinkedHashMap<>(other.attributes().labels());
    labels.putAll(first.attributes().labels());
    return new TopologyNode(
        first.id(), first.type(), first.name(), new NodeAttributes(versions, deployments, labels));
  }

  private static TopologyEdge mergeEdges(TopologyEdge first, TopologyEdge other) {
    EdgeMetrics a = first.metrics();
    EdgeMetrics b = other.metrics();
    return new TopologyEdge(
        first.sourceId(),
        first.targetId(),
        first.kind(),
        new EdgeMetrics(
            a.calls() + b.calls(),
            a.errors() + b.errors(),
            Math.max(a.p50Millis(), b.p50Millis()),
            Math.max(a.p95Millis(), b.p95Millis()),
            Math.max(a.p99Millis(), b.p99Millis()),
            Math.max(a.maxMillis(), b.maxMillis())));
  }

  private record EdgeKey(String sourceId, String targetId, EdgeKind kind) {

    static EdgeKey of(TopologyEdge edge) {
      return new EdgeKey(edge.sourceId(), edge.targetId(), edge.kind());
    }
  }
}

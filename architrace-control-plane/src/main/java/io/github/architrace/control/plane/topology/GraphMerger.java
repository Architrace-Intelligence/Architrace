/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GraphMerger {

    private static final Comparator<TopologyEdge> EDGE_ORDER = Comparator.comparing(TopologyEdge::sourceId)
            .thenComparing(TopologyEdge::targetId)
            .thenComparing(TopologyEdge::kind);

    private GraphMerger() {}

    public static TopologyGraph merge(Scope scope, Instant at, List<Snapshot> snapshots) {
        List<TopologyNode> nodes = snapshots.stream()
                .flatMap(snapshot -> snapshot.nodes().stream())
                .collect(Collectors.toMap(TopologyNode::id, Function.identity(), GraphMerger::mergeNodes))
                .values()
                .stream()
                .sorted(Comparator.comparing(TopologyNode::id))
                .toList();
        List<TopologyEdge> edges = snapshots.stream()
                .flatMap(snapshot -> snapshot.edges().stream())
                .collect(Collectors.toMap(EdgeKey::of, Function.identity(), GraphMerger::mergeEdges))
                .values()
                .stream()
                .sorted(EDGE_ORDER)
                .toList();
        return new TopologyGraph(scope, at, nodes, edges);
    }

    private static TopologyNode mergeNodes(TopologyNode first, TopologyNode other) {
        NodeAttributes mine = first.attributes();
        NodeAttributes theirs = other.attributes();
        Set<String> versions = Stream.concat(mine.versions().stream(), theirs.versions().stream())
                .collect(Collectors.toUnmodifiableSet());
        Set<Deployment> deployments = Stream.concat(mine.deployments().stream(), theirs.deployments().stream())
                .collect(Collectors.toUnmodifiableSet());
        Map<String, String> labels = Stream.concat(
                        mine.labels().entrySet().stream(), theirs.labels().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (kept, ignored) -> kept));
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

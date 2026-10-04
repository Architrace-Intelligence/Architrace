/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import io.github.architrace.graph.GraphNode.ServiceNode;
import io.github.architrace.span.SpanRecord;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GraphWindow {

    private final Instant start;
    private final Map<GraphNode, NodeState> nodes = new LinkedHashMap<>();
    private final Map<EdgeKey, EdgeMetrics> edges = new LinkedHashMap<>();

    public GraphWindow(Instant start) {
        this.start = Objects.requireNonNull(start, "start");
    }

    public Instant start() {
        return start;
    }

    public void recordService(SpanRecord span) {
        NodeState state =
                state(new ServiceNode(span.service().domain(), span.service().name()));
        state.versions().add(span.service().version());
        state.deployments().add(Placement.of(span.deployment()));
    }

    public void record(EdgeObservation observation) {
        state(observation.edge().source());
        state(observation.edge().target());
        edges.computeIfAbsent(observation.edge(), _ -> new EdgeMetrics())
                .record(observation.latencyMillis(), observation.error());
    }

    public GraphSnapshot freeze(Instant end) {
        List<SnapshotNode> frozenNodes = nodes.entrySet().stream()
                .map(entry -> new SnapshotNode(
                        entry.getKey(),
                        entry.getValue().versions(),
                        entry.getValue().deployments()))
                .toList();
        List<SnapshotEdge> frozenEdges = edges.entrySet().stream()
                .map(entry -> new SnapshotEdge(entry.getKey(), entry.getValue().summary()))
                .toList();
        return new GraphSnapshot(start, end, frozenNodes, frozenEdges);
    }

    private NodeState state(GraphNode node) {
        return nodes.computeIfAbsent(node, _ -> new NodeState(new LinkedHashSet<>(), new LinkedHashSet<>()));
    }

    private record NodeState(Set<String> versions, Set<Placement> deployments) {}
}

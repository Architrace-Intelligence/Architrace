/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class NodeViews {

    private NodeViews() {}

    public static List<NodeView> of(TopologyGraph graph, NodeType type) {
        Map<String, TopologyNode> nodesById =
                graph.nodes().stream().collect(Collectors.toMap(TopologyNode::id, Function.identity()));
        Map<String, List<TopologyEdge>> inbound =
                graph.edges().stream().collect(Collectors.groupingBy(TopologyEdge::targetId));
        Map<String, List<TopologyEdge>> outbound =
                graph.edges().stream().collect(Collectors.groupingBy(TopologyEdge::sourceId));
        return graph.nodes().stream()
                .filter(node -> node.type() == type)
                .map(node -> new NodeView(
                        node,
                        dependencies(inbound.getOrDefault(node.id(), List.of()), TopologyEdge::sourceId, nodesById),
                        dependencies(outbound.getOrDefault(node.id(), List.of()), TopologyEdge::targetId, nodesById)))
                .toList();
    }

    private static List<Dependency> dependencies(
            List<TopologyEdge> edges, Function<TopologyEdge, String> otherEnd, Map<String, TopologyNode> nodesById) {
        return edges.stream()
                .map(edge -> new Dependency(nodesById.get(otherEnd.apply(edge)), edge))
                .toList();
    }
}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Nodes {

    private Nodes() {}

    static Map<String, TopologyNode> byId(TopologyGraph graph) {
        return graph.nodes().stream().collect(Collectors.toMap(TopologyNode::id, Function.identity()));
    }

    static boolean hasType(Map<String, TopologyNode> nodes, String id, NodeType type) {
        TopologyNode node = nodes.get(id);
        return node != null && node.type() == type;
    }

    static Stream<TopologyEdge> syncCalls(
            TopologyGraph graph, Map<String, TopologyNode> nodes, NodeType sourceType, NodeType targetType) {
        return graph.edges().stream()
                .filter(edge -> edge.kind() == EdgeKind.SYNC)
                .filter(edge -> !edge.sourceId().equals(edge.targetId()))
                .filter(edge ->
                        hasType(nodes, edge.sourceId(), sourceType) && hasType(nodes, edge.targetId(), targetType));
    }

    static Map<String, Set<String>> group(
            Stream<TopologyEdge> edges, Function<TopologyEdge, String> key, Function<TopologyEdge, String> value) {
        return edges.collect(Collectors.groupingBy(key, Collectors.mapping(value, Collectors.toSet())));
    }
}

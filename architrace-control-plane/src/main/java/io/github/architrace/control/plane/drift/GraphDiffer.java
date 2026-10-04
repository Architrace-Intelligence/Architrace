/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.drift;

import io.github.architrace.control.plane.topology.EdgeKey;
import io.github.architrace.control.plane.topology.NodeAttributes;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GraphDiffer {

    private static final Comparator<TopologyNode> NODE_ORDER = Comparator.comparing(TopologyNode::id);

    private GraphDiffer() {}

    public static TopologyDiff diff(TopologyGraph left, TopologyGraph right, DiffMode mode) {
        Map<String, TopologyNode> before = byId(left.nodes());
        Map<String, TopologyNode> after = byId(right.nodes());
        Set<EdgeKey> leftEdges = keys(left.edges());
        Set<EdgeKey> rightEdges = keys(right.edges());
        return new TopologyDiff(
                GraphRef.of(left),
                GraphRef.of(right),
                onlyIn(right.nodes(), before),
                onlyIn(left.nodes(), after),
                changed(before, right.nodes(), mode),
                onlyIn(rightEdges, leftEdges),
                onlyIn(leftEdges, rightEdges));
    }

    private static Map<String, TopologyNode> byId(List<TopologyNode> nodes) {
        return nodes.stream().collect(Collectors.toMap(TopologyNode::id, Function.identity()));
    }

    private static Set<EdgeKey> keys(List<TopologyEdge> edges) {
        return edges.stream().map(EdgeKey::of).collect(Collectors.toUnmodifiableSet());
    }

    private static List<TopologyNode> onlyIn(List<TopologyNode> nodes, Map<String, TopologyNode> otherSide) {
        return nodes.stream()
                .filter(node -> !otherSide.containsKey(node.id()))
                .sorted(NODE_ORDER)
                .toList();
    }

    private static List<EdgeKey> onlyIn(Set<EdgeKey> edges, Set<EdgeKey> otherSide) {
        return edges.stream()
                .filter(edge -> !otherSide.contains(edge))
                .sorted(EdgeKey.ORDER)
                .toList();
    }

    private static List<NodeChange> changed(
            Map<String, TopologyNode> before, List<TopologyNode> rightNodes, DiffMode mode) {
        return rightNodes.stream()
                .filter(node -> before.containsKey(node.id()))
                .map(node -> new NodeChange(before.get(node.id()), node))
                .filter(change ->
                        differs(change.before().attributes(), change.after().attributes(), mode))
                .sorted(Comparator.comparing(NodeChange::after, NODE_ORDER))
                .toList();
    }

    private static boolean differs(NodeAttributes before, NodeAttributes after, DiffMode mode) {
        boolean versionsDiffer = !before.versions().equals(after.versions());
        return switch (mode) {
            case ENVIRONMENTS -> versionsDiffer;
            case TIMELINE -> versionsDiffer || !before.deployments().equals(after.deployments());
        };
    }
}

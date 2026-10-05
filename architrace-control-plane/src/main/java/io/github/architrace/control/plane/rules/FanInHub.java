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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record FanInHub(int maxCallers) implements ArchitectureRule {

    public FanInHub {
        if (maxCallers < 1) {
            throw new IllegalArgumentException("maxCallers must be at least 1");
        }
    }

    @Override
    public String id() {
        return "fan-in-hub";
    }

    @Override
    public Finding.Severity severity() {
        return Finding.Severity.MEDIUM;
    }

    @Override
    public List<Finding> evaluate(TopologyGraph graph) {
        Map<String, TopologyNode> nodes =
                graph.nodes().stream().collect(Collectors.toMap(TopologyNode::id, Function.identity()));
        Map<String, Set<String>> callersByTarget = graph.edges().stream()
                .filter(edge -> edge.kind() == EdgeKind.SYNC)
                .filter(edge -> !edge.sourceId().equals(edge.targetId()))
                .filter(edge -> isService(nodes, edge.sourceId()) && isService(nodes, edge.targetId()))
                .collect(Collectors.groupingBy(
                        TopologyEdge::targetId, Collectors.mapping(TopologyEdge::sourceId, Collectors.toSet())));
        return callersByTarget.entrySet().stream()
                .filter(entry -> entry.getValue().size() > maxCallers)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> report(
                        graph,
                        nodes.get(entry.getKey()),
                        entry.getValue().stream().sorted().toList()))
                .toList();
    }

    private static boolean isService(Map<String, TopologyNode> nodes, String id) {
        TopologyNode node = nodes.get(id);
        return node != null && node.type() == NodeType.SERVICE;
    }

    private Finding report(TopologyGraph graph, TopologyNode hub, List<String> callerIds) {
        return finding(
                graph,
                List.of(hub.id()),
                hub.name() + " has " + callerIds.size() + " direct callers",
                callerIds.size() + " services call " + hub.name()
                        + " synchronously, which makes it a coupling hotspot: an incompatible change or an outage there"
                        + " touches all of them",
                callerIds);
    }
}

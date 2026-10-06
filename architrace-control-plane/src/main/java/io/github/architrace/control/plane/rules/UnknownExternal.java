/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.PlatformHosts;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record UnknownExternal(Set<String> allowlist) implements ArchitectureRule {

    public UnknownExternal {
        allowlist = Set.copyOf(allowlist);
    }

    @Override
    public String id() {
        return "unknown-external";
    }

    @Override
    public Finding.Severity severity() {
        return Finding.Severity.LOW;
    }

    @Override
    public List<Finding> evaluate(TopologyGraph graph) {
        Map<String, String> names =
                graph.nodes().stream().collect(Collectors.toMap(TopologyNode::id, TopologyNode::name));
        Map<String, Set<String>> callersByTarget = graph.edges().stream()
                .filter(edge -> edge.kind() == EdgeKind.SYNC)
                .filter(edge -> names.containsKey(edge.sourceId()))
                .collect(Collectors.groupingBy(
                        TopologyEdge::targetId, Collectors.mapping(TopologyEdge::sourceId, Collectors.toSet())));
        return graph.nodes().stream()
                .filter(node -> node.type() == NodeType.EXTERNAL)
                .filter(node -> !allowlist.contains(node.name()))
                .filter(node -> !PlatformHosts.isPlatform(node))
                .sorted(Comparator.comparing(TopologyNode::id))
                .map(node -> report(
                        graph,
                        node,
                        callersByTarget.getOrDefault(node.id(), Set.of()).stream()
                                .sorted()
                                .toList(),
                        names))
                .toList();
    }

    private Finding report(
            TopologyGraph graph, TopologyNode external, List<String> callerIds, Map<String, String> names) {
        String callers = callerIds.stream().map(names::get).collect(Collectors.joining(", "));
        String detail = callerIds.isEmpty()
                ? external.name() + " is not on the allowlist of known external systems"
                : callers + " call " + external.name() + ", which is not on the allowlist of known external systems";
        return finding(graph, List.of(external.id()), "Unknown external system " + external.name(), detail, callerIds);
    }
}

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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record DataStreamWithoutProducer() implements ArchitectureRule {

    @Override
    public String id() {
        return "data-stream-without-producer";
    }

    @Override
    public Finding.Severity severity() {
        return Finding.Severity.LOW;
    }

    @Override
    public List<Finding> evaluate(TopologyGraph graph) {
        Map<String, TopologyNode> nodes = Nodes.byId(graph);
        Map<String, Set<String>> producers = Nodes.group(
                edges(graph, nodes, EdgeKind.PUBLISH, TopologyEdge::targetId),
                TopologyEdge::targetId,
                TopologyEdge::sourceId);
        Map<String, Set<String>> consumers = Nodes.group(
                edges(graph, nodes, EdgeKind.CONSUME, TopologyEdge::sourceId),
                TopologyEdge::sourceId,
                TopologyEdge::targetId);
        return graph.nodes().stream()
                .filter(node -> node.type() == NodeType.TOPIC)
                .filter(node -> consumers.containsKey(node.id()) && !producers.containsKey(node.id()))
                .sorted(Comparator.comparing(TopologyNode::id))
                .map(topic -> report(
                        graph,
                        topic,
                        consumers.get(topic.id()).stream().sorted().toList(),
                        nodes))
                .toList();
    }

    private static Stream<TopologyEdge> edges(
            TopologyGraph graph,
            Map<String, TopologyNode> nodes,
            EdgeKind kind,
            Function<TopologyEdge, String> topicEnd) {
        return graph.edges().stream()
                .filter(edge -> edge.kind() == kind)
                .filter(edge -> Nodes.hasType(nodes, topicEnd.apply(edge), NodeType.TOPIC));
    }

    private Finding report(
            TopologyGraph graph, TopologyNode topic, List<String> consumerIds, Map<String, TopologyNode> nodes) {
        String names = consumerIds.stream().map(id -> nodes.get(id).name()).collect(Collectors.joining(", "));
        return finding(
                graph,
                List.of(topic.id()),
                "Data stream " + topic.name() + " has consumers but no producer",
                names + " consume " + topic.name() + "; no service publishes to it in the observed traces. A topic"
                        + " filled by change data capture from an outbox table looks like this: the producing"
                        + " service only writes to its database",
                consumerIds);
    }
}

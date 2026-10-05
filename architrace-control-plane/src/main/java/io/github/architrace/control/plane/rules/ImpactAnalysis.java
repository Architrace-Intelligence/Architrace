/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.rules.Impact.ImpactedNode;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class ImpactAnalysis {

    private static final Comparator<Reach> ORDER = Comparator.comparingInt(Reach::distance)
            .thenComparing(Reach::id)
            .thenComparing(Reach::path, ImpactAnalysis::comparePaths);

    private ImpactAnalysis() {}

    public static Optional<Impact> of(TopologyGraph graph, String nodeId) {
        Map<String, TopologyNode> nodes =
                graph.nodes().stream().collect(Collectors.toMap(TopologyNode::id, Function.identity()));
        return Optional.ofNullable(nodes.get(nodeId)).map(subject -> analyse(graph, subject, nodes));
    }

    private static Impact analyse(TopologyGraph graph, TopologyNode subject, Map<String, TopologyNode> nodes) {
        Map<String, List<String>> callers =
                adjacency(graph, nodes, EdgeKind.SYNC, TopologyEdge::targetId, TopologyEdge::sourceId);
        Map<String, List<String>> producers =
                adjacency(graph, nodes, EdgeKind.PUBLISH, TopologyEdge::targetId, TopologyEdge::sourceId);
        Map<String, List<String>> topics =
                adjacency(graph, nodes, EdgeKind.PUBLISH, TopologyEdge::sourceId, TopologyEdge::targetId);
        Map<String, List<String>> consumers =
                adjacency(graph, nodes, EdgeKind.CONSUME, TopologyEdge::sourceId, TopologyEdge::targetId);
        Reach origin = new Reach(subject.id(), 0, List.of(subject.id()));
        boolean topicSubject = subject.type() == NodeType.TOPIC;
        List<Reach> seeds = topicSubject
                ? producers.getOrDefault(subject.id(), List.of()).stream()
                        .map(origin::extend)
                        .toList()
                : List.of(origin);
        Map<String, Reach> impaired = impaired(seeds, callers, subject.id());
        Stream<Reach> publishers =
                Stream.concat(topicSubject ? Stream.empty() : Stream.of(origin), impaired.values().stream());
        Stream<Reach> topicReaches = Stream.concat(
                topicSubject ? Stream.of(origin) : Stream.empty(),
                publishers.flatMap(publisher -> extendAll(publisher, topics)));
        Map<String, Reach> delayed = topicReaches
                .flatMap(topic -> extendAll(topic, consumers))
                .filter(reach -> !reach.id().equals(subject.id()) && !impaired.containsKey(reach.id()))
                .collect(Collectors.toMap(Reach::id, Function.identity(), BinaryOperator.minBy(ORDER)));
        return new Impact(
                subject,
                graph.at(),
                toNodes(impaired, nodes),
                toNodes(delayed, nodes),
                (int) impaired.keySet().stream()
                        .filter(id -> nodes.get(id).type() == NodeType.SERVICE)
                        .count(),
                (int) graph.nodes().stream()
                        .filter(node -> node.type() == NodeType.SERVICE)
                        .count());
    }

    private static Map<String, Reach> impaired(List<Reach> seeds, Map<String, List<String>> callers, String subjectId) {
        Map<String, Reach> reached = seeds.stream()
                .collect(Collectors.toMap(Reach::id, Function.identity(), BinaryOperator.minBy(ORDER), HashMap::new));
        Deque<Reach> queue = new ArrayDeque<>(seeds);
        while (!queue.isEmpty()) {
            Reach current = queue.poll();
            for (String caller : callers.getOrDefault(current.id(), List.of())) {
                if (!reached.containsKey(caller)) {
                    Reach next = current.extend(caller);
                    reached.put(caller, next);
                    queue.add(next);
                }
            }
        }
        reached.remove(subjectId);
        return reached;
    }

    private static Stream<Reach> extendAll(Reach reach, Map<String, List<String>> neighbours) {
        return neighbours.getOrDefault(reach.id(), List.of()).stream().map(reach::extend);
    }

    private static Map<String, List<String>> adjacency(
            TopologyGraph graph,
            Map<String, TopologyNode> nodes,
            EdgeKind kind,
            Function<TopologyEdge, String> key,
            Function<TopologyEdge, String> value) {
        return graph.edges().stream()
                .filter(edge -> edge.kind() == kind)
                .filter(edge -> nodes.containsKey(edge.sourceId()) && nodes.containsKey(edge.targetId()))
                .collect(Collectors.groupingBy(
                        key,
                        Collectors.mapping(
                                value,
                                Collectors.collectingAndThen(Collectors.toCollection(TreeSet::new), List::copyOf))));
    }

    private static int comparePaths(List<String> left, List<String> right) {
        return IntStream.range(0, Math.min(left.size(), right.size()))
                .map(index -> left.get(index).compareTo(right.get(index)))
                .filter(result -> result != 0)
                .findFirst()
                .orElse(Integer.compare(left.size(), right.size()));
    }

    private static List<ImpactedNode> toNodes(Map<String, Reach> reaches, Map<String, TopologyNode> nodes) {
        return reaches.values().stream()
                .sorted(ORDER)
                .map(reach -> new ImpactedNode(nodes.get(reach.id()), reach.distance(), reach.path()))
                .toList();
    }

    private record Reach(String id, int distance, List<String> path) {

        private Reach extend(String next) {
            return new Reach(
                    next,
                    distance + 1,
                    Stream.concat(Stream.of(next), path.stream()).toList());
        }
    }
}

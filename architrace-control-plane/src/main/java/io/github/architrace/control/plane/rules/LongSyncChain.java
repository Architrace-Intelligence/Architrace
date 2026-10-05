/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record LongSyncChain(int maxHops) implements ArchitectureRule {

    public LongSyncChain {
        if (maxHops < 1) {
            throw new IllegalArgumentException("maxHops must be at least 1");
        }
    }

    @Override
    public String id() {
        return "long-sync-chain";
    }

    @Override
    public Finding.Severity severity() {
        return Finding.Severity.MEDIUM;
    }

    @Override
    public List<Finding> evaluate(TopologyGraph graph) {
        Map<String, String> names =
                graph.nodes().stream().collect(Collectors.toMap(TopologyNode::id, TopologyNode::name));
        Map<String, SortedSet<String>> callees = graph.edges().stream()
                .filter(edge -> edge.kind() == EdgeKind.SYNC)
                .filter(edge -> !edge.sourceId().equals(edge.targetId()))
                .filter(edge -> names.containsKey(edge.sourceId()) && names.containsKey(edge.targetId()))
                .collect(Collectors.groupingBy(
                        TopologyEdge::sourceId,
                        Collectors.mapping(TopologyEdge::targetId, Collectors.toCollection(TreeSet::new))));
        Condensation condensation = Condensation.of(callees);
        return condensation
                .entries()
                .filter(entry -> condensation.hops(entry) > maxHops)
                .mapToObj(entry -> condensation.path(entry))
                .sorted(Comparator.comparing(List::getFirst))
                .map(path -> report(graph, path, names))
                .toList();
    }

    private Finding report(TopologyGraph graph, List<String> path, Map<String, String> names) {
        String chain = path.stream().map(names::get).collect(Collectors.joining(" -> "));
        return finding(
                graph,
                path,
                "Synchronous chain of " + (path.size() - 1) + " hops from " + names.get(path.getFirst()),
                chain + ": every hop adds latency and a point of failure to the request",
                path);
    }

    private static final class Condensation {

        private final List<List<String>> components;
        private final int[] hops;
        private final int[] next;
        private final boolean[] hasCaller;

        private Condensation(List<List<String>> components, int[] hops, int[] next, boolean[] hasCaller) {
            this.components = components;
            this.hops = hops;
            this.next = next;
            this.hasCaller = hasCaller;
        }

        private static Condensation of(Map<String, SortedSet<String>> callees) {
            List<List<String>> components = StronglyConnectedComponents.of(callees);
            Map<String, Integer> componentOf = IntStream.range(0, components.size())
                    .boxed()
                    .flatMap(index -> components.get(index).stream().map(id -> Map.entry(id, index)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            int[] hops = new int[components.size()];
            int[] next = new int[components.size()];
            boolean[] hasCaller = new boolean[components.size()];
            Arrays.fill(next, -1);
            for (int index = 0; index < components.size(); index++) {
                for (String member : components.get(index)) {
                    for (String callee : callees.getOrDefault(member, new TreeSet<>())) {
                        int target = componentOf.get(callee);
                        if (target == index) {
                            continue;
                        }
                        hasCaller[target] = true;
                        int candidate = hops[target] + 1;
                        boolean longer = candidate > hops[index];
                        boolean sameLengthSmallerId = candidate == hops[index]
                                && components
                                                .get(target)
                                                .getFirst()
                                                .compareTo(components
                                                        .get(next[index])
                                                        .getFirst())
                                        < 0;
                        if (longer || sameLengthSmallerId) {
                            hops[index] = candidate;
                            next[index] = target;
                        }
                    }
                }
            }
            return new Condensation(components, hops, next, hasCaller);
        }

        private IntStream entries() {
            return IntStream.range(0, components.size()).filter(index -> !hasCaller[index]);
        }

        private int hops(int entry) {
            return hops[entry];
        }

        private List<String> path(int entry) {
            return Stream.iterate(entry, index -> index >= 0, index -> next[index])
                    .map(index -> components.get(index).getFirst())
                    .toList();
        }
    }
}

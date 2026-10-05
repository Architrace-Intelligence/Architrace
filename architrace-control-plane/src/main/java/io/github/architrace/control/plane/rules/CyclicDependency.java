/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public record CyclicDependency() implements ArchitectureRule {

    @Override
    public String id() {
        return "cyclic-dependency";
    }

    @Override
    public Finding.Severity severity() {
        return Finding.Severity.HIGH;
    }

    @Override
    public List<Finding> evaluate(TopologyGraph graph) {
        Map<String, String> names =
                graph.nodes().stream().collect(Collectors.toMap(TopologyNode::id, TopologyNode::name));
        Map<String, SortedSet<String>> callees = graph.edges().stream()
                .filter(edge -> edge.kind() == EdgeKind.SYNC)
                .filter(edge -> !edge.sourceId().equals(edge.targetId()))
                .collect(Collectors.groupingBy(
                        TopologyEdge::sourceId,
                        Collectors.mapping(TopologyEdge::targetId, Collectors.toCollection(TreeSet::new))));
        return new Tarjan(callees)
                .cyclicComponents().stream()
                        .map(component -> report(graph, component, shortestCycle(component, callees), names))
                        .toList();
    }

    private Finding report(TopologyGraph graph, List<String> component, List<String> cycle, Map<String, String> names) {
        String path = cycle.stream().map(id -> names.getOrDefault(id, id)).collect(Collectors.joining(" -> "));
        return finding(
                graph,
                component,
                "Cyclic dependency between " + component.size() + " services",
                path + " -> " + names.getOrDefault(cycle.getFirst(), cycle.getFirst())
                        + " over synchronous calls; a failure or a slowdown in any of them propagates around the cycle",
                cycle);
    }

    private static List<String> shortestCycle(List<String> component, Map<String, SortedSet<String>> callees) {
        String start = component.getFirst();
        Set<String> members = Set.copyOf(component);
        Map<String, String> previous = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>(List.of(start));
        while (!queue.isEmpty()) {
            String node = queue.poll();
            for (String next : callees.getOrDefault(node, new TreeSet<>())) {
                if (next.equals(start)) {
                    return pathFrom(start, node, previous);
                }
                if (members.contains(next) && previous.putIfAbsent(next, node) == null) {
                    queue.add(next);
                }
            }
        }
        throw new IllegalStateException("no cycle through " + start + " in " + component);
    }

    private static List<String> pathFrom(String start, String last, Map<String, String> previous) {
        Deque<String> path = new ArrayDeque<>();
        for (String node = last; !node.equals(start); node = previous.get(node)) {
            path.addFirst(node);
        }
        path.addFirst(start);
        return List.copyOf(path);
    }

    private static final class Tarjan {

        private final Map<String, SortedSet<String>> callees;
        private final Map<String, Integer> index = new HashMap<>();
        private final Map<String, Integer> lowLink = new HashMap<>();
        private final Deque<String> stack = new ArrayDeque<>();
        private final Set<String> onStack = new HashSet<>();
        private final List<List<String>> components = new ArrayList<>();
        private int counter;

        private Tarjan(Map<String, SortedSet<String>> callees) {
            this.callees = callees;
        }

        private List<List<String>> cyclicComponents() {
            for (String node : new TreeSet<>(callees.keySet())) {
                if (!index.containsKey(node)) {
                    visit(node);
                }
            }
            return components.stream()
                    .filter(component -> component.size() > 1)
                    .map(component -> component.stream().sorted().toList())
                    .sorted(Comparator.comparing(List::getFirst))
                    .toList();
        }

        private void visit(String node) {
            index.put(node, counter);
            lowLink.put(node, counter);
            counter++;
            stack.push(node);
            onStack.add(node);
            for (String next : callees.getOrDefault(node, new TreeSet<>())) {
                if (!index.containsKey(next)) {
                    visit(next);
                    lowLink.merge(node, lowLink.get(next), Math::min);
                } else if (onStack.contains(next)) {
                    lowLink.merge(node, index.get(next), Math::min);
                }
            }
            if (lowLink.get(node).equals(index.get(node))) {
                components.add(popComponent(node));
            }
        }

        private List<String> popComponent(String root) {
            List<String> component = new ArrayList<>();
            String member;
            do {
                member = stack.pop();
                onStack.remove(member);
                component.add(member);
            } while (!member.equals(root));
            return component;
        }
    }
}

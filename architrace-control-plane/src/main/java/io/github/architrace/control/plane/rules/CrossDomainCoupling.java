/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record CrossDomainCoupling(int maxDomains) implements ArchitectureRule {

    private static final String SERVICE_PREFIX = "service:";

    public CrossDomainCoupling {
        if (maxDomains < 1) {
            throw new IllegalArgumentException("maxDomains must be at least 1");
        }
    }

    @Override
    public String id() {
        return "cross-domain-coupling";
    }

    @Override
    public Finding.Severity severity() {
        return Finding.Severity.MEDIUM;
    }

    @Override
    public List<Finding> evaluate(TopologyGraph graph) {
        Map<String, TopologyNode> nodes = Nodes.byId(graph);
        Map<String, Set<String>> foreignCalleesBySource = Nodes.group(
                Nodes.syncCalls(graph, nodes, NodeType.SERVICE, NodeType.SERVICE)
                        .filter(edge -> !domain(edge.sourceId()).equals(domain(edge.targetId()))),
                TopologyEdge::sourceId,
                TopologyEdge::targetId);
        return foreignCalleesBySource.entrySet().stream()
                .filter(entry -> domains(entry.getValue()).size() > maxDomains)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> report(
                        graph,
                        nodes.get(entry.getKey()),
                        entry.getValue().stream().sorted().toList(),
                        nodes))
                .toList();
    }

    private static Set<String> domains(Set<String> serviceIds) {
        return serviceIds.stream().map(CrossDomainCoupling::domain).collect(Collectors.toSet());
    }

    static String domain(String serviceId) {
        int separator = serviceId.lastIndexOf('/');
        return serviceId.startsWith(SERVICE_PREFIX) && separator > SERVICE_PREFIX.length()
                ? serviceId.substring(SERVICE_PREFIX.length(), separator)
                : "";
    }

    private Finding report(
            TopologyGraph graph, TopologyNode source, List<String> calleeIds, Map<String, TopologyNode> nodes) {
        int domainCount = domains(Set.copyOf(calleeIds)).size();
        String callees = calleeIds.stream()
                .map(id -> nodes.get(id).name() + " (" + domain(id) + ")")
                .collect(Collectors.joining(", "));
        return finding(
                graph,
                List.of(source.id()),
                source.name() + " calls into " + domainCount + " domains",
                source.name() + " (" + domain(source.id()) + ") calls " + callees + " in " + domainCount
                        + " other domains; synchronous coupling across domain boundaries spreads changes and outages"
                        + " between teams",
                calleeIds);
    }
}

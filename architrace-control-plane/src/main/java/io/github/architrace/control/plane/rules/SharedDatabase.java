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

public record SharedDatabase(int minServices) implements ArchitectureRule {

    public SharedDatabase {
        if (minServices < 2) {
            throw new IllegalArgumentException("minServices must be at least 2");
        }
    }

    @Override
    public String id() {
        return "shared-database";
    }

    @Override
    public Finding.Severity severity() {
        return Finding.Severity.HIGH;
    }

    @Override
    public List<Finding> evaluate(TopologyGraph graph) {
        Map<String, TopologyNode> nodes = Nodes.byId(graph);
        Map<String, Set<String>> clientsByDatabase = Nodes.group(
                Nodes.syncCalls(graph, nodes, NodeType.SERVICE, NodeType.DATABASE),
                TopologyEdge::targetId,
                TopologyEdge::sourceId);
        return clientsByDatabase.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= minServices)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> report(
                        graph,
                        nodes.get(entry.getKey()),
                        entry.getValue().stream().sorted().toList(),
                        nodes))
                .toList();
    }

    private Finding report(
            TopologyGraph graph, TopologyNode database, List<String> clientIds, Map<String, TopologyNode> nodes) {
        String clients = clientIds.stream().map(id -> nodes.get(id).name()).collect(Collectors.joining(", "));
        return finding(
                graph,
                List.of(database.id()),
                "Database " + database.name() + " is shared by " + clientIds.size() + " services",
                clients + " use the database " + database.name()
                        + " directly; a schema change for one of them affects all of them",
                clientIds);
    }
}

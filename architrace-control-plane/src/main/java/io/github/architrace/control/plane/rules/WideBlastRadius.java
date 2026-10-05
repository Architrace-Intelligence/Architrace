/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.rules.Impact.ImpactedNode;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record WideBlastRadius(int minSharePercent, int minServices) implements ArchitectureRule {

    public WideBlastRadius {
        if (minSharePercent < 1 || minSharePercent > 100) {
            throw new IllegalArgumentException("minSharePercent must be between 1 and 100");
        }
        if (minServices < 1) {
            throw new IllegalArgumentException("minServices must be at least 1");
        }
    }

    @Override
    public String id() {
        return "wide-blast-radius";
    }

    @Override
    public Finding.Severity severity() {
        return Finding.Severity.HIGH;
    }

    @Override
    public List<Finding> evaluate(TopologyGraph graph) {
        return graph.nodes().stream()
                .sorted(Comparator.comparing(TopologyNode::id))
                .map(node -> ImpactAnalysis.of(graph, node.id()))
                .flatMap(Optional::stream)
                .filter(this::exceedsThreshold)
                .map(impact -> report(graph, impact))
                .toList();
    }

    private boolean exceedsThreshold(Impact impact) {
        return impact.services() >= minServices && impact.services() * 100 > minSharePercent * impact.servicesTotal();
    }

    private Finding report(TopologyGraph graph, Impact impact) {
        String subject = impact.subject().name();
        List<ImpactedNode> impairedServices = impact.impaired().stream()
                .filter(impacted -> impacted.node().type() == NodeType.SERVICE)
                .toList();
        String names = impairedServices.stream()
                .map(impacted -> impacted.node().name())
                .collect(Collectors.joining(", "));
        return finding(
                graph,
                List.of(impact.subject().id()),
                "Failure of " + subject + " impairs " + impact.services() + " of " + impact.servicesTotal()
                        + " services",
                "If " + subject + " fails, " + impact.services() * 100 / impact.servicesTotal()
                        + " % of the services of the scope stop working: " + names,
                impact.impaired().stream().map(impacted -> impacted.node().id()).toList());
    }
}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FindingsQuery {

    private final TopologyQuery topology;
    private final FindingStore findings;

    public FindingsQuery(TopologyQuery topology, FindingStore findings) {
        this.topology = topology;
        this.findings = findings;
    }

    public List<Finding> findings(Scope scope, Optional<Finding.Severity> severity, Optional<String> ruleId) {
        topology.requireKnown(scope);
        return findings.findings(scope).stream()
                .filter(finding -> severity.map(finding.severity()::equals).orElse(true))
                .filter(finding -> ruleId.map(finding.ruleId()::equals).orElse(true))
                .toList();
    }

    public Impact impact(Scope scope, String nodeId, Instant at) {
        TopologyGraph graph = topology.currentGraph(scope, at);
        return ImpactAnalysis.of(graph, nodeId).orElseThrow(() -> new NodeNotFoundException(scope, nodeId));
    }
}

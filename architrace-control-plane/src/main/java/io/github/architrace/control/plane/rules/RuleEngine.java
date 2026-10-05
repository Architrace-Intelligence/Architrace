/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.TopologyGraph;
import java.util.List;
import java.util.Set;

public final class RuleEngine {

    private final List<ArchitectureRule> rules;

    public RuleEngine(List<ArchitectureRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static RuleEngine of(RulesProperties properties) {
        return new RuleEngine(List.of(
                new CyclicDependency(),
                new SharedDatabase(properties.sharedDatabase().minServices()),
                new UnknownExternal(Set.copyOf(properties.unknownExternal().allowlist()))));
    }

    public List<ArchitectureRule> rules() {
        return rules;
    }

    public List<Finding> evaluate(TopologyGraph graph) {
        return rules.stream()
                .flatMap(rule -> rule.evaluate(graph).stream())
                .sorted(Finding.ORDER)
                .toList();
    }
}

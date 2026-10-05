/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.TopologyGraph;
import java.util.List;

public sealed interface ArchitectureRule permits CyclicDependency, SharedDatabase, UnknownExternal {

    String id();

    Finding.Severity severity();

    List<Finding> evaluate(TopologyGraph graph);

    default Finding finding(
            TopologyGraph graph, List<String> subjectNodeIds, String title, String detail, List<String> evidence) {
        return new Finding(id(), severity(), graph.scope(), subjectNodeIds, title, detail, evidence, graph.at());
    }
}

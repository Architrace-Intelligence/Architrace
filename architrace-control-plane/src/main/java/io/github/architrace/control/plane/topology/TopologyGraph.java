/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record TopologyGraph(
    Scope scope, Instant at, List<TopologyNode> nodes, List<TopologyEdge> edges) {

  public TopologyGraph {
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(at, "at");
    nodes = List.copyOf(nodes);
    edges = List.copyOf(edges);
  }
}

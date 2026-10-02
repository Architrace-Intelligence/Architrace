/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Snapshot(
        AgentId agentId,
        Scope scope,
        TimeWindow window,
        Instant receivedAt,
        List<TopologyNode> nodes,
        List<TopologyEdge> edges) {

    public Snapshot {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(receivedAt, "receivedAt");
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }
}

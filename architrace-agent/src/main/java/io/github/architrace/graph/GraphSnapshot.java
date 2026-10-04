/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record GraphSnapshot(
        Instant windowStart, Instant windowEnd, List<SnapshotNode> nodes, List<SnapshotEdge> edges) {

    public GraphSnapshot {
        Objects.requireNonNull(windowStart, "windowStart");
        Objects.requireNonNull(windowEnd, "windowEnd");
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }

    public boolean isEmpty() {
        return nodes.isEmpty() && edges.isEmpty();
    }
}

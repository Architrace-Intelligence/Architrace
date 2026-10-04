/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.drift;

import io.github.architrace.control.plane.topology.EdgeKey;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.List;
import java.util.Objects;

public record TopologyDiff(
        GraphRef left,
        GraphRef right,
        List<TopologyNode> nodesAdded,
        List<TopologyNode> nodesRemoved,
        List<NodeChange> nodesChanged,
        List<EdgeKey> edgesAdded,
        List<EdgeKey> edgesRemoved) {

    public TopologyDiff {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        nodesAdded = List.copyOf(nodesAdded);
        nodesRemoved = List.copyOf(nodesRemoved);
        nodesChanged = List.copyOf(nodesChanged);
        edgesAdded = List.copyOf(edgesAdded);
        edgesRemoved = List.copyOf(edgesRemoved);
    }

    public boolean isEmpty() {
        return nodesAdded.isEmpty()
                && nodesRemoved.isEmpty()
                && nodesChanged.isEmpty()
                && edgesAdded.isEmpty()
                && edgesRemoved.isEmpty();
    }
}

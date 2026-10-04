/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import java.util.Objects;
import java.util.Set;

public record SnapshotNode(GraphNode node, Set<String> versions, Set<Placement> deployments) {

    public SnapshotNode {
        Objects.requireNonNull(node, "node");
        versions = Set.copyOf(versions);
        deployments = Set.copyOf(deployments);
    }
}

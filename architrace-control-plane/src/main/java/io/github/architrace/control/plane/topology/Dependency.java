/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.util.Objects;

public record Dependency(TopologyNode node, TopologyEdge edge) {

    public Dependency {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(edge, "edge");
    }
}

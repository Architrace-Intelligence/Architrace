/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.util.List;
import java.util.Objects;

public record NodeView(TopologyNode node, List<Dependency> inbound, List<Dependency> outbound) {

    public NodeView {
        Objects.requireNonNull(node, "node");
        inbound = List.copyOf(inbound);
        outbound = List.copyOf(outbound);
    }
}

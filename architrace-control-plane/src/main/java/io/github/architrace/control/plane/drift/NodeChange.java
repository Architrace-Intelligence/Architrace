/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.drift;

import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.Objects;

public record NodeChange(TopologyNode before, TopologyNode after) {

    public NodeChange {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        if (!before.id().equals(after.id())) {
            throw new IllegalArgumentException(
                    "a node change compares one node with itself, got " + before.id() + " and " + after.id());
        }
    }
}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.util.Objects;

public record TopologyNode(String id, NodeType type, String name, NodeAttributes attributes) {

    public TopologyNode {
        Names.requireIdentifier(id, "id");
        Objects.requireNonNull(type, "type");
        Names.requireIdentifier(name, "name");
        Objects.requireNonNull(attributes, "attributes");
    }
}

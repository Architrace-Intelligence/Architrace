/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.util.Objects;

public record TopologyEdge(String sourceId, String targetId, EdgeKind kind, EdgeMetrics metrics) {

    public TopologyEdge {
        Names.requireIdentifier(sourceId, "sourceId");
        Names.requireIdentifier(targetId, "targetId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(metrics, "metrics");
    }
}

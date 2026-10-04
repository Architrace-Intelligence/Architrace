/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.util.Comparator;
import java.util.Objects;

public record EdgeKey(String sourceId, String targetId, EdgeKind kind) {

    public static final Comparator<EdgeKey> ORDER = Comparator.comparing(EdgeKey::sourceId)
            .thenComparing(EdgeKey::targetId)
            .thenComparing(EdgeKey::kind);

    public EdgeKey {
        Names.requireIdentifier(sourceId, "sourceId");
        Names.requireIdentifier(targetId, "targetId");
        Objects.requireNonNull(kind, "kind");
    }

    public static EdgeKey of(TopologyEdge edge) {
        return new EdgeKey(edge.sourceId(), edge.targetId(), edge.kind());
    }
}

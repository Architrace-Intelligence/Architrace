/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.drift;

import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.TopologyGraph;
import java.time.Instant;
import java.util.Objects;

public record GraphRef(Scope scope, Instant at) {

    public GraphRef {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(at, "at");
    }

    public static GraphRef of(TopologyGraph graph) {
        return new GraphRef(graph.scope(), graph.at());
    }
}

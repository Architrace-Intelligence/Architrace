/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import io.github.architrace.span.SpanRecord;
import java.util.Objects;

public record EdgeObservation(EdgeKey edge, long latencyMicros, boolean error) {

    public EdgeObservation {
        Objects.requireNonNull(edge, "edge");
    }

    static EdgeObservation of(SpanRecord span, GraphNode source, GraphNode target, EdgeKind kind) {
        return new EdgeObservation(new EdgeKey(source, target, kind), span.latencyMicros(), span.error());
    }
}

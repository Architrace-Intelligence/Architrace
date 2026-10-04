/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import io.github.architrace.span.SpanRecord;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Objects;

public final class GraphBuilder {

    private final String environment;
    private final EdgeBuilder edges;
    private final InstantSource clock;
    private GraphWindow window;
    private long foreignSpans;
    private long droppedSpans;

    public GraphBuilder(String environment, EdgeBuilder edges, InstantSource clock) {
        this.environment = Objects.requireNonNull(environment, "environment");
        this.edges = Objects.requireNonNull(edges, "edges");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.window = new GraphWindow(clock.instant());
    }

    public void onSpan(SpanRecord span) {
        if (!environment.equals(span.service().environment())) {
            foreignSpans++;
            return;
        }
        window.recordService(span);
        edges.onSpan(span).forEach(window::record);
    }

    public void sweep(Instant now) {
        EdgeBuilder.Expiry expiry = edges.expire(now);
        expiry.externalEdges().forEach(window::record);
        droppedSpans += expiry.droppedSpans();
    }

    public GraphSnapshot freeze() {
        Instant now = clock.instant();
        GraphSnapshot snapshot = window.freeze(now);
        window = new GraphWindow(now);
        return snapshot;
    }

    public long foreignSpans() {
        return foreignSpans;
    }

    public long droppedSpans() {
        return droppedSpans;
    }

    public int pendingSpans() {
        return edges.pendingSpans();
    }
}

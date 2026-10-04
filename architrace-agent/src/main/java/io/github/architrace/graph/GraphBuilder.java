/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import io.github.architrace.span.SpanRecord;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

public final class GraphBuilder {

    private final String environment;
    private final EdgeBuilder edges;
    private final InstantSource clock;
    private final LongAdder foreignSpans = new LongAdder();
    private final LongAdder droppedSpans = new LongAdder();
    private GraphWindow window;
    private volatile int activeNodes;
    private volatile int activeEdges;

    public GraphBuilder(String environment, EdgeBuilder edges, InstantSource clock) {
        this.environment = Objects.requireNonNull(environment, "environment");
        this.edges = Objects.requireNonNull(edges, "edges");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.window = new GraphWindow(clock.instant());
    }

    public void onSpan(SpanRecord span) {
        if (!environment.equals(span.service().environment())) {
            foreignSpans.increment();
            return;
        }
        window.recordService(span);
        edges.onSpan(span).forEach(window::observe);
        publishSizes();
    }

    public void sweep(Instant now) {
        EdgeBuilder.Expiry expiry = edges.expire(now);
        expiry.externalEdges().forEach(window::observe);
        droppedSpans.add(expiry.droppedSpans());
        publishSizes();
    }

    public GraphSnapshot freeze() {
        Instant now = clock.instant();
        GraphSnapshot snapshot = window.freeze(now);
        window = new GraphWindow(now);
        publishSizes();
        return snapshot;
    }

    public long foreignSpans() {
        return foreignSpans.sum();
    }

    public long droppedSpans() {
        return droppedSpans.sum();
    }

    public int activeNodes() {
        return activeNodes;
    }

    public int activeEdges() {
        return activeEdges;
    }

    private void publishSizes() {
        activeNodes = window.nodeCount();
        activeEdges = window.edgeCount();
    }

    public int pendingSpans() {
        return edges.pendingSpans();
    }
}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.metrics;

import io.github.architrace.graph.GraphBuilder;
import io.github.architrace.otlp.SpanReceiver;
import io.github.architrace.pipeline.SpanQueue;
import io.github.architrace.publish.PublisherStats;
import io.github.architrace.publish.SnapshotQueue;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

public record AgentMetrics(
        SpanReceiver receiver, SpanQueue spans, GraphBuilder graph, SnapshotQueue snapshots, PublisherStats publisher) {

    public static final String PREFIX = "architrace.agent.";

    public AgentMetrics {
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(spans, "spans");
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(snapshots, "snapshots");
        Objects.requireNonNull(publisher, "publisher");
    }

    public void bindTo(MeterRegistry registry) {
        counter(registry, "spans.received", receiver, SpanReceiver::receivedSpans);
        counter(registry, "spans.rejected", spans, SpanQueue::rejected);
        counter(registry, "spans.foreign", graph, GraphBuilder::foreignSpans);
        counter(registry, "spans.evicted", graph, GraphBuilder::droppedSpans);
        gauge(registry, "spans.queued", spans, SpanQueue::size);
        gauge(registry, "spans.pending", graph, GraphBuilder::pendingSpans);
        gauge(registry, "nodes.active", graph, GraphBuilder::activeNodes);
        gauge(registry, "edges.active", graph, GraphBuilder::activeEdges);
        counter(registry, "snapshots.published", publisher, PublisherStats::publishedCount);
        counter(registry, "snapshots.acknowledged", publisher, PublisherStats::acknowledgedCount);
        counter(registry, "snapshots.rejected", publisher, PublisherStats::rejectedCount);
        counter(registry, "snapshots.dropped", snapshots, SnapshotQueue::dropped);
        gauge(registry, "snapshots.queued", snapshots, SnapshotQueue::size);
        counter(registry, "controlplane.sessions.ended", publisher, PublisherStats::endedSessions);
        gauge(registry, "controlplane.connected", publisher, stats -> stats.isConnected() ? 1 : 0);
    }

    private static <T> void counter(MeterRegistry registry, String name, T source, ToDoubleFunction<T> value) {
        FunctionCounter.builder(PREFIX + name, source, value).register(registry);
    }

    private static <T> void gauge(MeterRegistry registry, String name, T source, ToDoubleFunction<T> value) {
        Gauge.builder(PREFIX + name, source, value).register(registry);
    }
}

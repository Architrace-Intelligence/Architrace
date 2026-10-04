/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.graph.EdgeBuilder;
import io.github.architrace.graph.GraphBuilder;
import io.github.architrace.graph.PendingSpanIndex;
import io.github.architrace.otlp.SpanReceiver;
import io.github.architrace.pipeline.SpanQueue;
import io.github.architrace.publish.PublisherStats;
import io.github.architrace.publish.SnapshotQueue;
import io.github.architrace.span.AttributeMapping;
import io.github.architrace.span.Peer;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanNormaliser;
import io.github.architrace.testsupport.MutableClock;
import io.github.architrace.testsupport.OtlpRequests;
import io.github.architrace.testsupport.TestSnapshots;
import io.github.architrace.testsupport.TestSpans;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.proto.trace.v1.Span;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentMetricsTest {

    private final MutableClock clock = MutableClock.startingAt("2026-10-04T10:00:00Z");
    private final SpanQueue spans = new SpanQueue(1);
    private final SpanReceiver receiver =
            new SpanReceiver(new SpanNormaliser(AttributeMapping.defaults(), "DEV", "local"), spans);
    private final GraphBuilder graph = new GraphBuilder(
            TestSpans.ENVIRONMENT, new EdgeBuilder(new PendingSpanIndex(Duration.ofSeconds(120)), clock), clock);
    private final SnapshotQueue snapshots = new SnapshotQueue(1);
    private final PublisherStats publisher = new PublisherStats();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AgentMetrics sut = new AgentMetrics(receiver, spans, graph, snapshots, publisher);

    @Test
    void metersReflectTheCountersAndSizesOfThePipeline() {
        sut.bindTo(registry);
        receiver.receive(OtlpRequests.request(
                OtlpRequests.standardResource("checkout"),
                OtlpRequests.span(Span.SpanKind.SPAN_KIND_SERVER, OtlpRequests.TRACE_ID, OtlpRequests.SERVER_SPAN_ID),
                OtlpRequests.span(Span.SpanKind.SPAN_KIND_SERVER, OtlpRequests.TRACE_ID, OtlpRequests.CLIENT_SPAN_ID)));
        graph.onSpan(TestSpans.withPeer(
                SpanKind.CLIENT, "t", "c1", "checkout", new Peer.Http("api.example.com", Optional.empty())));
        graph.onSpan(TestSpans.span(SpanKind.SERVER, "t", "s1", Optional.empty(), "orders", new Peer.None(), "PROD"));
        snapshots.offer(TestSnapshots.full());
        snapshots.offer(TestSnapshots.full());
        publisher.published();
        publisher.acknowledged();
        publisher.rejected();
        publisher.sessionEnded();
        publisher.connected(true);

        assertThat(counter("spans.received")).isEqualTo(2);
        assertThat(counter("spans.rejected")).isEqualTo(1);
        assertThat(gauge("spans.queued")).isEqualTo(1);
        assertThat(counter("spans.foreign")).isEqualTo(1);
        assertThat(gauge("spans.pending")).isEqualTo(1);
        assertThat(gauge("nodes.active")).isEqualTo(1);
        assertThat(gauge("edges.active")).isZero();
        assertThat(counter("snapshots.published")).isEqualTo(1);
        assertThat(counter("snapshots.acknowledged")).isEqualTo(1);
        assertThat(counter("snapshots.rejected")).isEqualTo(1);
        assertThat(counter("snapshots.dropped")).isEqualTo(1);
        assertThat(gauge("snapshots.queued")).isEqualTo(1);
        assertThat(counter("controlplane.sessions.ended")).isEqualTo(1);
        assertThat(gauge("controlplane.connected")).isEqualTo(1);

        clock.advance(Duration.ofSeconds(120));
        graph.sweep(clock.instant());
        publisher.connected(false);

        assertThat(gauge("spans.pending")).isZero();
        assertThat(gauge("edges.active")).isEqualTo(1);
        assertThat(gauge("nodes.active")).isEqualTo(2);
        assertThat(gauge("controlplane.connected")).isZero();
    }

    private double counter(String name) {
        return registry.get(AgentMetrics.PREFIX + name).functionCounter().count();
    }

    private double gauge(String name) {
        return registry.get(AgentMetrics.PREFIX + name).gauge().value();
    }
}

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
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanNormaliser;
import io.github.architrace.testsupport.MutableClock;
import io.github.architrace.testsupport.TestSnapshots;
import io.github.architrace.testsupport.TestSpans;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class DropReporterTest {

    private final MutableClock clock = MutableClock.startingAt("2026-10-04T10:00:00Z");
    private final SpanQueue spans = new SpanQueue(1);
    private final GraphBuilder graph = new GraphBuilder(
            TestSpans.ENVIRONMENT, new EdgeBuilder(new PendingSpanIndex(Duration.ofSeconds(120)), clock), clock);
    private final SnapshotQueue snapshots = new SnapshotQueue(1);
    private final PublisherStats publisher = new PublisherStats();
    private final DropReporter sut = new DropReporter(new AgentMetrics(
            new SpanReceiver(new SpanNormaliser(AttributeMapping.defaults(), "DEV", "local"), spans),
            spans,
            graph,
            snapshots,
            publisher));

    @Test
    void reportsOnlyTheDeltasSinceThePreviousReport() {
        assertThat(sut.report()).isEmpty();

        spans.offer(TestSpans.span(SpanKind.CLIENT, "t", "c1", "checkout"));
        spans.offer(TestSpans.span(SpanKind.CLIENT, "t", "c2", "checkout"));
        spans.offer(TestSpans.span(SpanKind.CLIENT, "t", "c3", "checkout"));
        snapshots.offer(TestSnapshots.full());
        snapshots.offer(TestSnapshots.full());
        publisher.rejected();

        assertThat(sut.report())
                .contains("Since the last report: 2 spans rejected by the full queue, "
                        + "1 snapshots dropped by the full queue, 1 snapshots rejected by the control plane");
        assertThat(sut.report()).isEmpty();

        graph.onSpan(TestSpans.span(SpanKind.CLIENT, "t", "c4", "checkout"));
        clock.advance(Duration.ofSeconds(120));
        graph.sweep(clock.instant());

        assertThat(sut.report()).contains("Since the last report: 1 spans evicted without a partner");
    }
}

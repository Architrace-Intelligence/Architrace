/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.graph.GraphNode.ExternalNode;
import io.github.architrace.graph.GraphNode.ServiceNode;
import io.github.architrace.span.Peer;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;
import io.github.architrace.testsupport.MutableClock;
import io.github.architrace.testsupport.TestSpans;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GraphBuilderTest {

    private final MutableClock clock = MutableClock.startingAt("2026-10-04T10:00:00Z");
    private final GraphBuilder sut = new GraphBuilder(
            TestSpans.ENVIRONMENT, new EdgeBuilder(new PendingSpanIndex(Duration.ofSeconds(120)), clock), clock);

    @Test
    void spansOfAnotherEnvironmentAreCountedAndIgnored() {
        SpanRecord foreign =
                TestSpans.span(SpanKind.SERVER, "t", "s1", Optional.empty(), "checkout", new Peer.None(), "PROD");

        sut.onSpan(foreign);
        sut.onSpan(TestSpans.span(SpanKind.SERVER, "t", "s2", "orders"));

        assertThat(sut.foreignSpans()).isEqualTo(1);
        assertThat(sut.freeze().nodes())
                .extracting(SnapshotNode::node)
                .containsExactly(new ServiceNode("default", "orders"));
    }

    @Test
    void freezeRotatesTheWindowAtTheClockInstant() {
        sut.onSpan(TestSpans.span(SpanKind.SERVER, "t", "s1", "orders"));
        clock.advance(Duration.ofSeconds(60));

        GraphSnapshot first = sut.freeze();
        GraphSnapshot second = sut.freeze();

        assertThat(first.windowStart()).isEqualTo(clock.instant().minusSeconds(60));
        assertThat(first.windowEnd()).isEqualTo(clock.instant());
        assertThat(first.nodes()).hasSize(1);
        assertThat(second.windowStart()).isEqualTo(clock.instant());
        assertThat(second.isEmpty()).isTrue();
    }

    @Test
    void sweepTurnsExpiredHttpClientsIntoExternalEdgesAndCountsDrops() {
        SpanRecord http = TestSpans.withPeer(
                SpanKind.CLIENT, "t", "c1", "checkout", new Peer.Http("api.example.com", Optional.empty()));
        SpanRecord plain = TestSpans.withPeer(SpanKind.CLIENT, "t", "c2", "checkout", new Peer.None());
        sut.onSpan(http);
        sut.onSpan(plain);
        assertThat(sut.pendingSpans()).isEqualTo(2);

        sut.sweep(clock.instant().plusSeconds(120));
        GraphSnapshot snapshot = sut.freeze();

        assertThat(snapshot.edges())
                .extracting(SnapshotEdge::key)
                .containsExactly(new EdgeKey(
                        new ServiceNode("default", "checkout"), new ExternalNode("api.example.com"), EdgeKind.SYNC));
        assertThat(sut.droppedSpans()).isEqualTo(1);
        assertThat(sut.pendingSpans()).isZero();
    }
}

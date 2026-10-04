/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.architrace.graph.EdgeBuilder;
import io.github.architrace.graph.EdgeKey;
import io.github.architrace.graph.EdgeKind;
import io.github.architrace.graph.GraphBuilder;
import io.github.architrace.graph.GraphNode.ExternalNode;
import io.github.architrace.graph.GraphNode.ServiceNode;
import io.github.architrace.graph.GraphSnapshot;
import io.github.architrace.graph.PendingSpanIndex;
import io.github.architrace.graph.SnapshotEdge;
import io.github.architrace.span.Peer;
import io.github.architrace.span.SpanKind;
import io.github.architrace.testsupport.MutableClock;
import io.github.architrace.testsupport.TestSpans;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GraphWorkerTest {

    private static final Duration SWEEP_INTERVAL = Duration.ofSeconds(10);

    private final MutableClock clock = MutableClock.startingAt("2026-10-04T10:00:00Z");
    private final SpanQueue queue = new SpanQueue(64);
    private final GraphBuilder builder = new GraphBuilder(
            TestSpans.ENVIRONMENT, new EdgeBuilder(new PendingSpanIndex(Duration.ofSeconds(120)), clock), clock);
    private final GraphWorker sut = new GraphWorker(queue, builder, clock, SWEEP_INTERVAL);
    private final Thread worker = Thread.ofVirtual().unstarted(() -> {
        try {
            sut.run();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    });

    @AfterEach
    void stopWorker() throws InterruptedException {
        worker.interrupt();
        worker.join(2_000);
        assertThat(worker.isAlive()).isFalse();
    }

    @Test
    void workerDrainsTheQueueAndServesFreezeRequestsOnItsOwnThread() throws Exception {
        worker.start();
        queue.offerAll(List.of(
                TestSpans.span(SpanKind.CLIENT, "t", "c1", "checkout"),
                TestSpans.child(SpanKind.SERVER, "t", "s1", "c1", "orders")));

        GraphSnapshot snapshot = sut.requestFreeze().get(5, TimeUnit.SECONDS);

        assertThat(snapshot.edges())
                .extracting(SnapshotEdge::key)
                .containsExactly(new EdgeKey(
                        new ServiceNode("default", "checkout"), new ServiceNode("default", "orders"), EdgeKind.SYNC));
        assertThat(queue.size()).isZero();
    }

    @Test
    void concurrentFreezeRequestsShareOneSnapshot() throws Exception {
        worker.start();

        var first = sut.requestFreeze();
        var second = sut.requestFreeze();

        assertThat(second).isSameAs(first);
        assertThat(first.get(5, TimeUnit.SECONDS).isEmpty()).isTrue();
    }

    @Test
    void workerSweepsThePendingIndexWhenTheIntervalElapses() throws Exception {
        worker.start();
        queue.offer(TestSpans.withPeer(
                SpanKind.CLIENT, "t", "c1", "checkout", new Peer.Http("api.example.com", Optional.empty())));
        await().atMost(Duration.ofSeconds(5)).until(() -> builder.pendingSpans() == 1);

        clock.advance(Duration.ofSeconds(120));
        await().atMost(Duration.ofSeconds(5)).until(() -> builder.pendingSpans() == 0);
        GraphSnapshot snapshot = sut.requestFreeze().get(5, TimeUnit.SECONDS);

        assertThat(snapshot.edges())
                .extracting(SnapshotEdge::key)
                .containsExactly(new EdgeKey(
                        new ServiceNode("default", "checkout"), new ExternalNode("api.example.com"), EdgeKind.SYNC));
    }
}

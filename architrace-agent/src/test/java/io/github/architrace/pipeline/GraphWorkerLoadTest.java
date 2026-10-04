/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.graph.EdgeBuilder;
import io.github.architrace.graph.GraphBuilder;
import io.github.architrace.graph.GraphSnapshot;
import io.github.architrace.graph.PendingSpanIndex;
import io.github.architrace.span.Peer;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;
import io.github.architrace.testsupport.TestSpans;
import java.time.Duration;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("load")
class GraphWorkerLoadTest {

    private static final int SPANS_PER_SECOND = 10_000;
    private static final int SERVICES = 50;
    private static final int BATCH = 100;
    private static final long MAX_HEAP_BYTES = 512L * 1024 * 1024;

    @Test
    void sustainsTenThousandSpansPerSecondWithBoundedMemory() throws Exception {
        Duration duration = Duration.ofSeconds(Long.parseLong(System.getProperty("load.seconds", "10")));
        InstantSource clock = InstantSource.system();
        SpanQueue queue = new SpanQueue(65_536);
        GraphBuilder builder = new GraphBuilder(
                TestSpans.ENVIRONMENT, new EdgeBuilder(new PendingSpanIndex(Duration.ofSeconds(120)), clock), clock);
        GraphWorker worker = new GraphWorker(queue, builder, clock, Duration.ofSeconds(10));
        Thread workerThread = Thread.ofPlatform().start(() -> runQuietly(worker));
        long deadline = System.nanoTime() + duration.toNanos();
        long produced = 0;
        long traces = 0;
        long nextBatchAt = System.nanoTime();
        long batchIntervalNanos = TimeUnit.SECONDS.toNanos(1) * BATCH / SPANS_PER_SECOND;
        List<GraphSnapshot> snapshots = new ArrayList<>();
        long nextFreeze = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (System.nanoTime() < deadline) {
            queue.offerAll(tracePairs(traces, BATCH / 2));
            traces += BATCH / 2;
            produced += BATCH;
            nextBatchAt += batchIntervalNanos;
            if (System.nanoTime() >= nextFreeze) {
                snapshots.add(worker.requestFreeze().get(5, TimeUnit.SECONDS));
                nextFreeze += TimeUnit.SECONDS.toNanos(1);
            }
            LockSupport.parkNanos(nextBatchAt - System.nanoTime());
        }
        snapshots.add(worker.requestFreeze().get(5, TimeUnit.SECONDS));
        workerThread.interrupt();
        workerThread.join(5_000);
        System.gc();
        long usedHeap =
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long edges = snapshots.stream()
                .mapToLong(snapshot -> snapshot.edges().size())
                .sum();
        System.out.printf(
                "load: %d spans in %ds, %d rejected, %d pending, %d snapshots, %d edges, heap %d MB%n",
                produced,
                duration.toSeconds(),
                queue.rejected(),
                builder.pendingSpans(),
                snapshots.size(),
                edges,
                usedHeap / (1024 * 1024));

        assertThat(queue.rejected()).isZero();
        assertThat(builder.pendingSpans()).isZero();
        assertThat(edges).isPositive();
        assertThat(usedHeap).isLessThan(MAX_HEAP_BYTES);
    }

    private static List<SpanRecord> tracePairs(long firstTrace, int pairs) {
        return IntStream.range(0, pairs)
                .mapToObj(offset -> firstTrace + offset)
                .flatMap(trace -> {
                    String traceId = Long.toHexString(trace);
                    String caller = "service-" + (trace % SERVICES);
                    String callee = "service-" + ((trace + 1) % SERVICES);
                    SpanRecord client = TestSpans.withPeer(
                            SpanKind.CLIENT, traceId, "c", caller, new Peer.Http(callee, Optional.empty()));
                    SpanRecord server = TestSpans.child(SpanKind.SERVER, traceId, "s", "c", callee);
                    return java.util.stream.Stream.of(client, server);
                })
                .toList();
    }

    private static void runQuietly(GraphWorker worker) {
        try {
            worker.run();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}

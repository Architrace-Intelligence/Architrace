/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.pipeline;

import io.github.architrace.graph.GraphBuilder;
import io.github.architrace.graph.GraphSnapshot;
import io.github.architrace.span.SpanRecord;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class GraphWorker {

    private static final int BATCH_SIZE = 512;
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(50);

    private final SpanQueue queue;
    private final GraphBuilder builder;
    private final InstantSource clock;
    private final Duration sweepInterval;
    private final AtomicReference<CompletableFuture<GraphSnapshot>> freezeRequest = new AtomicReference<>();
    private final List<SpanRecord> batch = new ArrayList<>(BATCH_SIZE);

    public GraphWorker(SpanQueue queue, GraphBuilder builder, InstantSource clock, Duration sweepInterval) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.builder = Objects.requireNonNull(builder, "builder");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sweepInterval = Objects.requireNonNull(sweepInterval, "sweepInterval");
    }

    public Void run() throws InterruptedException {
        Instant nextSweep = clock.instant().plus(sweepInterval);
        while (!Thread.currentThread().isInterrupted()) {
            drain();
            serveFreezeRequest();
            nextSweep = sweepIfDue(nextSweep);
        }
        return null;
    }

    public CompletableFuture<GraphSnapshot> requestFreeze() {
        CompletableFuture<GraphSnapshot> fresh = new CompletableFuture<>();
        CompletableFuture<GraphSnapshot> inFlight = freezeRequest.compareAndExchange(null, fresh);
        return inFlight == null ? fresh : inFlight;
    }

    private void drain() throws InterruptedException {
        SpanRecord first = queue.poll(POLL_TIMEOUT);
        if (first == null) {
            return;
        }
        builder.onSpan(first);
        queue.drainTo(batch, BATCH_SIZE - 1);
        batch.forEach(builder::onSpan);
        batch.clear();
    }

    private void serveFreezeRequest() {
        CompletableFuture<GraphSnapshot> request = freezeRequest.getAndSet(null);
        if (request != null) {
            request.complete(builder.freeze());
        }
    }

    private Instant sweepIfDue(Instant nextSweep) {
        Instant now = clock.instant();
        if (now.isBefore(nextSweep)) {
            return nextSweep;
        }
        builder.sweep(now);
        return now.plus(sweepInterval);
    }
}

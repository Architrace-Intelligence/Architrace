/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.publish;

import io.github.architrace.graph.GraphSnapshot;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public final class SnapshotQueue {

    private final LinkedBlockingDeque<GraphSnapshot> queue;
    private final LongAdder dropped = new LongAdder();

    public SnapshotQueue(int capacity) {
        this.queue = new LinkedBlockingDeque<>(capacity);
    }

    public void offer(GraphSnapshot snapshot) {
        while (!queue.offerLast(snapshot)) {
            dropOldest();
        }
    }

    public void requeue(GraphSnapshot snapshot) {
        if (!queue.offerFirst(snapshot)) {
            dropped.increment();
        }
    }

    public GraphSnapshot poll(Duration timeout) throws InterruptedException {
        return queue.pollFirst(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    public int size() {
        return queue.size();
    }

    public long dropped() {
        return dropped.sum();
    }

    private void dropOldest() {
        if (queue.pollFirst() != null) {
            dropped.increment();
        }
    }
}

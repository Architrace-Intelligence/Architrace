/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.pipeline;

import io.github.architrace.span.SpanRecord;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public final class SpanQueue {

    private final ArrayBlockingQueue<SpanRecord> queue;
    private final LongAdder rejected = new LongAdder();

    public SpanQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    public boolean offer(SpanRecord span) {
        boolean accepted = queue.offer(span);
        if (!accepted) {
            rejected.increment();
        }
        return accepted;
    }

    public void offerAll(List<SpanRecord> spans) {
        spans.forEach(this::offer);
    }

    public SpanRecord poll(Duration timeout) throws InterruptedException {
        return queue.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    public int drainTo(Collection<? super SpanRecord> sink, int maxElements) {
        return queue.drainTo(sink, maxElements);
    }

    public int size() {
        return queue.size();
    }

    public long rejected() {
        return rejected.sum();
    }
}

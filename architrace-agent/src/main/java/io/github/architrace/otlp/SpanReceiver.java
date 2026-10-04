/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.otlp;

import io.github.architrace.pipeline.SpanQueue;
import io.github.architrace.span.SpanNormaliser;
import io.github.architrace.span.SpanRecord;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

public class SpanReceiver {

    private final SpanNormaliser normaliser;
    private final SpanQueue queue;
    private final LongAdder received = new LongAdder();

    public SpanReceiver(SpanNormaliser normaliser, SpanQueue queue) {
        this.normaliser = Objects.requireNonNull(normaliser, "normaliser");
        this.queue = Objects.requireNonNull(queue, "queue");
    }

    public void receive(ExportTraceServiceRequest request) {
        List<SpanRecord> spans = normaliser.normalise(request);
        received.add(spans.size());
        queue.offerAll(spans);
    }

    public long receivedSpans() {
        return received.sum();
    }
}

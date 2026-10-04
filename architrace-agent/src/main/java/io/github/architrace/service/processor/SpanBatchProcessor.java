/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.service.processor;

import io.github.architrace.otlp.SpanPipeline;
import io.github.architrace.otlp.SpanRingBuffer;
import io.github.architrace.span.SpanRecord;
import java.util.ArrayList;
import java.util.List;

public class SpanBatchProcessor {

    private static final int BATCH_SIZE = 512;

    private final SpanRingBuffer ringBuffer;
    private final SpanPipeline pipeline;

    public SpanBatchProcessor(SpanRingBuffer ringBuffer, SpanPipeline pipeline) {
        this.ringBuffer = ringBuffer;
        this.pipeline = pipeline;
    }

    public void submit(List<SpanRecord> spans) {
        spans.forEach(ringBuffer::publish);
    }

    public Void run() {
        List<SpanRecord> batch = new ArrayList<>(BATCH_SIZE);
        while (!Thread.currentThread().isInterrupted()) {
            SpanRecord span;
            while ((span = ringBuffer.poll()) != null) {
                batch.add(span);
                if (batch.size() == BATCH_SIZE) {
                    pipeline.process(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                pipeline.process(batch);
                batch.clear();
            }
            Thread.onSpinWait();
        }
        return null;
    }
}

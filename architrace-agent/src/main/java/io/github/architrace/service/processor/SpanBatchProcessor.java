/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.processor;

import io.github.architrace.model.InternalSpan;
import io.github.architrace.otlp.SpanPipeline;
import io.github.architrace.otlp.SpanRingBuffer;
import java.util.ArrayList;
import java.util.List;

public class SpanBatchProcessor {

  private static final int INITIAL_CAPACITY = 512;

  private final SpanRingBuffer ringBuffer;
  private final SpanPipeline pipeline;

  public SpanBatchProcessor(SpanRingBuffer ringBuffer, SpanPipeline pipeline) {
    this.ringBuffer = ringBuffer;
    this.pipeline = pipeline;
  }

  public void submit(List<InternalSpan> spans) {
    for (InternalSpan span : spans) {
      ringBuffer.publish(span);
    }
  }

  public Void run() {
    List<InternalSpan> batch = new ArrayList<>(INITIAL_CAPACITY);
    while (!Thread.currentThread().isInterrupted()) {
      InternalSpan span;
      while ((span = ringBuffer.poll()) != null) {
        batch.add(span);
        if (batch.size() == INITIAL_CAPACITY) {
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

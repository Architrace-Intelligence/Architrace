/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import io.github.architrace.model.InternalSpan;
import io.github.architrace.service.processor.SpanProcessor;
import java.util.List;

public class SpanPipeline {

  private final List<SpanProcessor> spanProcessors;

  public SpanPipeline(List<SpanProcessor> spanProcessors) {
    this.spanProcessors = spanProcessors;
  }

  public void process(List<InternalSpan> spans) {
    for (SpanProcessor spanProcessor : spanProcessors) {
      for (InternalSpan span : spans) {
        spanProcessor.onSpan(span);
      }
    }
  }
}
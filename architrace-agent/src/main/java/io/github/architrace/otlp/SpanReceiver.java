/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.otlp;

import io.github.architrace.service.graph.SpanExtractor;
import io.github.architrace.model.InternalSpan;
import io.github.architrace.service.processor.SpanBatchProcessor;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.util.List;

public class SpanReceiver {

  private final SpanExtractor extractor;
  private final SpanBatchProcessor batchProcessor;

  public SpanReceiver(SpanExtractor extractor, SpanBatchProcessor batchProcessor) {
    this.extractor = extractor;
    this.batchProcessor = batchProcessor;
  }

  public void receive(ExportTraceServiceRequest request) {
    List<InternalSpan> spans = extractor.extract(request);
    batchProcessor.submit(spans);
  }

}
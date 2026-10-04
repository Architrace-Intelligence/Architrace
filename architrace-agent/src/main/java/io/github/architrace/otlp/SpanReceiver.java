/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.otlp;

import io.github.architrace.service.processor.SpanBatchProcessor;
import io.github.architrace.span.SpanNormaliser;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;

public class SpanReceiver {

    private final SpanNormaliser normaliser;
    private final SpanBatchProcessor batchProcessor;

    public SpanReceiver(SpanNormaliser normaliser, SpanBatchProcessor batchProcessor) {
        this.normaliser = normaliser;
        this.batchProcessor = batchProcessor;
    }

    public void receive(ExportTraceServiceRequest request) {
        batchProcessor.submit(normaliser.normalise(request));
    }
}

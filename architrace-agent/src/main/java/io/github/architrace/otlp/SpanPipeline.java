/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.otlp;

import io.github.architrace.service.processor.SpanProcessor;
import io.github.architrace.span.SpanRecord;
import java.util.List;

public class SpanPipeline {

    private final List<SpanProcessor> spanProcessors;

    public SpanPipeline(List<SpanProcessor> spanProcessors) {
        this.spanProcessors = List.copyOf(spanProcessors);
    }

    public void process(List<SpanRecord> spans) {
        spanProcessors.forEach(processor -> spans.forEach(processor::onSpan));
    }
}

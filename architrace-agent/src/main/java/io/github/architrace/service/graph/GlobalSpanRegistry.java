/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.service.graph;

import io.github.architrace.model.TraceSpanKey;
import io.github.architrace.span.SpanRecord;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GlobalSpanRegistry {

    private final ConcurrentMap<TraceSpanKey, SpanRecord> spanIndex = new ConcurrentHashMap<>();

    public boolean registerIfAbsent(SpanRecord span) {
        return spanIndex.putIfAbsent(new TraceSpanKey(span.traceId(), span.spanId()), span) == null;
    }

    public Optional<SpanRecord> findParent(SpanRecord span) {
        return span.parentSpanId().map(parentSpanId -> spanIndex.get(new TraceSpanKey(span.traceId(), parentSpanId)));
    }
}

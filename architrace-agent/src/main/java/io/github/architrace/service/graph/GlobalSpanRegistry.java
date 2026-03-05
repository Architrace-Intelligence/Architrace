/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.graph;

import io.github.architrace.model.InternalSpan;
import io.github.architrace.model.TraceSpanKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GlobalSpanRegistry {

  private final ConcurrentMap<TraceSpanKey, InternalSpan> spanIndex = new ConcurrentHashMap<>();

  public boolean registerIfAbsent(InternalSpan span){
    var key = new TraceSpanKey(span.traceId(), span.spanId());

    return spanIndex.putIfAbsent(key, span) == null;
  }

  public void register(InternalSpan span) {
    if (span.spanId() == null || span.traceId() == null) {
      return;
    }

    var key = new TraceSpanKey(span.traceId(), span.spanId());
    spanIndex.put(key, span);
  }

  public InternalSpan findParent(InternalSpan span) {
    if (span.parentSpanId() == null) {
      return null;
    }

    var parentKey = new TraceSpanKey(span.traceId(), span.parentSpanId());

    return spanIndex.get(parentKey);
  }

  public ConcurrentMap<TraceSpanKey, InternalSpan> getSpanIndex() {
    return spanIndex;
  }
}
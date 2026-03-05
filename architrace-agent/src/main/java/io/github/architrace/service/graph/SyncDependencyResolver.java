/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.graph;

import io.github.architrace.model.InternalSpan;
import io.github.architrace.model.SpanKind;
import io.github.architrace.model.TraceParentKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SyncDependencyResolver extends AbstractDependencyResolver {

  private final GlobalSpanRegistry registry;
  // SERVER spans that arrived before their CLIENT parent.
  private final ConcurrentMap<TraceParentKey, InternalSpan> waitingServers = new ConcurrentHashMap<>();

  public SyncDependencyResolver(GlobalSpanRegistry registry) {
    this.registry = registry;
  }

  public void onSpan(InternalSpan span) {
    boolean isNew = registry.registerIfAbsent(span);
    if (!isNew) {
      return;
    }

    if (span.kind() == SpanKind.SERVER) {
      handleServer(span);
    }

    if (span.kind() == SpanKind.CLIENT) {
      handleClient(span);
    }
  }

  private void handleServer(InternalSpan server) {
    InternalSpan parent = registry.findParent(server);

    if (parent != null && parent.kind() == SpanKind.CLIENT) {
      buildDependency(parent, server);
    } else {
      TraceParentKey key = new TraceParentKey(server.traceId(), server.parentSpanId());
      waitingServers.put(key, server);
    }
  }

  private void handleClient(InternalSpan client) {
    TraceParentKey key = new TraceParentKey(client.traceId(), client.spanId());
    InternalSpan waitingServer = waitingServers.remove(key);

    if (waitingServer != null) {
      buildDependency(client, waitingServer);
    }
  }
}

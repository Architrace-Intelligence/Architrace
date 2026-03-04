/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.graph;

import io.github.architrace.model.EdgeKey;
import io.github.architrace.model.EdgeMetrics;
import io.github.architrace.model.InternalSpan;
import io.github.architrace.model.LogicalServiceId;
import io.github.architrace.model.SpanKind;
import io.github.architrace.model.TraceParentKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SyncDependencyResolver {

  private final GlobalSpanRegistry registry;

  // SERVER которые пришли раньше CLIENT
  private final ConcurrentMap<TraceParentKey, InternalSpan> waitingServers = new ConcurrentHashMap<>();
  private final ConcurrentMap<EdgeKey, EdgeMetrics> edges = new ConcurrentHashMap<>();

  public SyncDependencyResolver(GlobalSpanRegistry registry) {
    this.registry = registry;
  }

  private void handleServer(InternalSpan server) {
    InternalSpan parent = registry.findParent(server);

    if (parent != null && parent.kind() == SpanKind.CLIENT) {
      buildDependency(parent, server);
    } else {
      // CLIENT ещё не пришёл — сохраняем ожидание
      TraceParentKey key = new TraceParentKey(
          server.traceId(),
          server.parentSpanId()
      );
      waitingServers.put(key, server);
    }
  }

  private void handleClient(InternalSpan client) {

    TraceParentKey key = new TraceParentKey(
        client.traceId(),
        client.spanId()
    );

    InternalSpan waitingServer = waitingServers.remove(key);

    if (waitingServer != null) {
      buildDependency(client, waitingServer);
    }
  }

  public void onSpan(InternalSpan span) {

    boolean isNew = registry.registerIfAbsent(span);

    if (!isNew) {
      return; // дубликат — ничего не делаем
    }

    if (span.kind() == SpanKind.SERVER) {
      handleServer(span);
    }

    if (span.kind() == SpanKind.CLIENT) {
      handleClient(span);
    }
  }

  private void buildDependency(InternalSpan client, InternalSpan server) {

    LogicalServiceId from = client.logicalServiceId();
    LogicalServiceId to = server.logicalServiceId();

    // 1. Проверка environment
    if (!from.environment().equals(to.environment())) {
      return;
    }

    // 2. Не строим self-loop
    if (from.equals(to)) {
      return;
    }

    EdgeKey key = new EdgeKey(
        from.asString(),
        to.asString()
    );

    edges.compute(key, (k, metrics) -> {
      if (metrics == null) {
        metrics = new EdgeMetrics();
      }
      metrics.incrementCallCount();
      return metrics;
    });
  }
}
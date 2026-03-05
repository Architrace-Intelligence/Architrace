/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.graph;

import io.github.architrace.model.EdgeKey;
import io.github.architrace.model.EdgeMetrics;
import io.github.architrace.model.InternalSpan;
import io.github.architrace.model.LogicalServiceId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractDependencyResolver {

  private final ConcurrentMap<EdgeKey, EdgeMetrics> edges = new ConcurrentHashMap<>();

  protected final void buildDependency(InternalSpan fromSpan, InternalSpan toSpan) {
    LogicalServiceId from = fromSpan.logicalServiceId();
    LogicalServiceId to = toSpan.logicalServiceId();

    if (!from.environment().equals(to.environment())) {
      return;
    }

    if (from.equals(to)) {
      return;
    }

    EdgeKey key = new EdgeKey(from.asString(), to.asString());
    edges.compute(key, (k, metrics) -> {
      if (metrics == null) {
        metrics = new EdgeMetrics();
      }

      return metrics;
    });
  }

  public ConcurrentMap<EdgeKey, EdgeMetrics> getEdges() {
    return edges;
  }
}

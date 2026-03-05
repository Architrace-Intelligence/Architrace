/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.snapshot;

import io.github.architrace.service.graph.AsyncDependencyResolver;
import io.github.architrace.service.graph.NodeRegistry;
import io.github.architrace.service.graph.SyncDependencyResolver;
import io.github.architrace.model.EdgeKey;
import io.github.architrace.model.EdgeMetrics;
import io.github.architrace.otlp.GraphNode;
import io.github.architrace.otlp.GraphSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GraphSnapshotService {

  private final NodeRegistry nodeRegistry;
  private final SyncDependencyResolver syncResolver;
  private final AsyncDependencyResolver asyncResolver;

  public GraphSnapshotService(NodeRegistry nodeRegistry, SyncDependencyResolver syncResolver,
                              AsyncDependencyResolver asyncResolver) {
    this.nodeRegistry = nodeRegistry;
    this.syncResolver = syncResolver;
    this.asyncResolver = asyncResolver;
  }

  public GraphSnapshot snapshot() {
    Set<GraphNode> nodes = nodeRegistry.getNodes();
    Map<EdgeKey, EdgeMetrics> edges = new HashMap<>();
    merge(edges, syncResolver.getEdges());
    merge(edges, asyncResolver.getEdges());

    return new GraphSnapshot(nodes, edges);
  }

  private void merge( Map<EdgeKey, EdgeMetrics> target, Map<EdgeKey, EdgeMetrics> source) {
    source.forEach((k, v) -> target.merge(k, v, EdgeMetrics::merge));
  }

}
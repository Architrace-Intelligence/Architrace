package io.github.architrace.otlp;

import io.github.architrace.model.EdgeMetrics;
import io.github.architrace.service.graph.AbstractDependencyResolver;
import io.github.architrace.service.graph.NodeRegistry;

public class GraphAggregator {

  private final NodeRegistry nodeRegistry;
  private final AbstractDependencyResolver resolver;

  public GraphAggregator(NodeRegistry nodeRegistry,
                         AbstractDependencyResolver resolver) {
    this.nodeRegistry = nodeRegistry;
    this.resolver = resolver;
  }

  public GraphSnapshot snapshotAndReset() {

    var nodes = nodeRegistry.getNodes();
    var edges = resolver.getEdges();

    GraphSnapshot snapshot = new GraphSnapshot(nodes, edges);

    edges.values().forEach(EdgeMetrics::reset);

    return snapshot;
  }
}
/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.model;

import io.github.architrace.grpc.proto.GraphBatch;
import io.github.architrace.grpc.proto.GraphEdge;
import io.github.architrace.grpc.proto.GraphNode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class GraphAggregator {

  private String agentName = "";
  private final Map<String, GraphNode> nodes = new LinkedHashMap<>();
  private final Map<String, Long> edgeCallCounts = new LinkedHashMap<>();

  public synchronized void add(GraphBatch delta) {
    if (!delta.getAgentName().isBlank()) {
      this.agentName = delta.getAgentName();
    }
    delta.getNodesList().forEach(node -> nodes.putIfAbsent(node.getId(), node));
    delta.getEdgesList().forEach(edge -> {
      String key = edge.getSourceNodeId() + "->" + edge.getTargetNodeId() + "@" + edge.getOperation();
      edgeCallCounts.merge(key, edge.getCallCount(), Long::sum);
    });
  }

  public synchronized Optional<GraphBatch> drain() {
    if (nodes.isEmpty() && edgeCallCounts.isEmpty()) {
      return Optional.empty();
    }

    GraphBatch.Builder batch =
        GraphBatch.newBuilder()
            .setAgentName(agentName)
            .setObservedAtEpochMs(Instant.now().toEpochMilli())
            .addAllNodes(nodes.values());

    edgeCallCounts.forEach((key, count) -> {
      String[] sourceAndRest = key.split("->", 2);
      String[] targetAndOp = sourceAndRest[1].split("@", 2);
      batch.addEdges(
          GraphEdge.newBuilder()
              .setSourceNodeId(sourceAndRest[0])
              .setTargetNodeId(targetAndOp[0])
              .setOperation(targetAndOp[1])
              .setCallCount(count)
              .build());
    });

    nodes.clear();
    edgeCallCounts.clear();

    return Optional.of(batch.build());
  }
}


/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.graph;

import io.github.architrace.grpc.proto.GraphBatch;
import io.github.architrace.grpc.proto.GraphEdge;
import io.github.architrace.grpc.proto.GraphNode;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class SpanToGraphConverter {

  public GraphBatch convert(ExportTraceServiceRequest request, String agentName) {
    Map<String, GraphNode> nodes = new LinkedHashMap<>();
    Map<String, Long> edges = new LinkedHashMap<>();

    request.getResourceSpansList().forEach(resourceSpans ->
      resourceSpans.getScopeSpansList().forEach(scopeSpans -> {
        scopeSpans.getSpansList().forEach(span -> {
          String serviceName = resourceSpans.getResource().getAttributesList().stream()
              .filter(a -> "service.name".equals(a.getKey()))
              .findFirst()
              .map(a -> a.getValue().getStringValue())
              .orElse("unknown-service");

          String sourceNodeId = "svc:" + serviceName;
          String targetNodeId = "op:" + span.getName();
          nodes.putIfAbsent(
              sourceNodeId,
              GraphNode.newBuilder().setId(sourceNodeId).setType("SERVICE").setName(serviceName).build());
          nodes.putIfAbsent(
              targetNodeId,
              GraphNode.newBuilder().setId(targetNodeId).setType("OPERATION").setName(span.getName()).build());

          String edgeKey = sourceNodeId + "->" + targetNodeId;
          edges.merge(edgeKey, 1L, Long::sum);
        });
      })
    );

    GraphBatch.Builder batch =
        GraphBatch.newBuilder()
            .setAgentName(agentName)
            .setObservedAtEpochMs(Instant.now().toEpochMilli())
            .addAllNodes(nodes.values());

    edges.forEach((edgeKey, callCount) -> {
      String[] parts = edgeKey.split("->", 2);
      batch.addEdges(
          GraphEdge.newBuilder()
              .setSourceNodeId(parts[0])
              .setTargetNodeId(parts[1])
              .setOperation("span")
              .setCallCount(callCount)
              .build());
    });

    return batch.build();
  }
}

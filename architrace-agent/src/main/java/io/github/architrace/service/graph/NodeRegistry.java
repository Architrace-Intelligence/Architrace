/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.graph;

import io.github.architrace.model.InternalSpan;
import io.github.architrace.otlp.GraphNode;
import io.github.architrace.otlp.NodeDescriptor;
import io.github.architrace.otlp.NodeExtractor;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NodeRegistry {

  private final ConcurrentMap<String, GraphNode> nodes = new ConcurrentHashMap<>();
  private final NodeExtractor extractor = new NodeExtractor();

  public void register(InternalSpan span) {
    NodeDescriptor descriptor = extractor.extract(span);

    nodes.computeIfAbsent(
        descriptor.id(),
        id -> new GraphNode(
            span.logicalServiceId(),
            descriptor.type(),
            descriptor.name()
        )
    );
  }

  public Set<GraphNode> getNodes() {
    return new HashSet<>(nodes.values());
  }

}
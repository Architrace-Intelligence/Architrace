/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeAggregator {

  public Map<LogicalServiceId, LogicalServiceNode> aggregate(List<InternalSpan> spans) {
    Map<LogicalServiceId, LogicalServiceNode> nodes = new HashMap<>();
    for (InternalSpan span : spans) {
      LogicalServiceId id = span.logicalServiceId();
//      LogicalServiceNode node = nodes.computeIfAbsent(
//          id,
//          LogicalServiceNode::new
//      );
//
//      node.registerDeployment(span.clusterId(), span.namespace());
    }

    return nodes;
  }
}
/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import io.github.architrace.model.LogicalServiceId;
import io.github.architrace.model.NodeType;

public class GraphNode {

  private final String id;
  private final NodeType type;
  private final String name;

  public GraphNode(LogicalServiceId logicalServiceId, NodeType type, String name) {
    this.id = logicalServiceId.asString();
    this.type = type;
    this.name = name;
  }

  public String id() {
    return id;
  }
}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import io.github.architrace.model.InternalSpan;
import io.github.architrace.model.NodeType;
import java.util.Optional;

public class NodeExtractor {

  public NodeDescriptor extract(InternalSpan span) {
    return switch (span) {
      case InternalSpan s when s.isDbSystem() -> databaseNode(s);
      case InternalSpan s when s.isMessaging() -> messagingNode(s);
      case InternalSpan s when s.isExternalService() -> externalNode(s);
      default -> serviceNode(span);
    };
  }

  private NodeDescriptor databaseNode(InternalSpan span) {
    return Optional.ofNullable(span.dbSystem())
        .map(db -> new NodeDescriptor("db:" + db, NodeType.DATABASE, db))
        .orElseGet(() -> serviceNode(span));
  }

  private NodeDescriptor messagingNode(InternalSpan span) {
    return Optional.ofNullable(span.messagingDestination())
        .map(topic -> new NodeDescriptor("topic:" + topic, NodeType.TOPIC, topic))
        .orElseGet(() -> serviceNode(span));
  }

  private NodeDescriptor externalNode(InternalSpan span) {
    return Optional.ofNullable(span.httpHost())
        .map(host -> new NodeDescriptor("ext:" + host, NodeType.EXTERNAL_SERVICE, host))
        .orElseGet(() -> serviceNode(span));
  }

  private NodeDescriptor serviceNode(InternalSpan span) {
    return Optional.ofNullable(span.logicalServiceId())
        .map(id -> new NodeDescriptor(id.asString(), NodeType.SERVICE, id.serviceName()))
        .orElseThrow(() -> new IllegalStateException("Cannot resolve node for span: " + span));
  }
}
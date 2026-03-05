/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.model;

public record InternalSpan(

    String traceId,
    String spanId,
    String parentSpanId,

    String environment,
    String domainId,
    String serviceName,
    String clusterId,
    String namespace,

    SpanKind kind,

    String httpHost,
    String httpMethod,

    String dbSystem,
    String dbName,

    String messagingSystem,
    String messagingDestination
) {
  public LogicalServiceId logicalServiceId() {
    return new LogicalServiceId(environment, domainId, serviceName);
  }

  public boolean isDbSystem(){
    return dbSystem != null;
  }

  public boolean isMessaging(){
    return messagingDestination != null;
  }

  public boolean isExternalService(){
    return httpHost != null && kind == SpanKind.CLIENT;
  }

  public SpanType spanType() {
    return switch (kind) {
      case CLIENT, SERVER -> SpanType.SYNC;
      case PRODUCER, CONSUMER -> SpanType.ASYNC;
      default -> SpanType.INTERNAL;
    };
  }
}

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

    SpanKind kind
) {
  public LogicalServiceId logicalServiceId() {
    return new LogicalServiceId(environment, domainId, serviceName);
  }
}

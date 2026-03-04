/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.graph;

import io.github.architrace.model.InternalSpan;
import io.github.architrace.model.SpanKind;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpanToGraphConverter {

  private static final ScopedValue<Map<String, String>> RESOURCE_ATTRS = ScopedValue.newInstance();

  public List<InternalSpan> extract(ExportTraceServiceRequest request) {
    return request.getResourceSpansList().stream()
        .flatMap(resourceSpans ->
            ScopedValue.where(RESOURCE_ATTRS, toAttributeMap(resourceSpans.getResource()))
                .call(() -> resourceSpans.getScopeSpansList().stream()
                    .flatMap(s -> s.getSpansList().stream())
                    .map(span -> mapSpan(span, RESOURCE_ATTRS.get())))
        )
        .toList();
  }

  private Map<String, String> toAttributeMap(io.opentelemetry.proto.resource.v1.Resource resource) {
    return resource.getAttributesList().stream()
        .collect(Collectors.toMap(KeyValue::getKey, attr -> attr.getValue().getStringValue(),
            (existing, duplicate) -> existing  // keep first, or use `duplicate` to keep last
        ));
  }

  private InternalSpan mapSpan(Span span, Map<String, String> resourceAttrs) {
    return new InternalSpan(
        span.getTraceId().toStringUtf8(),
        span.getSpanId().toStringUtf8(),
        span.getParentSpanId().toStringUtf8(),
        resourceAttrs.get("deployment.environment"),
        resourceAttrs.get("domain.id"),
        resourceAttrs.get("service.name"),
        resourceAttrs.get("k8s.cluster.name"),
        resourceAttrs.get("k8s.namespace.name"),
        mapKind(span.getKind()));
  }

  private SpanKind mapKind(Span.SpanKind otlpKind) {
    return switch (otlpKind) {
      case SPAN_KIND_CLIENT -> SpanKind.CLIENT;
      case SPAN_KIND_SERVER -> SpanKind.SERVER;
      case SPAN_KIND_PRODUCER -> SpanKind.PRODUCER;
      case SPAN_KIND_CONSUMER -> SpanKind.CONSUMER;
      case SPAN_KIND_INTERNAL -> SpanKind.INTERNAL;
      default -> SpanKind.INTERNAL;
    };
  }
}

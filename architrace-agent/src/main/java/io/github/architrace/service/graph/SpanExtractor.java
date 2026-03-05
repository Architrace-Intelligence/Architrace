/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.graph;

import static io.github.architrace.otlp.AttributeDictionary.DB_NAME;
import static io.github.architrace.otlp.AttributeDictionary.DB_SYSTEM;
import static io.github.architrace.otlp.AttributeDictionary.HTTP_HOST;
import static io.github.architrace.otlp.AttributeDictionary.HTTP_METHOD;
import static io.github.architrace.otlp.AttributeDictionary.MSG_DEST;
import static io.github.architrace.otlp.AttributeDictionary.MSG_SYSTEM;

import com.google.protobuf.ByteString;
import io.github.architrace.model.InternalSpan;
import io.github.architrace.model.SpanKind;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpanExtractor {

  public List<InternalSpan> extract(ExportTraceServiceRequest request) {
    return request.getResourceSpansList().stream()
        .flatMap(resourceSpans -> {
          Map<String, String> attrs = toAttributeMap(resourceSpans.getResource());
          return resourceSpans.getScopeSpansList().stream()
              .flatMap(s -> s.getSpansList().stream())
              .map(span -> mapSpan(span, attrs));
        })
        .toList();
  }

  private Map<String, String> toAttributeMap(io.opentelemetry.proto.resource.v1.Resource resource) {
    return resource.getAttributesList().stream()
        .collect(Collectors.toMap(KeyValue::getKey, attr -> attr.getValue().getStringValue(),
            (existing, duplicate) -> existing
        ));
  }

  private Map<String, String> toAttributeMap(List<KeyValue> attributes) {
    return attributes.stream()
        .collect(Collectors.toMap(
            KeyValue::getKey,
            attr -> attr.getValue().getStringValue(),
            (existing, duplicate) -> existing
        ));
  }

  private static String toHex(ByteString bytes) {
    byte[] raw = bytes.toByteArray();
    StringBuilder sb = new StringBuilder(raw.length * 2);
    for (byte b : raw) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private InternalSpan mapSpan(Span span, Map<String, String> resourceAttrs) {
    Map<String, String> spanAttrs = toAttributeMap(span.getAttributesList());

    return new InternalSpan(
        toHex(span.getTraceId()),
        toHex(span.getSpanId()),
        toHex(span.getParentSpanId()),

        resourceAttrs.get("environment"),
        resourceAttrs.get("domainId"),
        resourceAttrs.get("serviceName"),
        resourceAttrs.get("cluster"),
        resourceAttrs.get("namespace"),

        mapKind(span.getKind()),

        spanAttrs.get(HTTP_HOST),
        spanAttrs.get(HTTP_METHOD),

        spanAttrs.get(DB_SYSTEM),
        spanAttrs.get(DB_NAME),

        spanAttrs.get(MSG_SYSTEM),
        spanAttrs.get(MSG_DEST)
    );
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

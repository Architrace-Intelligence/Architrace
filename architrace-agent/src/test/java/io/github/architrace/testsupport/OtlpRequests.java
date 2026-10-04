/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.testsupport;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

public final class OtlpRequests {

    public static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    public static final String CLIENT_SPAN_ID = "b7ad6b7169203331";
    public static final String SERVER_SPAN_ID = "00f067aa0ba902b7";

    private OtlpRequests() {}

    public static ExportTraceServiceRequest request(List<KeyValue> resourceAttributes, Span.Builder... spans) {
        List<Span> built = Arrays.stream(spans).map(Span.Builder::build).toList();
        return ExportTraceServiceRequest.newBuilder()
                .addResourceSpans(ResourceSpans.newBuilder()
                        .setResource(Resource.newBuilder().addAllAttributes(resourceAttributes))
                        .addScopeSpans(ScopeSpans.newBuilder().addAllSpans(built)))
                .build();
    }

    public static Span.Builder span(Span.SpanKind kind, String traceId, String spanId) {
        return Span.newBuilder()
                .setTraceId(bytes(traceId))
                .setSpanId(bytes(spanId))
                .setKind(kind)
                .setName(kind.name().toLowerCase());
    }

    public static Span.Builder childSpan(Span.SpanKind kind, String traceId, String spanId, String parentSpanId) {
        return span(kind, traceId, spanId).setParentSpanId(bytes(parentSpanId));
    }

    public static Span.Builder failed(Span.Builder span) {
        return span.setStatus(Status.newBuilder().setCode(Status.StatusCode.STATUS_CODE_ERROR));
    }

    public static KeyValue text(String key, String value) {
        return KeyValue.newBuilder()
                .setKey(key)
                .setValue(AnyValue.newBuilder().setStringValue(value))
                .build();
    }

    public static KeyValue number(String key, long value) {
        return KeyValue.newBuilder()
                .setKey(key)
                .setValue(AnyValue.newBuilder().setIntValue(value))
                .build();
    }

    public static KeyValue flag(String key, boolean value) {
        return KeyValue.newBuilder()
                .setKey(key)
                .setValue(AnyValue.newBuilder().setBoolValue(value))
                .build();
    }

    public static KeyValue fraction(String key, double value) {
        return KeyValue.newBuilder()
                .setKey(key)
                .setValue(AnyValue.newBuilder().setDoubleValue(value))
                .build();
    }

    public static KeyValue list(String key, String... values) {
        ArrayValue.Builder array = ArrayValue.newBuilder();
        Arrays.stream(values)
                .forEach(value -> array.addValues(AnyValue.newBuilder().setStringValue(value)));
        return KeyValue.newBuilder()
                .setKey(key)
                .setValue(AnyValue.newBuilder().setArrayValue(array))
                .build();
    }

    public static List<KeyValue> standardResource(String serviceName) {
        return List.of(
                text("service.name", serviceName),
                text("service.namespace", "shop"),
                text("service.version", "1.4.2"),
                text("deployment.environment.name", "PROD"),
                text("k8s.cluster.name", "eu-1"),
                text("k8s.namespace.name", "checkout"),
                text("service.instance.id", "checkout-7d9f"));
    }

    private static ByteString bytes(String hex) {
        return ByteString.copyFrom(HexFormat.of().parseHex(hex));
    }
}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.testsupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.span.AttributeMapping;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Map;

public final class TestDataProvider {

    private static final ObjectMapper JSON = new ObjectMapper();

    private TestDataProvider() {}

    public static String readResource(String resourcePath) throws IOException {
        try (var in = resourceStream(resourcePath)) {
            return new String(in.readAllBytes());
        }
    }

    public static Map<String, Object> readJsonObject(String resourcePath) throws IOException {
        return JSON.readValue(readResource(resourcePath), new TypeReference<>() {});
    }

    public static AgentConfig agentConfig() {
        return agentConfig("localhost:9090", AgentConfig.DEFAULT_OTLP_PORT, Duration.ofSeconds(1));
    }

    public static AgentConfig agentConfig(String controlPlaneServer, int otlpPort, Duration retryDelay) {
        return new AgentConfig(
                "demo",
                "DEV",
                "cluster-1",
                "agent-a",
                new AgentConfig.ControlPlaneSettings(controlPlaneServer, retryDelay),
                new AgentConfig.OtlpSettings(otlpPort),
                new AgentConfig.SnapshotSettings(Duration.ofSeconds(60), 64),
                new AgentConfig.BufferSettings(16, Duration.ofSeconds(120)),
                new AgentConfig.MetricsSettings(9464),
                AttributeMapping.defaults());
    }

    public static ExportTraceServiceRequest createSingleSpanRequest(String spanName) {
        Span span = Span.newBuilder()
                .setTraceId(ByteString.copyFrom(new byte[16]))
                .setSpanId(ByteString.copyFrom(new byte[8]))
                .setName(spanName)
                .build();
        ScopeSpans scopeSpans = ScopeSpans.newBuilder().addSpans(span).build();
        ResourceSpans resourceSpans =
                ResourceSpans.newBuilder().addScopeSpans(scopeSpans).build();
        return ExportTraceServiceRequest.newBuilder()
                .addResourceSpans(resourceSpans)
                .build();
    }

    public static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static InputStream resourceStream(String resourcePath) {
        var stream = TestDataProvider.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        return stream;
    }
}

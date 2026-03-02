/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.testsupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.github.architrace.core.config.AgentConfig;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public final class TestDataProvider {

  private static final ObjectMapper JSON = new ObjectMapper();

  private TestDataProvider() {
  }

  public static Path copyResourceToTemp(Path tempDir, String resourcePath, String targetFileName)
      throws IOException {
    Path target = tempDir.resolve(targetFileName);
    try (var in = resourceStream(resourcePath)) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return target;
  }

  public static String readResource(String resourcePath) throws IOException {
    try (var in = resourceStream(resourcePath)) {
      return new String(in.readAllBytes());
    }
  }

  public static Map<String, Object> readJsonObject(String resourcePath) throws IOException {
    return JSON.readValue(readResource(resourcePath), new TypeReference<>() {
    });
  }

  public static AgentConfig createAgentConfig(String endpoint, String agentName) {
    return new AgentConfig(
        "cluster-1",
        new AgentConfig.Agent(agentName),
        new AgentConfig.ControlPlane(new AgentConfig.Bootstrap(endpoint)),
        4319,
        5L);
  }

  public static ExportTraceServiceRequest createSingleSpanRequest(String spanName) {
    Span span =
        Span.newBuilder()
            .setTraceId(ByteString.copyFrom(new byte[16]))
            .setSpanId(ByteString.copyFrom(new byte[8]))
            .setName(spanName)
            .build();

    ScopeSpans scopeSpans = ScopeSpans.newBuilder().addSpans(span).build();
    ResourceSpans resourceSpans = ResourceSpans.newBuilder().addScopeSpans(scopeSpans).build();

    return ExportTraceServiceRequest.newBuilder().addResourceSpans(resourceSpans).build();
  }

  public static int findFreePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private static java.io.InputStream resourceStream(String resourcePath) {
    var stream = TestDataProvider.class.getClassLoader().getResourceAsStream(resourcePath);
    if (stream == null) {
      throw new IllegalArgumentException("Resource not found: " + resourcePath);
    }
    return stream;
  }
}

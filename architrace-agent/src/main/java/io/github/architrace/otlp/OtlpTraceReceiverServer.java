/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.HealthStatusManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OtlpTraceReceiverServer implements AutoCloseable {

  private static final String HEALTH_SERVICE = "otlp-trace-receiver";

  private final Server server;

  public OtlpTraceReceiverServer(int port, OtlpTraceServiceImpl otlpTraceService) {
    var healthStatusManager = new HealthStatusManager();
    healthStatusManager.setStatus(HEALTH_SERVICE, HealthCheckResponse.ServingStatus.SERVING);

    this.server = ServerBuilder.forPort(port)
        .addService(otlpTraceService)
        .addService(healthStatusManager.getHealthService())
        .build();
  }

  public void start() {
    try {
      server.start();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to start OTLP trace receiver.", e);
    }
  }

  /**
   * Blocks until server is terminated.
   * Responds correctly to interruption (used by StructuredTaskScope).
   */
  public void await() throws InterruptedException {
    try {
      server.awaitTermination();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      close();
      throw e;
    }
  }

  @Override
  public void close() {
    server.shutdown();
    try {
      if (!server.awaitTermination(3, TimeUnit.SECONDS)) {
        server.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      server.shutdownNow();
    }
  }
}
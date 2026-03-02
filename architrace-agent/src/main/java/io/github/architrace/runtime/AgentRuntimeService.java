/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.runtime;

import com.google.inject.Inject;
import io.github.architrace.controlplane.ControlPlaneBootstrapService;
import io.github.architrace.controlplane.ControlPlaneLifecycle;
import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.core.config.AgentConfigLoader;
import io.github.architrace.graph.ControlPlanePublisher;
import io.github.architrace.graph.GraphAggregator;
import io.github.architrace.graph.SpanToGraphConverter;
import io.github.architrace.otlp.OtlpTraceReceiverServer;
import io.github.architrace.otlp.OtlpTraceServiceImpl;

import java.nio.file.Path;
import java.util.concurrent.StructuredTaskScope;

public final class AgentRuntimeService {

  private final AgentConfigLoader configLoader;
  private final ControlPlaneBootstrapService bootstrapService;

  private volatile ControlPlaneLifecycle activeLifecycle;

  @Inject
  public AgentRuntimeService(AgentConfigLoader configLoader, ControlPlaneBootstrapService bootstrapService) {
    this.configLoader = configLoader;
    this.bootstrapService = bootstrapService;
  }

  public void run(Path configPath) throws Exception {
    var config = configLoader.load(configPath);
    try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
      scope.fork(() -> runReceiver(config));
      scope.fork(() -> runControlPlaneSupervisor(config));
      scope.join();
    }
  }

  private Void runReceiver(AgentConfig config) throws Exception {
    var service = new OtlpTraceServiceImpl(config.agent().name(), new SpanToGraphConverter(), new GraphAggregator(),
        new ControlPlanePublisher(() -> activeLifecycle));

    var receiver = new OtlpTraceReceiverServer(config.otlpReceiverPort(), service);

    receiver.start();

    try {
      receiver.await();
    } finally {
      receiver.close();
    }

    return null;
  }

  private Void runControlPlaneSupervisor(AgentConfig config) throws Exception {
    while (!Thread.currentThread().isInterrupted()) {
      ControlPlaneLifecycle lifecycle = null;
      try {
        lifecycle = bootstrapService.bootstrap(config);
        activeLifecycle = lifecycle;
        lifecycle.run();
      } catch (Exception ignored) {
        Thread.currentThread().interrupt();
        break;
      } finally {
        activeLifecycle = null;
        if (lifecycle != null) {
          lifecycle.close();
        }
      }
      try {
        Thread.sleep(config.controlPlaneRetrySeconds() * 1000L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    return null;
  }

}

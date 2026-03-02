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

  private ControlPlaneLifecycle activeLifecycle;

  @Inject
  public AgentRuntimeService(AgentConfigLoader configLoader, ControlPlaneBootstrapService bootstrapService) {
    this.configLoader = configLoader;
    this.bootstrapService = bootstrapService;
  }

  public void run(Path configPath) throws InterruptedException {
    var config = configLoader.load(configPath);
    try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
      scope.fork(() -> runReceiver(config));
      scope.fork(() -> runControlPlaneSupervisor(config));
      scope.join();
    }
  }

  private synchronized ControlPlaneLifecycle activeLifecycle() {
    return activeLifecycle;
  }

  private synchronized void setActiveLifecycle(ControlPlaneLifecycle lifecycle) {
    this.activeLifecycle = lifecycle;
  }

  private Void runReceiver(AgentConfig config) throws InterruptedException {
    var service = new OtlpTraceServiceImpl(config.agent().name(), new SpanToGraphConverter(), new GraphAggregator(),
        new ControlPlanePublisher(this::activeLifecycle));

    var receiver = new OtlpTraceReceiverServer(config.otlpReceiverPort(), service);

    receiver.start();

    try {
      receiver.await();
    } finally {
      receiver.close();
    }

    return null;
  }

  private Void runControlPlaneSupervisor(AgentConfig config) {
    while (!Thread.currentThread().isInterrupted()) {
      if (!runControlPlaneSession(config)) {
        return null;
      }
    }

    return null;
  }

  private boolean runControlPlaneSession(AgentConfig config) {
    ControlPlaneLifecycle lifecycle = null;
    try {
      lifecycle = bootstrapService.bootstrap(config);
      setActiveLifecycle(lifecycle);
      lifecycle.run();
    } catch (RuntimeException _) {
      Thread.currentThread().interrupt();
      return false;
    } finally {
      setActiveLifecycle(null);
      if (lifecycle != null) {
        lifecycle.close();
      }
    }

    return sleepBeforeRetry(config.controlPlaneRetrySeconds());
  }

  private boolean sleepBeforeRetry(Long retrySeconds) {
    try {
      Thread.sleep(retrySeconds * 1000L);
      return true;
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}

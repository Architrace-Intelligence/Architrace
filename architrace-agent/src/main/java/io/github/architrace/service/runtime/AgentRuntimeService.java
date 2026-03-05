/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.runtime;

import com.google.inject.Inject;
import io.github.architrace.controlplane.ControlPlaneBootstrapService;
import io.github.architrace.controlplane.ControlPlaneLifecycle;
import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.core.config.AgentConfigLoader;
import io.github.architrace.grpc.TransportClient;
import io.github.architrace.otlp.GraphAggregator;
import io.github.architrace.otlp.GraphSnapshot;
import io.github.architrace.otlp.OtlpTraceReceiverServer;
import io.github.architrace.otlp.OtlpTraceServiceImpl;
import io.github.architrace.otlp.SpanPipeline;
import io.github.architrace.otlp.SpanReceiver;
import io.github.architrace.otlp.SpanRingBuffer;
import io.github.architrace.service.graph.AsyncDependencyResolver;
import io.github.architrace.service.graph.GlobalSpanRegistry;
import io.github.architrace.service.graph.NodeRegistry;
import io.github.architrace.service.graph.SpanExtractor;
import io.github.architrace.service.graph.SyncDependencyResolver;
import io.github.architrace.service.processor.AsyncDependencyProcessor;
import io.github.architrace.service.processor.NodeProcessor;
import io.github.architrace.service.processor.SpanBatchProcessor;
import io.github.architrace.service.processor.SyncDependencyProcessor;
import java.nio.file.Path;
import java.util.List;
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

    var registry = new GlobalSpanRegistry();
    var syncResolver = new SyncDependencyResolver(registry);
    var asyncResolver = new AsyncDependencyResolver();
    NodeRegistry nodeRegistry = new NodeRegistry();
    GraphAggregator aggregator = new GraphAggregator(nodeRegistry, syncResolver );

    SpanRingBuffer ringBuffer = new SpanRingBuffer(1 << 16);
    SpanPipeline pipeline = new SpanPipeline(
        List.of(
            new SyncDependencyProcessor(syncResolver),
            new AsyncDependencyProcessor(asyncResolver),
            new NodeProcessor(nodeRegistry)
        )
    );
    var batchProcessor = new SpanBatchProcessor(ringBuffer, pipeline);
    var receiver = new SpanReceiver(new SpanExtractor(), batchProcessor);

    try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
      scope.fork(() -> runReceiver(config, receiver));
      scope.fork(batchProcessor::run);
      scope.fork(() -> runControlPlaneSupervisor(config));
      scope.fork(() -> runSnapshotLoop(aggregator, activeLifecycle.getTransportClient()));

      scope.join();
    }
  }

  private synchronized ControlPlaneLifecycle activeLifecycle() {
    return activeLifecycle;
  }

  private synchronized void setActiveLifecycle(ControlPlaneLifecycle lifecycle) {
    this.activeLifecycle = lifecycle;
  }

  private Void runReceiver(AgentConfig config, SpanReceiver spanReceiver) throws InterruptedException {
    var service = new OtlpTraceServiceImpl(spanReceiver);
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

  private Void runSnapshotLoop(GraphAggregator aggregator, TransportClient client) throws InterruptedException {
    while (!Thread.currentThread().isInterrupted()) {
      Thread.sleep(60_000);
      GraphSnapshot snapshot = aggregator.snapshotAndReset();
      client.send(snapshot);
    }

    return null;
  }
}

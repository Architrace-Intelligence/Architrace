/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.service.runtime;

import com.google.inject.Inject;
import io.github.architrace.controlplane.ControlPlaneBootstrapService;
import io.github.architrace.controlplane.ControlPlaneLifecycle;
import io.github.architrace.core.config.AgentConfig;
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
import io.github.architrace.service.graph.SyncDependencyResolver;
import io.github.architrace.service.processor.AsyncDependencyProcessor;
import io.github.architrace.service.processor.NodeProcessor;
import io.github.architrace.service.processor.SpanBatchProcessor;
import io.github.architrace.service.processor.SyncDependencyProcessor;
import io.github.architrace.span.SpanNormaliser;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicReference;

public final class AgentRuntimeService {

    private final ControlPlaneBootstrapService bootstrapService;
    private final AtomicReference<ControlPlaneLifecycle> activeLifecycle = new AtomicReference<>();

    @Inject
    public AgentRuntimeService(ControlPlaneBootstrapService bootstrapService) {
        this.bootstrapService = Objects.requireNonNull(bootstrapService, "bootstrapService");
    }

    public void run(AgentConfig config) throws InterruptedException {
        var registry = new GlobalSpanRegistry();
        var syncResolver = new SyncDependencyResolver(registry);
        var asyncResolver = new AsyncDependencyResolver();
        var nodeRegistry = new NodeRegistry();
        var aggregator = new GraphAggregator(nodeRegistry, syncResolver);
        var ringBuffer = new SpanRingBuffer(config.buffers().ringSize());
        var pipeline = new SpanPipeline(List.of(
                new SyncDependencyProcessor(syncResolver),
                new AsyncDependencyProcessor(asyncResolver),
                new NodeProcessor(nodeRegistry)));
        var batchProcessor = new SpanBatchProcessor(ringBuffer, pipeline);
        var normaliser = new SpanNormaliser(config.attributeMapping(), config.environment(), config.cluster());
        var receiver = new SpanReceiver(normaliser, batchProcessor);
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
            scope.fork(() -> runReceiver(config.otlp().port(), receiver));
            scope.fork(batchProcessor::run);
            scope.fork(() -> runControlPlaneSupervisor(config));
            scope.fork(() -> runSnapshotLoop(aggregator, config.snapshot().interval()));
            scope.join();
        }
    }

    private Void runReceiver(int port, SpanReceiver spanReceiver) throws InterruptedException {
        var receiver = new OtlpTraceReceiverServer(port, new OtlpTraceServiceImpl(spanReceiver));
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
            activeLifecycle.set(lifecycle);
            lifecycle.run();
        } catch (RuntimeException _) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            activeLifecycle.set(null);
            if (lifecycle != null) {
                lifecycle.close();
            }
        }
        return sleepBeforeRetry(config.controlPlane().retryDelay());
    }

    private static boolean sleepBeforeRetry(Duration delay) {
        try {
            Thread.sleep(delay);
            return true;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private Void runSnapshotLoop(GraphAggregator aggregator, Duration interval) throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(interval);
            GraphSnapshot snapshot = aggregator.snapshotAndReset();
            ControlPlaneLifecycle lifecycle = activeLifecycle.get();
            if (lifecycle != null) {
                lifecycle.getTransportClient().send(snapshot);
            }
        }
        return null;
    }
}

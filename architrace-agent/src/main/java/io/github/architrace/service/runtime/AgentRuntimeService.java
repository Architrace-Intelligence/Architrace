/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.service.runtime;

import com.google.inject.Inject;
import io.github.architrace.controlplane.ControlPlaneBootstrapService;
import io.github.architrace.controlplane.ControlPlaneLifecycle;
import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.graph.EdgeBuilder;
import io.github.architrace.graph.GraphBuilder;
import io.github.architrace.graph.GraphSnapshot;
import io.github.architrace.graph.PendingSpanIndex;
import io.github.architrace.otlp.OtlpTraceReceiverServer;
import io.github.architrace.otlp.OtlpTraceServiceImpl;
import io.github.architrace.otlp.SpanReceiver;
import io.github.architrace.pipeline.GraphWorker;
import io.github.architrace.pipeline.SpanQueue;
import io.github.architrace.span.SpanNormaliser;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public final class AgentRuntimeService {

    static final Duration SWEEP_INTERVAL = Duration.ofSeconds(10);

    private final ControlPlaneBootstrapService bootstrapService;
    private final AtomicReference<ControlPlaneLifecycle> activeLifecycle = new AtomicReference<>();

    @Inject
    public AgentRuntimeService(ControlPlaneBootstrapService bootstrapService) {
        this.bootstrapService = Objects.requireNonNull(bootstrapService, "bootstrapService");
    }

    public void run(AgentConfig config) throws InterruptedException {
        var clock = InstantSource.system();
        var queue = new SpanQueue(config.buffers().ringSize());
        var edges = new EdgeBuilder(new PendingSpanIndex(config.buffers().pendingTtl()), clock);
        var builder = new GraphBuilder(config.environment(), edges, clock);
        var worker = new GraphWorker(queue, builder, clock, SWEEP_INTERVAL);
        var normaliser = new SpanNormaliser(config.attributeMapping(), config.environment(), config.cluster());
        var receiver = new SpanReceiver(normaliser, queue);
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
            scope.fork(() -> runReceiver(config.otlp().port(), receiver));
            scope.fork(worker::run);
            scope.fork(() -> runControlPlaneSupervisor(config));
            scope.fork(() -> runSnapshotLoop(worker, config.snapshot().interval()));
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

    private Void runSnapshotLoop(GraphWorker worker, Duration interval)
            throws InterruptedException, ExecutionException, TimeoutException {
        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(interval);
            GraphSnapshot snapshot = worker.requestFreeze().get(interval.toMillis(), TimeUnit.MILLISECONDS);
            ControlPlaneLifecycle lifecycle = activeLifecycle.get();
            if (lifecycle != null) {
                lifecycle.getTransportClient().send(snapshot);
            }
        }
        return null;
    }
}

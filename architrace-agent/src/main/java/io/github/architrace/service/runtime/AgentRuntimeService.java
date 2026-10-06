/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.service.runtime;

import com.google.inject.Inject;
import io.github.architrace.controlplane.AgentIdentity;
import io.github.architrace.controlplane.ControlPlaneClientFactory;
import io.github.architrace.controlplane.ControlPlaneSession;
import io.github.architrace.controlplane.ControlPlaneSupervisor;
import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.graph.EdgeBuilder;
import io.github.architrace.graph.GraphBuilder;
import io.github.architrace.graph.PendingSpanIndex;
import io.github.architrace.graph.TopicFilter;
import io.github.architrace.metrics.AgentMetrics;
import io.github.architrace.metrics.DropReporter;
import io.github.architrace.metrics.MetricsServer;
import io.github.architrace.otlp.OtlpTraceReceiverServer;
import io.github.architrace.otlp.OtlpTraceServiceImpl;
import io.github.architrace.otlp.SpanReceiver;
import io.github.architrace.pipeline.GraphWorker;
import io.github.architrace.pipeline.SpanQueue;
import io.github.architrace.publish.PublisherStats;
import io.github.architrace.publish.SnapshotQueue;
import io.github.architrace.span.SpanNormaliser;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class AgentRuntimeService {

    static final Duration SWEEP_INTERVAL = Duration.ofSeconds(10);
    static final Duration REPORT_INTERVAL = Duration.ofSeconds(10);

    private final ControlPlaneClientFactory clientFactory;

    @Inject
    public AgentRuntimeService(ControlPlaneClientFactory clientFactory) {
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory");
    }

    public void run(AgentConfig config) throws InterruptedException {
        var clock = InstantSource.system();
        var spans = new SpanQueue(config.buffers().ringSize());
        var edges = new EdgeBuilder(
                new PendingSpanIndex(config.buffers().pendingTtl()),
                clock,
                new TopicFilter(config.topics().ignore()));
        var builder = new GraphBuilder(config.environment(), edges, clock);
        var worker = new GraphWorker(spans, builder, clock, SWEEP_INTERVAL);
        var normaliser = new SpanNormaliser(config.attributeMapping(), config.environment(), config.cluster());
        var receiver = new SpanReceiver(normaliser, spans);
        var snapshots = new SnapshotQueue(config.snapshot().queueSize());
        var stats = new PublisherStats();
        var identity = AgentIdentity.from(config);
        var supervisor = new ControlPlaneSupervisor(
                () -> new ControlPlaneSession(
                        identity, clientFactory.create(config.controlPlane().server()), snapshots, stats, clock),
                config.controlPlane().retryDelay(),
                stats);
        var metrics = new AgentMetrics(receiver, spans, builder, snapshots, stats);
        var registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        metrics.bindTo(registry);
        var reporter = new DropReporter(metrics);
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
            scope.fork(() -> runReceiver(config.otlp().port(), receiver));
            scope.fork(worker::run);
            scope.fork(supervisor::run);
            scope.fork(
                    () -> runSnapshotLoop(worker, snapshots, config.snapshot().interval()));
            scope.fork(() -> runMetricsServer(config.metrics().port(), registry, stats));
            scope.fork(() -> runReporter(reporter));
            scope.join();
        }
    }

    private static Void runMetricsServer(int port, PrometheusMeterRegistry registry, PublisherStats stats)
            throws InterruptedException {
        try (var server = new MetricsServer(port, registry, stats)) {
            server.start();
            server.await();
        }
        return null;
    }

    private static Void runReporter(DropReporter reporter) throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(REPORT_INTERVAL);
            reporter.report();
        }
        return null;
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

    private Void runSnapshotLoop(GraphWorker worker, SnapshotQueue snapshots, Duration interval)
            throws InterruptedException, ExecutionException, TimeoutException {
        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(interval);
            snapshots.offer(worker.requestFreeze().get(interval.toMillis(), TimeUnit.MILLISECONDS));
        }
        return null;
    }
}
